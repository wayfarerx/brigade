/*
 * Storage.scala
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
 * Base class for event storage strategies.
 */
trait Storage {

  /**
   * Loads the roster for a specified guild and channel.
   *
   * @param guildId The ID of the guild to load for.
   * @param channelId The ID of the channel to load for.
   * @return The roster for a specified guild and channel.
   */
  def getRoster(guildId: String, channelId: String): Option[Roster]

  /**
   * Saves the roster for a specified guild and channel.
   *
   * @param guildId The ID of the guild to save for.
   * @param channelId The ID of the channel to save for.
   * @param roster The roster to save.
   */
  def putRoster(guildId: String, channelId: String, roster: Roster): Unit

  /**
   * Deletes the roster for a specified guild and channel.
   *
   * @param guildId The ID of the guild to delete for.
   * @param channelId The ID of the channel to delete for.
   */
  def deleteRoster(guildId: String, channelId: String): Unit

  /**
   * Lists the specified number of the most recently saved teams for a guild and channel.
   *
   * @param guildId The ID of the guild to load for.
   * @param channelId The ID of the channel to load for.
   * @param count The number of teams to load.
   * @return The requested collection of team IDs.
   */
  def listTeamIds(guildId: String, channelId: String, count: Int): Vector[String]

  /**
   * Loads the specified team information.
   *
   * @param guildId The ID of the guild to load for.
   * @param channelId The ID of the channel to load for.
   * @param teamId The ID of the team to load.
   * @return The specified team information.
   */
  def getTeam(guildId: String, channelId: String, teamId: String): Option[Team]

  /**
   * Saves the specified team information.
   *
   * @param guildId The ID of the guild to save for.
   * @param channelId The ID of the channel to save for.
   * @param teamId The ID of the team to save.
   * @param team The team information to save.
   */
  def putTeam(guildId: String, channelId: String, teamId: String, team: Team): Unit

}

/**
 * Implementation of the storage interface.
 */
object Storage {

  /**
   * A dummy storage instance that is always empty and never saves anything.
   */
  object Empty extends Storage {

    /* Always return none. */
    override def getRoster(guildId: String, channelId: String): Option[Roster] = None

    /* Do nothing. */
    override def putRoster(guildId: String, channelId: String, roster: Roster): Unit = ()

    /* Do nothing. */
    override def deleteRoster(guildId: String, channelId: String): Unit = ()

    /* Always return empty. */
    override def listTeamIds(guildId: String, channelId: String, count: Int): Vector[String] = Vector()

    /* Always return none. */
    override def getTeam(guildId: String, channelId: String, teamId: String): Option[Team] = None

    /* Do nothing. */
    override def putTeam(guildId: String, channelId: String, teamId: String, team: Team): Unit = ()

  }


}