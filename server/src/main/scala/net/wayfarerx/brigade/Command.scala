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

package net.wayfarerx.brigade

import scala.collection.immutable.ListMap

/**
 * Base type for commands extracted from messages.
 */
sealed trait Command

/**
 * Implementations of the supported commands.
 */
object Command {

  /**
   * Specifies a channel as hosting an event.
   *
   * @param admins The event administrators.
   */
  case class Event(admins: Set[User]) extends Command

  /**
   * Opens a roster with the specified roles and counts.
   *
   * @param slots The mapping of required roles to the number of users needed per role.
   */
  case class Open(slots: ListMap[Role, Int]) extends Command

  /**
   * Abandons any in-progress roster.
   */
  case object Abort extends Command

  /**
   * Completes the in-progress roster and creates a team.
   */
  case object Close extends Command

  /**
   * Requests the help message.
   */
  case object Help extends Command

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
   * @param user The user to assign.
   * @param role The role to assign the user to.
   */
  case class Assign(user: User, role: Role) extends Mutation

  /**
   * Releases the specified user from any assigned roles.
   *
   * @param user The user to be released.
   */
  case class Release(user: User) extends Mutation

  /**
   * Volunteers the specified user for the supplied role.
   *
   * @param user The user that is volunteering.
   * @param role The role that is being volunteered for.
   */
  case class Volunteer(user: User, role: Role) extends Mutation

  /**
   * Drops a user from the specified volunteered role.
   *
   * @param user The user that is dropping.
   * @param role The roles to drop.
   */
  case class Drop(user: User, role: Role) extends Mutation

  /**
   * Drops a user from the any volunteered roles.
   *
   * @param user The user that is dropping.
   */
  case class DropAll(user: User) extends Mutation

}
