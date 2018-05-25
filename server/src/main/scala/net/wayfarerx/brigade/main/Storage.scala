/*
 * Storage.scala
 *
 * Copyright 2018 wayfarerx <x@wayfarerx.net> (@thewayfarerx)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wayfarerx.brigade
package main

import java.nio.charset.Charset
import java.nio.file.{Files, Path, Paths}

import collection.JavaConverters._
import collection.immutable.ListMap
import util.{Failure, Success, Try}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl._

import io.circe._
import io.circe.generic.auto._
import io.circe.generic.extras.{Configuration => Config}
import io.circe.syntax._

import software.amazon.awssdk.services.s3.model.{
  GetObjectRequest,
  ListObjectsRequest,
  NoSuchKeyException,
  PutObjectRequest
}

import net.wayfarerx.aws.S3

/**
 * An actor that handles interfacing with the persistent storage layer.
 *
 * @param driver    The driver used to interact with the persistence layer.
 * @param reporting The actor to report outcomes to.
 */
final class Storage private(driver: Storage.Driver, reporting: ActorRef[Reporting.Action]) {

  import Storage._

  /** The behavior that handles storage actions. */
  private def behavior: Behavior[Action] = Behaviors.receive[Action] { (ctx, act) =>
    val self = ctx.self
    act match {

      case LoadSession(channelId, respondTo, timestamp) =>
        driver.loadSession(channelId, {
          case Success(Some(bytes)) =>
            Try(new String(bytes, UTF8)) match {
              case Success(string) => parser.parse(string).flatMap(_.as[Brigade.Session]) match {
                case Right(result) =>
                  reporting ! Reporting.SessionLoaded(channelId, timestamp)
                  respondTo ! Event.Initialize(Some(result), timestamp)
                case Left(failed) =>
                  reporting ! Reporting.FailedToParseSession(failed.getMessage, channelId, timestamp)
                  respondTo ! Event.Initialize(None, timestamp)
              }
              case Failure(thrown) =>
                reporting ! Reporting.FailedToDecodeSession(thrown.getMessage, channelId, timestamp)
                respondTo ! Event.Initialize(None, timestamp)
            }
          case Success(None) =>
            reporting ! Reporting.SessionSkipped(channelId, timestamp)
            respondTo ! Event.Initialize(None, timestamp)
          case Failure(thrown) =>
            reporting ! Reporting.FailedToLoadSession(thrown.getMessage, channelId, timestamp)
            self ! LoadSession(channelId, respondTo, timestamp)
        })

      case SaveSession(Event.SaveSession(channelId, session, timestamp)) =>
        driver.saveSession(channelId, session.asJson.noSpaces.getBytes(UTF8), {
          case Success(()) => reporting ! Reporting.SessionSaved(channelId, timestamp)
          case Failure(thrown) => reporting ! Reporting.FailedToSaveSession(thrown.getMessage, channelId, timestamp)
        })

      case LoadHistory(Event.LoadHistory(channelId, depth, respondTo, timestamp)) =>
        driver.loadHistory(channelId, depth, {
          case Success(allBytes) =>
            val result = allBytes flatMap { bytes =>
              Try(new String(bytes, UTF8)) match {
                case Success(string) =>
                  Some(string)
                case Failure(thrown) =>
                  reporting ! Reporting.FailedToDecodeTeams(thrown.getMessage, channelId, timestamp)
                  None
              }
            } flatMap { string =>
              parser.parse(string).flatMap(_.as[Vector[Team]]) match {
                case Right(teams) =>
                  Some(teams)
                case Left(failed) =>
                  reporting ! Reporting.FailedToParseTeams(failed.getMessage, channelId, timestamp)
                  None
              }
            }
            if (result.nonEmpty) reporting ! Reporting.HistoryLoaded(channelId, timestamp)
            respondTo ! Event.HistoryLoaded(History(result), timestamp)
          case Failure(thrown) =>
            reporting ! Reporting.FailedToLoadHistory(thrown.getMessage, channelId, timestamp)
            respondTo ! Event.HistoryLoaded(History(), timestamp)
        })

      case PrependToHistory(Event.PrependToHistory(channelId, teams, timestamp)) =>
        driver.saveToHistory(channelId, teams.asJson.noSpaces.getBytes(UTF8), timestamp, {
          case Success(()) => reporting ! Reporting.HistoryPrepended(channelId, timestamp)
          case Failure(thrown) => reporting ! Reporting.FailedToPrependHistory(thrown.getMessage, channelId, timestamp)
        })

    }
    Behaviors.same
  }

}

/**
 * Factory for storage actors.
 */
object Storage {

  /** The character set we use to encode JSON files. */
  private val UTF8 = Charset.forName("UTF-8")

  /** Configure for snake case names. */
  private implicit val Configuration: Config = Config.default.withSnakeCaseMemberNames

  /** The user JSON encoder. */
  private implicit val UserEncoder: Encoder[User] = u => Json.fromLong(u.id)

