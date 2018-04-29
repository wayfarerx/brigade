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

import collection.immutable.{ListMap, SortedSet}
import concurrent.duration._
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl._

/**
 * An actor that manages the brigade for a channel.
 *
 * @param id         The ID of this channel.
 * @param owner      The owner of this channel.
 * @param outgoing   The connection to the outside world.
 */
final class Channel(id: Channel.Id, owner: User, outgoing: ActorRef[Event.Outgoing]) {


}

/**
 * Definitions associated with channels.
 */
object Channel {

  /**
   * The ID of a channel.
   *
   * @param value The underlying channel ID value.
   */
  final class Id private(val value: Long) extends AnyVal {

    /* Convert to a string. */
    override def toString: String = s"Channel.Id($value)"

  }

  /**
   * Factory for channel IDs.
   */
  object Id {

    /**
     * Creates a new channel ID.
     *
     * @param value The underlying channel ID value.
     * @return a new channel ID.
     */
    def apply(value: Long): Id = new Id(value)

  }

}
