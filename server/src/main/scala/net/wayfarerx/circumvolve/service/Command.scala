/*
 * Command.scala
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

import net.wayfarerx.circumvolve.model_old.{User, Role}

/**
 * Base type for commands sent from the connection to a guild.
 */
sealed trait Command {

  /** The ID of the channel this command pertains to. */
  def channelId: String

}

/**
 * Implementations of the possible commands.
 */
object Command {

  /**
   * Opens a roster with the specified roles and counts.
   *
   * @param channelId The ID of the channel this command pertains to.
   * @param eventId   The ID assigned to the new incarnation of the event.
   * @param slots     The mapping of required roles to the number of members needed per role.
   */
  case class Open(channelId: String, eventId: String, slots: Vector[(Role, Int)]) extends Command

  /**
   * Abandons any in-progress roster.
   *
   * @param channelId The ID of the channel this command pertains to.
   */
  case class Abort(channelId: String) extends Command

  /**
   * Completes the in-progress roster and creates a team.
   *
   * @param channelId The ID of the channel this command pertains to.
   */
  case class Close(channelId: String) extends Command

  /**
   * Assigns members to the specified roles.
   *
   * @param channelId   The ID of the channel this command pertains to.
   * @param assignments The collection of members and their assigned roles.
   */
  case class Assign(channelId: String, assignments: Vector[(User, Role)]) extends Command

  /**
   * Releases the specified members from their assigned roles.
   *
   * @param channelId The ID of the channel this command pertains to.
   * @param members   The mapping of roles to lists of members.
   */
  case class Release(channelId: String, members: Set[User]) extends Command

  /**
   * Volunteers the specified member for the supplied roles.
   *
   * @param channelId The ID of the channel this command pertains to.
   * @param member    The member that is volunteering.
   * @param roles     The roles that are being volunteered for.
   */
  case class Volunteer(channelId: String, member: User, roles: Vector[Role]) extends Command

  /**
   * Drops a member from certain roles in the in-progress roster.
   *
   * @param channelId    The ID of the channel this command pertains to.
   * @param member       The member that is dropping.
   * @param limitToRoles The only roles to drop or empty to drop all roles.
   */
  case class Drop(channelId: String, member: User, limitToRoles: Vector[Role]) extends Command

  /**
   * Queries the roles a member has volunteered for in the in-progress roster.
   *
   * @param channelId The ID of the channel this command pertains to.
   * @param messageId The ID of the message that contained the query.
   * @param member    The member to query the volunteered roles for.
   */
  case class Query(channelId: String, messageId: String, member: User) extends Command

}
