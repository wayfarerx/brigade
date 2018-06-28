/*
 * Discord.scala
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

import java.time.Instant
import java.util.concurrent.{Executors, TimeUnit}
import java.util.concurrent.atomic.AtomicBoolean

import collection.JavaConverters._
import concurrent.{ExecutionContext, Future}
import concurrent.duration._
import language.implicitConversions
import util.{Failure, Success}
import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.actor.typed.scaladsl._
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.util.{EmbedBuilder, RequestBuffer}

import scala.collection.immutable.ListMap


/**
 * An actor that handles sending events to Discord.
 *
 * @param client    The client used to interact with the Discord server.
 * @param reporting The actor to report outcomes to.
 */
final class Discord private(client: IDiscordClient, reporting: ActorRef[Reporting.Action]) {

  import Discord._

  /** The thread pool for sending events to Discord. */
  private val pool = Executors.newCachedThreadPool()

  /** The execution context that uses the thread pool. */
  private implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(pool)

  /** The behavior that handles Discord actions. */
  private def behavior: Behavior[Action] = Behaviors.receive[Action] { (ctx, act) =>
    act match {

      case PostReplies(Event.PostReplies(channelId, replies, timestamp)) =>
        Future {
          replies foreach {
            case Reply.Usage =>
              send(channelId, _.sendMessage(usageMsg))
            case Reply.Status(user, assigned, volunteered) =>
              send(channelId, _.sendMessage(statusMsg(user, assigned, volunteered)))
            case Reply.UpdateTeams(slots, teamsMsgId, teams) =>
              send(channelId, _.getMessageByID(teamsMsgId.value).edit(teamsChangedMsg(slots, teams, finalized = false)))
            case Reply.FinalizeTeams(slots, teamsMsgId, teams) =>
              send(channelId, _.getMessageByID(teamsMsgId.value).edit(teamsChangedMsg(slots, teams, finalized = true)))
            case Reply.AbandonTeams(teamsMsgId) =>
              send(channelId, _.getMessageByID(teamsMsgId.value).edit(abandonTeamsMsg))
          }
        } onComplete {
          case Success(_) =>
            reporting ! Reporting.PostedReplies(channelId, timestamp)
          case Failure(thrown) =>
            reporting ! Reporting.FailedToPostReplies(thrown.getMessage, channelId, timestamp)
        }

      case LoadMessages(Event.LoadMessages(channelId, lastModified, respondTo, timestamp)) =>
        val latch = new AtomicBoolean
        ctx.system.scheduler.scheduleOnce(1.minute) {
          if (latch.compareAndSet(false, true)) executionContext.execute(() => {
            reporting ! Reporting.FailedToLoadMessages("Timed out.", channelId, timestamp)
            respondTo ! Event.MessagesLoaded(Event.Configure(Vector(), timestamp), Vector(), timestamp)
          })
        }
        Future {
          val pinned = send(channelId, _.getPinnedMessages).asScala.toVector
          val history = send(channelId,
            _.getMessageHistoryFrom(Instant.ofEpochMilli(math.max(lastModified, timestamp - 6.hours.toMillis))))
          Event.Configure(
            pinned map Utils.transformMessage,
            ((None: Option[Long]) /: pinned.map(_.getTimestamp.toEpochMilli)) { (p, n) =>
              p map (Math.max(_, n))
            } getOrElse timestamp) ->
            history.asArray.toVector.map(m => Event.Submit(Utils.transformMessage(m), m.getTimestamp.toEpochMilli))
        } onComplete {
          case Success((configure, submissions)) =>
            if (latch.compareAndSet(false, true)) {
              reporting ! Reporting.LoadedMessages(channelId, timestamp)
              respondTo ! Event.MessagesLoaded(configure, submissions, timestamp)
            }
          case Failure(thrown) =>
            if (latch.compareAndSet(false, true)) {
              reporting ! Reporting.FailedToLoadMessages(thrown.getMessage, channelId, timestamp)
              respondTo ! Event.MessagesLoaded(Event.Configure(Vector(), timestamp), Vector(), timestamp)
            }
        }

      case PrepareTeams(Event.PrepareTeams(channelId, respondTo, timestamp)) =>
        val latch = new AtomicBoolean
        ctx.system.scheduler.scheduleOnce(1.minute) {
          if (latch.compareAndSet(false, true)) executionContext.execute(() => {
            reporting ! Reporting.FailedToPrepareTeams("Timed out.", channelId, timestamp)
            respondTo ! Event.TeamsPrepared(None, timestamp)
          })
        }
        Future {
          Message.Id(send(channelId, _.sendMessage(prepareTeamsMsg)).getLongID)
        } onComplete {
          case Success(teamsMsgId) =>
            if (latch.compareAndSet(false, true)) {
              reporting ! Reporting.PreparedTeams(channelId, timestamp)
              respondTo ! Event.TeamsPrepared(Some(teamsMsgId), timestamp)
            }
          case Failure(thrown) =>
            if (latch.compareAndSet(false, true)) {
              reporting ! Reporting.FailedToPrepareTeams(thrown.getMessage, channelId, timestamp)
              respondTo ! Event.TeamsPrepared(None, timestamp)
            }
        }

    }
    Behaviors.same
  } receiveSignal {
    case (_, PostStop) ⇒
      pool.shutdown()
      pool.awaitTermination(1, TimeUnit.MINUTES)
      Behaviors.same
  }

