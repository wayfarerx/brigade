/*
 * Status.scala
 *
 * Copyright 2017 wayfarerx <x@wayfarerx.net> (@thewayfarerx)
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

package net.wayfarerx.circumvolve.service

import net.wayfarerx.circumvolve.model.{Roster, Team}

/**
 * Base class for messages sent to the UI that describe the currently available team.
 */
sealed trait Status {

  /** The ID of the guild this status pertains to. */
  def guildId: String

  /** The ID of the channel this status pertains to. */
  def channelId: String

  /** The ID of the event this status pertains to. */
  def eventId: String

}

/**
 * Implementation of the various UI messages.
 */
object Status {

  /**
   * A message sent to the UI after an event opens.
   *
   * @param guildId The ID of the guild this status pertains to.
   * @param channelId The ID of the channel this status pertains to.
   * @param eventId The ID of the event this status pertains to.
   * @param roster The roster for the event.
   * @param team The team assembled for the event.
   */
  case class Opened(guildId: String, channelId: String, eventId: String, roster: Roster, team: Team) extends Status

  /**
   * A message sent to the UI after an event is updated.
   *
   * @param guildId The ID of the guild this status pertains to.
   * @param channelId The ID of the channel this status pertains to.
   * @param eventId The ID of the event this status pertains to.
   * @param roster The roster for the event.
   * @param team The team assembled for the event.
   */
  case class Updated(guildId: String, channelId: String, eventId: String, roster: Roster, team: Team) extends Status

  /**
   * A message sent to the UI after an event is closed.
   *
   * @param guildId The ID of the guild this status pertains to.
   * @param channelId The ID of the channel this status pertains to.
   * @param eventId The ID of the event this status pertains to.
   * @param roster The roster for the event.
   * @param team The team assembled for the event.
   */
  case class Closed(guildId: String, channelId: String, eventId: String, roster: Roster, team: Team) extends Status

  /**
   * A message sent to the UI after an event is aborted.
   *
   * @param guildId The ID of the guild this status pertains to.
   * @param channelId The ID of the channel this status pertains to.
   * @param eventId The ID of the event this status pertains to.
   */
  case class Aborted(guildId: String, channelId: String, eventId: String) extends Status

}
