/*
 * Brigade.scala
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

import collection.immutable.{ListMap, SortedSet}
import concurrent.duration._
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl._

/**
 * An actor that manages the brigade for a channel.
 *
 * @param channel    The channel this brigade is bound to.
 * @param connection The connection to the server.
 * @param storage    The persistent storage engine.
 */
final class Director(channel: Channel, connection: ActorRef[Connection], storage: ActorRef[Storage]) {

  import Brigade.Session

  /** The initial behavior drops all events until initialized then loads the persistent state. */
  val behavior: Behaviors.Immutable[Event] = Behaviors.immutable { (ctx, evt) =>
    evt match {
      case Event.Initialize(initializedAt) =>
        storage ! Storage.LoadState(channel.id, ctx.self ! Event.BrigadeLoaded(initializedAt, _))
        initializing
      case _ =>
        Behaviors.same
    }
  }

  /** The loading behavior drops all events until the persistent state has been loaded then scans the connection. */
  private def initializing: Behaviors.Immutable[Event] = Behaviors.immutable { (ctx, evt) =>
    evt match {
      case Event.BrigadeLoaded(loadedAt, state) =>
        // At maximum scan only the last hour.
        connection ! Connection.ScanMessages(
          channel.id,
          Math.max(state.lastModified, loadedAt - 1.hour.toMillis),
          ctx.self ! Event.MessagesScanned(loadedAt, _, _)
        )
        Buffer().scanning(state)
      case _ =>
        Behaviors.same
    }
  }

  /** The waiting behavior waits for events from the server. */
  private def waiting(state: Any): Behaviors.Immutable[Event] = Behaviors.immutable { (ctx, evt) => ??? /*
    evt match {
      case event@Event.UpdateDirectives(_, _) => drain(Buffer(Some(event)), state, ctx)
      case event@Event.HandleMessage(_, _) => drain(Buffer(messageEvents = SortedSet(event)), state, ctx)
      case _ => Behaviors.same
    }*/
  }

  //@annotation.tailrec
  private def drain(buffered: Buffer, state: Any, context: ActorContext[Event]): Behaviors.Immutable[Event] = ??? /*
    buffered.directiveEvents match {
      //
      // Handle directive updates first.
      //
      case Some(evt) =>
        val owner = channel.guild.owner
        val organizers = evt.messages.filter(_.author == owner).flatMap(_.commands).collect {
          case Command.Brigade(users) => users
        }.flatten.toSet + owner
        val modified =
          if (organizers == state.organizers) state.copy(lastModified = evt.timestamp)
          else state.copy(lastModified = evt.timestamp, organizers = organizers) match {
            case next@Brigade(_, _, Some(session)) if organizers(session.organizer) =>
              refreshTeams(organizers, session)
              next
            case next@Brigade(_, _, Some(session)) =>
              abortTeams(session)
              next.copy(session = None)
            case next =>
              next
          }
        storage ! Storage.SaveState(channel.id, modified)
        drain(buffered.copy(directiveEvents = None), modified, context)
      //
      // Process the next remaining message event.
      //
      case None =>
        if (buffered.messageEvents.isEmpty) waiting(state) else {
          val Event.HandleMessage(timestamp, message) = buffered.messageEvents.head
          // Print the help message if requested.
          if (message.commands.exists {
            case Command.Help => true
            case _ => false
          }) showHelp()
          // Examine the lifecycle commands.
          val lifecycle = message.commands.collect {
            case cmd@Command.Lifecycle() => cmd
          }
          if (lifecycle.isEmpty) {
            drain(buffered.copy(messageEvents = buffered.messageEvents.tail), state, context)
          } else {
            // Attempt to ensure an active session.
            val (commands, active) = state.session filter (_.openMsgId != message.id) map (lifecycle -> _) orElse {
              val opening = lifecycle dropWhile {
                case Command.Open(_, _) => false
                case _ => true
              }
              if (opening.isEmpty) opening -> None else {
                val command = opening.head.asInstanceOf[Command.Open]

                opening.drop(1) -> (??? : Option[Session])
              }

              ??? : Option[Session]
            } map { p =>
              p
              continue(
                state.organizers,
                ???,
                message.id,
                message.author,
                ???
              )
            }
          }
          ???
        }
    }*/

