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

/**
 * Definition of a channel.
 *
 * @param id             The ID of this channel.
 * @param organizers The administrators of this channel.
 * @param guild          The guild this channel belongs to.
 */
case class Channel(id: Channel.Id, organizers: Set[User], guild: Guild)

/**
 * Defines the channel ID.
 */
object Channel {

  /**
   * The ID of a channel.
   *
   * @param value The underlying channel ID value.
   */
  final class Id private(val value: Long) extends AnyVal

  /**
   * Factory for channel IDs.
   */
  object Id {

    /**
     * Creates a new channel ID.
     *
     * @param value The underlying channel ID value.
     * @return A new channel ID.
     */
    def apply(value: Long): Id = new Id(value)

  }

}
