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

/**
 * A channel that events are hosted in.
 *
 * @param id The ID of this channel.
 */
final class Manager(val id: Manager.Id, initialState: Manager.State = Manager.Inactive(0)) {

  import Manager._

  /** The behavior of this channel. */
  val behavior: Behaviors.Immutable[Message] = behave(initialState)

  /**
   * The behavior of a channel.
   *
   * @param state The state of the behavior.
   * @return The behavior of a channel.
   */
  private def behave(state: State): Behaviors.Immutable[Message] = state match {
    case i@Inactive(_) => inactive(i)
    case a@Active(_, _) => active(a)
  }

  /**
   * The inactive behavior of a channel.
   *
   * @param state The inactive state of the behavior.
   * @return The behavior of a channel.
   */
  private def inactive(state: Inactive): Behaviors.Immutable[Message] = Behaviors.immutable[Message] { (_, message) =>
    /*val Director.OldOutcome(b, t, r) = state.brigade(message)
    b match {
      case Director.OldInactive => ???
      case Director.OldActive(_, _, _) => ???
    }

    b
    t
    r*/
    ???
  }

  /**
   * The active behavior of a channel.
   *
   * @param state The active state of the behavior.
   * @return The behavior of a channel.
   */
  private def active(state: Active): Behaviors.Immutable[Message] = Behaviors.immutable[Message] { (_, message) =>

    ???
  }

}

/**
 * Definitions associated with channels.
 */
object Manager {

  /**
   * Definition of a channel ID.
   *
   * @param value The underlying ID value.
   */
  final class Id(val value: Long) extends AnyVal

  /**
   * The base type of channel states.
   */
  sealed trait State {

    /** The instant the channel was last modified. */
    def lastModifiedMs: Long

    /** The state of the channel's brigade. */
    def brigade: Channel

  }

  /**
   * The state of an inactive channel.
   *
   * @param lastModifiedMs The last time the channel was modified.
   */
  case class Inactive(lastModifiedMs: Long) extends State {

    /* Always the inactive brigade. */
    override def brigade = ??? // Director.OldInactive.type = Director.OldInactive

  }


  /**
   * The state of an active channel.
   *
   * @param lastModifiedMs The last time the channel was modified.
   * @param brigade        The state of the channel's brigade.
   */
  case class Active(lastModifiedMs: Long, brigade: Channel) extends State {

  }

  sealed trait Interaction

  case object ShowHelp extends Interaction

  case class ReplyToQueries(replies: Vector[Reply]) extends Interaction

}