  private def continue(
    organizers: Set[User],
    session: Session,
    msgId: Message.Id,
    author: User,
    commands: Vector[Command.Lifecycle]
  ): Option[Session] = {
    val transactions = commands takeWhile {
      case Command.Terminal() if organizers(author) => false
      case _ => true
    } collect { case cmd@Command.Transaction() => cmd }
    val terminal = commands.dropWhile {
      case Command.Terminal() if organizers(author) => false
      case _ => true
    }.headOption
    val (allMutations, allReplies) = ((Vector[Command.Mutation]() -> Vector[Reply]()) /: transactions) { (p, t) =>
      val (mutations, replies) = p
      t match {
        case cmd@Command.Mutation() =>
          (mutations :+ cmd) -> replies
        case Command.Query(user) =>
          val roster = (session :+ (msgId, author, mutations: _*)).ledger.buildRoster(organizers)
          mutations -> (replies :+ Reply(
            user,
            roster.assignments.filter(_._1 == user).map(_._2).distinct,
            roster.volunteers.filter(_._1 == user).sortBy(_._3).map(_._2).distinct
          ))
      }
    }
    if (allReplies.nonEmpty) connection ! Connection.PostReplies(channel.id, allReplies)
    /*terminal match {
      case Some(Command.Close) =>
        val roster = (session :+ (msgId, author, allMutations: _*)).ledger.buildRoster(organizers)
        val teams = roster.buildTeams(session.slots, session.history) filter
          (_.members.values.flatten.size == session.slots.values.sum)
        refreshTeams(session, teams)
        storage ! Storage.SaveToHistory(channel.id, teams)
        None
      case Some(Command.Abort) =>
        abortTeams(session)
        None
      case None =>
        val next = session :+ (msgId, author, allMutations: _*)
        if (next != session) refreshTeams(organizers, next)
        Some(next)
    }*/
    ???
  }

  private def refreshTeams(organizers: Set[User], session: Session): Unit = {
    refreshTeams(session, session.ledger.buildRoster(organizers).buildTeams(session.slots, session.history))
  }

  private def refreshTeams(session: Session, teams: Vector[Team]): Unit = {
    connection ! Connection.UpdateTeams(channel.id, session.teamsMsgId, teams)
  }

  private def abortTeams(session: Session): Unit = {
    connection ! Connection.UpdateTeams(channel.id, session.teamsMsgId, Vector())
  }

  private def showHelp(): Unit = {
    connection ! Connection.PostHelp(channel.id)
  }

  private case class Buffer(
    directiveEvents: Option[Event.UpdateDirectives] = None,
    messageEvents: SortedSet[Event.HandleMessage] = ??? //SortedSet[Event.HandleMessage]()
  ) {

    def scanning(state: Any): Behaviors.Immutable[Event] = Behaviors.immutable { (ctx, evt) =>
      evt match {
        case event@Event.UpdateDirectives(_, _) =>
          buffer(event).scanning(state)
        case event@Event.HandleMessage(_, _) =>
          buffer(event).scanning(state)
        case Event.MessagesScanned(_, directives, messages) =>
          drain(((directives map buffer getOrElse this) /: messages) (_ buffer _), state, ctx)
        case _ =>
          Behaviors.same
      }
    }

    private def buffer(that: Event.UpdateDirectives): Buffer =
      if (directiveEvents exists (_.timestamp > that.timestamp)) this else copy(directiveEvents = Some(that))

    private def buffer(that: Event.HandleMessage): Buffer =
      if (messageEvents(that)) this else copy(messageEvents = messageEvents + that)

  }

}

/**
 * Definitions of the brigade types.
 */
object Director {

  /*
   * The outcome of applying a message to a brigade.
   *
   * @param brigade The resulting brigade.
   * @param teams   The teams that were built.
   * @param replies The replies that were generated.
   *
  case class OldOutcome(
    brigade: Brigade,
    teams: Vector[Team] = Vector(),
    replies: Vector[Reply] = Vector()
  )

  /**
   * The inactive brigade.
   */
  case object OldInactive extends Brigade {

    /* Always inactive. */
    override def isActive: Boolean = false

    /* Try to become active. */
    override def apply(msgId: Message.Id, commands: Vector[Command.Lifecycle], config: Roster.Config): OldOutcome =
      commands indexWhere {
        case Command.Open(_) => true
        case _ => false
      } match {
        case index if index >= 0 =>
          val slots = commands(index).asInstanceOf[Command.Open].slots
          resume(msgId, slots, msgId, commands drop index + 1, config, Ledger())
        case _ => OldOutcome(OldInactive)
      }

  }

  /**
   * An active brigade.
   *
   * @param openMsgId The ID of the message that opened this brigade.
   * @param slots     The slots specified for this brigade.
   * @param ledger    The ledger that stores the state of this brigade.
   */
  case class OldActive(openMsgId: Message.Id, slots: ListMap[Role, Int], ledger: Ledger) extends Brigade {

    /* Always inactive. */
    override def isActive: Boolean = true

    /* Try to become active. */
    override def apply(msgId: Message.Id, commands: Vector[Command.Lifecycle], config: Roster.Config): OldOutcome =
      if (msgId == openMsgId) {
        commands indexWhere {
          case Command.Open(_) => true
          case _ => false
        } match {
          case index if index >= 0 =>
            val slots = commands(index).asInstanceOf[Command.Open].slots
            resume(openMsgId, slots, msgId, commands drop index + 1, config, ledger)
          case _ => OldOutcome(OldInactive)
        }
      } else {
        resume(openMsgId, slots, msgId, commands, config, ledger)
      }

  }
*/
}
