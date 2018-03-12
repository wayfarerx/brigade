/*
 * Command.scala
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

package net.wayfarerx.circumvolve

/**
 * Base type for commands extracted from messages.
 */
sealed trait Command

/**
 * Implementations of the supported commands.
 */
object Command {

  def extract(message: Message): Vector[Command] = {


    ???
  }

  /**
   * Opens a roster with the specified roles and counts.
   *
   * @param slots The mapping of required roles to the number of users needed per role.
   */
  case class Open(slots: Vector[(Role, Int)]) extends Command

  /**
   * Abandons any in-progress roster.
   */
  case object Abort extends Command

  /**
   * Completes the in-progress roster and creates a team.
   */
  case object Close extends Command

  /**
   * Queries the roles a user has volunteered for in the in-progress roster.
   *
   * @param user The user to query the volunteered roles for.
   */
  case class Query(user: User) extends Command

  /**
   * A specialization of command that changes an in-progress event.
   */
  sealed trait Mutation extends Command

  /**
   * Assigns users to the specified roles.
   *
   * @param assignments The collection of users and their assigned roles.
   */
  case class Assign(assignments: Vector[(User, Role)]) extends Mutation

  /**
   * Releases the specified users from their assigned roles.
   *
   * @param users The users to be released.
   */
  case class Release(users: Set[User]) extends Mutation

  /**
   * Volunteers the specified user for the supplied roles.
   *
   * @param user  The user that is volunteering.
   * @param roles The roles that are being volunteered for.
   */
  case class Volunteer(user: User, roles: Vector[Role]) extends Mutation

  /**
   * Drops a user from certain roles in the in-progress roster.
   *
   * @param user  The user that is dropping.
   * @param roles The roles to drop or empty to drop all roles.
   */
  case class Drop(user: User, roles: Vector[Role]) extends Mutation

}
