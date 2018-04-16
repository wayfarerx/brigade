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

/**
 * Base type of storage events.
 */
sealed trait Storage

/**
 * Definitions of the storage events.
 */
object Storage {

  /**
   * Loads the persistent brigade information.
   *
   * @param id The ID of the channel to load the brigade information for.
   * @param callback The callback to invoke with the brigade information.
   */
  case class LoadState(
    id: Channel.Id,
    callback: Brigade => Unit
  ) extends Storage

  /**
   * Saves the persistent brigade information.
   *
   * @param id The ID of the channel to save the brigade information for.
   * @param state The state of the brigade to save.
   */
  case class SaveState(
    id: Channel.Id,
    state: Brigade
  ) extends Storage

  /**
   * Loads the persistent history of the specified channel.
   *
   * @param id The ID of the channel to load the history of.
   * @param maxEntries The maximum number of team sets to load.
   * @param callback The callback to invoke with the requested history.
   */
  case class LoadHistory(
    id: Channel.Id,
    maxEntries: Int,
    callback: History => Unit
  ) extends Storage

  /**
   * Prepends to the persistent history of the specified channel.
   *
   * @param id The ID of the channel to prepend to the history of.
   * @param teams The team set to prepend to the channel's history.
   */
  case class SaveToHistory(
    id: Channel.Id,
    teams: Vector[Team]
  ) extends Storage

}
