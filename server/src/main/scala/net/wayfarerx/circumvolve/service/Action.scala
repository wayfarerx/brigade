/*
 * Action.scala
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
 * Base type for actions parsed from a message.
 */
sealed trait Action

/**
 * Implementations of the possible actions.
 */
object Action {

  /**
   * Opens a roster with the specified roles and counts.
   *
   * @param slots The mapping of required roles to the number of members needed per role.
   */
  case class Open(slots: Vector[(Role, Int)]) extends Action

  /**
   * Abandons any in-progress roster.
   */
  case object Abort extends Action

  /**
   * Completes the in-progress roster and creates a team.
   */
  case object Close extends Action

  /**
   * Assigns members to the specified roles.
   *
   * @param assignments The collection of members and their assigned roles.
   */
  case class Assign(assignments: Vector[(User, Role)]) extends Action

  /**
   * Releases the specified members from their assigned roles.
   *
   * @param members The mapping of roles to lists of members.
   */
  case class Release(members: Set[User]) extends Action

  /**
   * Offers the specified member for the supplied roles.
   *
   * @param member The name of the member that is volunteering.
   * @param roles  The roles that are being volunteered for.
   */
  case class Offer(member: User, roles: Vector[Role]) extends Action

  /**
   * Kicks a member from certain roles in the in-progress roster.
   *
   * @param member       The name of the member that is dropping.
   * @param limitToRoles The only roles to drop or empty to drop all roles.
   */
  case class Kick(member: User, limitToRoles: Vector[Role]) extends Action

  /**
   * Volunteers the specified member for the supplied roles.
   *
   * @param roles The roles that are being volunteered for.
   */
  case class Volunteer(roles: Vector[Role]) extends Action

  /**
   * Drops a member from certain roles in the in-progress roster.
   *
   * @param limitToRoles The only roles to drop or empty to drop all roles.
   */
  case class Drop(limitToRoles: Vector[Role]) extends Action

  /**
   * Queries the roles a has volunteered for.
   *
   * @param member The member to query or none to query the author.
   */
  case class Query(member: Option[User]) extends Action

  /**
   * An action that prints the help message.
   */
  case object Help extends Action

}
