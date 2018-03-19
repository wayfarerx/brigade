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
   * Requests the help message.
   */
  case object Help extends Command

  /**
   * Specifies a channel as hosting an event.
   *
   * @param admins The event administrators.
   */
  case class Event(admins: Set[User]) extends Command

  /**
   * A specialization of command that begins, interacts with or ends an event.
   */
  sealed trait Lifecycle extends Command

  /**
   * Extractor for lifecycle commands.
   */
  object Lifecycle {

    /** True for all lifecycle commands.*/
    def unapply(lifecycle: Lifecycle): Boolean = true

  }

  /**
   * Opens a roster with the specified roles and counts.
   *
   * @param slots The mapping of required roles to the number of users needed per role.
   */
  case class Open(slots: ListMap[Role, Int]) extends Lifecycle

  /**
   * A specialization of command that references an in-progress event.
   */
  sealed trait Transaction extends Lifecycle

  /**
   * Extractor for transaction commands.
   */
  object Transaction {

    /** True for all transaction commands.*/
    def unapply(transaction: Transaction): Boolean = true

  }

  /**
   * Queries the roles a user has volunteered for in the in-progress roster.
   *
   * @param user The user to query the volunteered roles for.
   */
  case class Query(user: User) extends Transaction

  /**
   * A specialization of transaction that changes an in-progress event.
   */
  sealed trait Mutation extends Transaction

  /**
   * Extractor for mutation commands.
   */
  object Mutation {

    /** True for all mutation commands.*/
    def unapply(mutation: Mutation): Boolean = true

  }

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

  /**
   * A specialization of command that terminates an in-progress event.
   */
  sealed trait Terminal extends Lifecycle

  /**
   * Extractor for terminal commands.
   */
  object Terminal {

    /** True for all terminal commands. */
    def unapply(terminal: Terminal): Boolean = true

  }

  /**
   * Abandons any in-progress roster.
   */
  case object Abort extends Terminal

  /**
   * Completes the in-progress roster and creates a team.
   */
  case object Close extends Terminal

}
