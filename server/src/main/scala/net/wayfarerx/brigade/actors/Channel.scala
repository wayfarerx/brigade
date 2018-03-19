/*
 * Channel.scala
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
package actors

import akka.actor.typed.scaladsl._

import scala.collection.immutable.ListMap

/**
 * A channel that events are hosted in.
 *
 * @param id The ID of this channel.
 */
final class Channel(val id: Channel.Id) {

  import Channel._

  /**
   * The behavior of this channel.
   *
   * @param state The initial state of the behavior, defaults to `None`.
   * @return The behavior of this channel.
   */
  def behavior(state: Option[State] = None): Behaviors.Immutable[Message] =
    Behaviors.immutable[Message] { (_, message) =>
      val (s, _) = Behavior(message.id)(state, message.commands)
      // TODO save state and teams
      behavior(s)
    }

}

/**
 * Definitions associated with channels.
 */
object Channel {

  /**
   * Definition of a channel ID.
   *
   * @param value The underlying ID value.
   */
  final class Id(val value: Long) extends AnyVal

  /**
   * The active channel state.
   *
   * @param slots The slots to be filled.
   * @param ledger The ledger of mutation commands.
   */
  case class State(slots: ListMap[Role, Int], ledger: Ledger)

  /**
   * Implementation of a channel's behavior.
   *
   * @param msgId The ID of the message that triggered this behavior.
   */
  case class Behavior(msgId: Message.Id) {

    def apply(state: Option[State], commands: Vector[Command]): (Option[State], Vector[Set[Team]]) =
      behave(Vector(), state, commands)

    private def behave(
      teams: Vector[Set[Team]],
      state: Option[State],
      commands: Vector[Command]
    ): (Option[State], Vector[Set[Team]]) = ??? /*
      if (commands.isEmpty) state -> teams else {

        /* Handle an active state. */
        def onActive(state: State): (Vector[Set[Team]], Option[State], Vector[Command]) = {
          val mutations = commands takeWhile {
            case Command.Abort | Command.Close => false
            case _ => true
          } collect {
            case cmd: Command.Mutation => cmd
          }
          val ledger = if (mutations.isEmpty) state.ledger else
            state.ledger.copy(entries = state.ledger.entries :+ Ledger.Entry(msgId, mutations))
          val next = commands.dropWhile {
            case Command.Abort | Command.Close => false
            case _ => true
          }
          next.headOption match {
            case Some(Command.Close) =>

              ???
            case Some(Command.Abort) =>
              (Vector(), None, next.drop(1))
            case None =>
              (Vector(), Some(State(state.slots, ledger)), Vector())
          }
        }

        /* Handle an inactive state. */
        def onInactive: (Vector[Set[Team]], Option[State], Vector[Command]) =
          commands.dropWhile {
            case Command.Open(_) => false
            case _ => true
          }.headOption.collect {
            case Command.Open(slots) =>
              (Vector(), Some(State(slots, Ledger())), ???)
          } getOrElse(Vector(), None, Vector())

        val (_teams, _state, _commands) = state match {
          case Some(s) => onActive(s)
          case None => onInactive
        }
        behave(_teams ++ teams, _state, _commands)
      }
*/
  }

}