  /** The user JSON decoder. */
  private implicit val UserDecoder: Decoder[User] = _.as[Long].map(User(_))

  /** The role JSON encoder. */
  private implicit val RoleEncoder: Encoder[Role] = r => Json.fromString(r.id)

  /** The role JSON decoder. */
  private implicit val RoleDecoder: Decoder[Role] = _.as[String].map(Role(_))

  /** The message ID JSON encoder. */
  private implicit val MessageIdEncoder: Encoder[Message.Id] = i => Json.fromLong(i.value)

  /** The message ID JSON decoder. */
  private implicit val MessageIdDecoder: Decoder[Message.Id] = _.as[Long].map(Message.Id(_))

  /** The team JSON encoder. */
  private implicit val TeamEncoder: Encoder[Team] = _.members.toVector.asJson

  /** The team JSON decoder. */
  private implicit val TeamDecoder: Decoder[Team] = _.as[Vector[(Role, Vector[User])]].map(t => Team(ListMap(t: _*)))

  /** The slots JSON encoder. */
  private implicit val SlotsEncoder: Encoder[ListMap[Role, Int]] = _.toVector.asJson

  /** The slots JSON decoder. */
  private implicit val SlotsDecoder: Decoder[ListMap[Role, Int]] = _.as[Vector[(Role, Int)]].map(v => ListMap(v: _*))

  /** The ledger JSON encoder. */
  private implicit val LedgerEncoder: Encoder[Ledger] = _.entries.map(e => (e.msgId, e.author, e.mutations)).asJson

  /** The ledger JSON decoder. */
  private implicit val LedgerDecoder: Decoder[Ledger] =
    _.as[Vector[(Message.Id, User, Vector[(Command.Mutation, Int)])]]
      .map(v => Ledger(v.map(e => Ledger.Entry(e._1, e._2, e._3))))

  /**
   * Creates a storage behavior using the local file system.
   *
   * @param path      The path to store data at.
   * @param reporting The actor to report outcomes to.
   * @return A storage behavior using the local file system.
   */
  def apply(
    path: Path,
    reporting: ActorRef[Reporting.Action]
  ): Behavior[Storage.Action] =
    new Storage(Driver.Default(path), reporting).behavior

  /**
   * Creates a storage behavior using the cloud.
   *
   * @param s3        The S3 actor to use.
   * @param bucket    The S3 bucket to use.
   * @param prefix    The S3 path prefix to use.
   * @param reporting The actor to report outcomes to.
   * @return A storage behavior using the cloud.
   */
  def apply(
    s3: ActorRef[S3.Event],
    bucket: String,
    prefix: String,
    reporting: ActorRef[Reporting.Action]
  ): Behavior[Storage.Action] =
    new Storage(Driver.Cloud(s3, bucket, prefix), reporting).behavior

  /**
   * Base class for storage messages.
   */
  sealed trait Action

  /**
   * The message that instructs the storage layer to load a session.
   *
   * @param channelId The ID of the channel this event pertains to.
   * @param respondTo The actor to respond to.
   * @param timestamp The instant this action occurred.
   */
  case class LoadSession(channelId: Channel.Id, respondTo: ActorRef[Event.Initialize], timestamp: Long) extends Action

  /**
   * The message that instructs the storage layer to save a session.
   *
   * @param event The event that describes this action.
   */
  case class SaveSession(event: Event.SaveSession) extends Action

  /**
   * The message that instructs the storage layer to load the history of a channel.
   *
   * @param event The event that describes this action.
   */
  case class LoadHistory(event: Event.LoadHistory) extends Action

  /**
   * The message that instructs the storage layer to prepend a set of teams to the history of this channel.
   *
   * @param event The event that describes this action.
   */
  case class PrependToHistory(event: Event.PrependToHistory) extends Action

  /**
   * Base class for storage drivers.
   */
  sealed trait Driver {

    /**
     * Loads a session from the storage medium.
     *
     * @param channelId The ID of the channel to load a session for.
     * @param cb        The call-back to invoke with the result of the operation.
     * @return The session that was loaded if one is available.
     */
    def loadSession(channelId: Channel.Id, cb: Try[Option[Array[Byte]]] => Unit): Unit

    /**
     * Saves a session to the storage medium.
     *
     * @param channelId The ID of the channel to save the session for.
     * @param session   The session to save.
     * @param cb        The call-back to invoke with the result of the operation.
     */
    def saveSession(channelId: Channel.Id, session: Array[Byte], cb: Try[Unit] => Unit): Unit

    /**
     * Loads history from the storage medium.
     *
     * @param channelId The ID of the channel to load history for.
     * @param depth     The maximum number of team sets to load.
     * @param cb        The call-back to invoke with the result of the operation.
     * @return The team sets that were loaded.
     */
    def loadHistory(channelId: Channel.Id, depth: Int, cb: Try[Vector[Array[Byte]]] => Unit): Unit