  /**
   * Sends a message to a channel while honoring rate limits.
   *
   * @tparam T The type of result from sending a message.
   * @param channelId The ID of the channel to send to.
   * @param f         The function that sends a message to a channel.
   * @return The result of sending a message.
   */
  private def send[T](channelId: Channel.Id, f: IChannel => T): T =
    RequestBuffer.request { () => f(client.getChannelByID(channelId.value)) }.get()

}

/**
 * Factory for Discord actors.
 */
object Discord {

  /**
   * Creates a Discord behavior using a connection to the server.
   *
   * @param client    The client to communicate with.
   * @param reporting The actor to report outcomes to.
   * @return A Discord behavior using a connection to the server.
   */
  def apply(
    client: IDiscordClient,
    reporting: ActorRef[Reporting.Action]
  ): Behavior[Discord.Action] =
    new Discord(client, reporting).behavior

  /** The message that describes how to use brigade. */
  private def usageMsg =
    """Hello, I'm the Brigade team builder, progeny of the inimitable wayfarerx.
      |I see this channel has been set up to build teams, excellent! Here's the commands you can use:
      |```
      | !open (!ROLE COUNT)+     -- Sets up the team roster.
      | !abort                   -- Abandons the roster and does not build a team.
      | !close                   -- Closes the roster and finalizes a team.
      | !assign (@USER !ROLE)+   -- Assigns members to roles in the team.
      | !release @USER+          -- Releases previously assigned members.
      | !offer @USER !ROLE+      -- Volunteers a member for one or more roles.
      | !kick @USER !ROLE*       -- Kicks a member from one or more roles (or all roles if you don't list any).```
      |Note that you have to be an event administrator to use the above commands. Here's the commands for everyone:
      |```
      | !ROLE+                   -- Volunteers YOU for one or more roles.
      | !drop !ROLE*             -- Drops YOU from one or more roles (or all roles if you don't list any).
      | !? @USER?                -- Lists the roles that @USER has volunteered for or YOUR roles if @USER is omitted.
      | !help                    -- Repeats this message for whenever you feel like you miss me.```
      |For a more detailed tutorial about how this bot works see https://github.com/wayfarerx/circumvolve.
    """.stripMargin

  /**
   * Generates a message that reports the status of a user in the brigade.
   *
   * @param user        The user to generate the status for.
   * @param assigned    The roles the user is assigned to.
   * @param volunteered The roles the user has volunteered for.
   * @return The message that reports the status of a user in the brigade.
   */
  private def statusMsg(user: User, assigned: Vector[Role], volunteered: Vector[Role]) = {

    def render(prefix: String, suffix: Vector[Role]): String = prefix + (suffix match {
      case Vector(v) => v.id
      case v => s"${v.init map (_.id) mkString ", "} & ${v.last.id}"
    })

    def isAssignedTo: String = render("is assigned to ", assigned)

    def isVolunteeredFor: String = render("has volunteered for ", volunteered)

    (assigned, volunteered) match {
      case (Vector(), Vector()) => s"<@!${user.id}> is not a member of this brigade."
      case (_, Vector()) => s"<@!${user.id}> $isAssignedTo."
      case (Vector(), _) => s"<@!${user.id}> $isVolunteeredFor."
      case (_, _) => s"<@!${user.id}> $isAssignedTo and $isVolunteeredFor."
    }
  }

  /** The message used when updating teams. */
  private def teamsChangedMsg(slots: ListMap[Role, Int], teams: Vector[Team], finalized: Boolean) = {
    val builder = new EmbedBuilder
    builder.withTitle(if (finalized) "Final Teams" else "Current Teams")
    builder.withColor(114, 137, 218)
    teams.zipWithIndex foreach { case (team, teamIndex) =>
      val content = slots.map { case (role, count) =>
        val members = team.members.getOrElse(role, Vector()) map (Some(_))
        (role, members ++ Vector.fill(count - members.size)(None))
      } flatMap { case (role, users) =>
        users.zipWithIndex map { case (u, i) =>
          s"${role.id} ${i + 1}: ${u map (user => s"<@!${user.id}>") getOrElse "?"}"
        }
      } mkString "\r\n"
      builder.appendField(s"Team ${teamIndex + 1}", content, true)
    }
    builder.build()
  }

  /** The message used when abandoning teams. */
  private def abandonTeamsMsg = "This brigade was abandoned."

  /** The message used when preparing teams. */
  private def prepareTeamsMsg = "Preparing brigade..."

  /**
   * Base class for Discord messages.
   */
  sealed trait Action

  /**
   * Posts replies to Discord.
   *
   * @param event The event that describes this action.
   */
  case class PostReplies(event: Event.PostReplies) extends Action


  /**
   * Loads messages from Discord.
   *
   * @param event The event that describes this action.
   */
  case class LoadMessages(event: Event.LoadMessages) extends Action

  /**
   * Prepares a teams message in Discord.
   *
   * @param event The event that describes this action.
   */
  case class PrepareTeams(event: Event.PrepareTeams) extends Action

}