    /**
     * Saves a team set to a channel's history in the storage medium.
     *
     * @param channelId The ID of the channel to prepend to the history of.
     * @param teams     The team set to prepend to the history.
     * @param timestamp The instant that the team set was to be prepended.
     * @param cb        The call-back to invoke with the result of the operation.
     */
    def saveToHistory(channelId: Channel.Id, teams: Array[Byte], timestamp: Long, cb: Try[Unit] => Unit): Unit

  }

  /**
   * Factory for storage drivers.
   */
  object Driver {

    /** The name of the session file. */
    private def sessionFileName =
      "current.json"

    /** The name of a new history file. */
    private def historyFileName(timestamp: Long) =
      f"history_${Long.MaxValue - timestamp}%019d.json"

    /**
     * Storage driver that uses the local filesystem,
     *
     * @param root The root directory to store files in.
     */
    case class Default(root: Path) extends Driver {

      /* Read a session from disk. */
      override def loadSession(channelId: Channel.Id, cb: Try[Option[Array[Byte]]] => Unit): Unit =
        cb(Try {
          val path = root.resolve(Paths.get(channelId.toString, sessionFileName))
          if (Files.isRegularFile(path)) Some(Files.readAllBytes(path)) else None
        })

      /* Write a session to disk. */
      override def saveSession(channelId: Channel.Id, session: Array[Byte], cb: Try[Unit] => Unit): Unit =
        cb(Try {
          val path = root.resolve(Paths.get(channelId.toString, sessionFileName))
          if (!Files.isDirectory(path)) Files.write(path, session)
        })

      /* Read a number of team sets from disk. */
      override def loadHistory(channelId: Channel.Id, depth: Int, cb: Try[Vector[Array[Byte]]] => Unit): Unit =
        cb(Try {
          val path = root.resolve(Paths.get(channelId.toString))
          if (!Files.isDirectory(path)) Vector() else Files.list(path).iterator().asScala.filter { child =>
            Files.isRegularFile(child) && child.getName(child.getNameCount - 1).startsWith("history_")
          }.take(depth).map(Files.readAllBytes).toVector
        })

      /* Write a team set to disk. */
      override def saveToHistory(channelId: Channel.Id, teams: Array[Byte], ts: Long, cb: Try[Unit] => Unit): Unit =
        cb(Try {
          val path = root.resolve(Paths.get(channelId.toString, historyFileName(ts)))
          if (!Files.isDirectory(path)) Files.write(path, teams)
        })

    }

    /**
     * Storage driver that uses Amazon S3,
     *
     * @param s3     The S3 actor to use.
     * @param bucket The S3 bucket to use.
     * @param prefix The S3 path prefix to use.
     */
    case class Cloud(s3: ActorRef[S3.Event], bucket: String, prefix: String) extends Driver {

      /* Read a session from S3. */
      override def loadSession(channelId: Channel.Id, cb: Try[Option[Array[Byte]]] => Unit): Unit =
        s3 ! S3.GetObject(
          GetObjectRequest.builder()
            .bucket(bucket)
            .key(s"$prefix/${channelId.value}/$sessionFileName")
            .build(),
          result => cb(result map {
            case (_, bytes) => Some(bytes)
          } recover {
            case _: NoSuchKeyException => None
          })
        )

      /* Write a session to S3. */
      override def saveSession(channelId: Channel.Id, session: Array[Byte], cb: Try[Unit] => Unit): Unit =
        s3 ! S3.PutObject(
          PutObjectRequest.builder()
            .bucket(bucket)
            .key(s"$prefix/${channelId.value}/$sessionFileName")
            .build() -> session,
          result => cb(result map (_ => ()))
        )

      /* Read a number of team sets from S3. */
      override def loadHistory(channelId: Channel.Id, depth: Int, cb: Try[Vector[Array[Byte]]] => Unit): Unit = {

        def loadTeams(
          remaining: Vector[String],
          results: Vector[Array[Byte]]
        ): Unit =
          if (remaining.isEmpty) cb(Success(results)) else
            s3 ! S3.GetObject(
              GetObjectRequest.builder()
                .bucket(bucket)
                .key(s"$prefix/${channelId.value}/${remaining.head}")
                .build(),
              _ map { case (_, result) => loadTeams(remaining.tail, results :+ result) }
            )

        s3 ! S3.ListObjects(
          ListObjectsRequest.builder()
            .bucket(bucket)
            .prefix(s"$prefix/${channelId.value}/history_")
            .maxKeys(depth)
            .build(),
          _ map (response => loadTeams(response.contents.asScala.toVector.map(_.key), Vector()))
        )
      }

      /* Write a team set to S3. */
      override def saveToHistory(channelId: Channel.Id, teams: Array[Byte], ts: Long, cb: Try[Unit] => Unit): Unit =
        s3 ! S3.PutObject(
          PutObjectRequest.builder()
            .bucket(bucket)
            .key(s"$prefix/${channelId.value}/${historyFileName(ts)}")
            .build() -> teams,
          result => cb(result map (_ => ()))
        )

    }

  }

}
