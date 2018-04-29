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
 * Base type for commands that can be submitted to a brigade.
 */
trait Command

/**
 * Definitions of the supported command types.
 */
object Command {

  /**
   * Extracts the data from a command.
   *
   * @param cmd The command to extract.
   * @return True for all commands.
   */
  def unapply(cmd: Command): Boolean = true

  /**
   * A command that represents a user asking for help.
   */
  case object Help extends Command

  /**
   * A command that represents the opening of a roster for a brigade.
   *
   * @param slots      The slots available in a team.
   * @param teamsMsgId The ID of the message used to display teams if it is available.
   */
  case class Open(slots: ListMap[Role, Int], teamsMsgId: Option[Message.Id]) extends Command

  /**
   * A specialization of command that references an in-progress roster.
   */
  sealed trait Transaction extends Command

  /**
   * Extractor for transaction commands.
   */
  object Transaction {

    /**
     * Extracts the data from a transaction command.
     *
     * @param cmd The command to extract.
     * @return True for all transaction commands.
     */
    def unapply(cmd: Transaction): Boolean = true

  }

  /**
   * Queries the roles a user is assigned to or has volunteered for in the in-progress roster.
   *
   * @param user The user to query the roles for.
   */
  case class Query(user: User) extends Transaction

  /**
   * A specialization of transaction that changes an in-progress roster.
   */
  sealed trait Mutation extends Transaction

  /**
   * Extractor for mutation commands.
   */
  object Mutation {

    /**
     * Extracts the data from a mutation command.
     *
     * @param cmd The command to extract.
     * @return True for all mutation commands.
     */
    def unapply(cmd: Mutation): Boolean = true

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
   * A specialization of command that terminates an in-progress roster.
   */
  sealed trait Terminal extends Command

  /**
   * Extractor for terminal commands.
   */
  object Terminal {

    /**
     * Extracts the data from a terminal command.
     *
     * @param cmd The command to extract.
     * @return True for all terminal commands.
     */
    def unapply(cmd: Terminal): Boolean = true

  }

  /**
   * Abandons any in-progress roster.
   */
  case object Abort extends Terminal

  /**
   * Completes the in-progress roster and creates one or more teams.
   */
  case object Close extends Terminal

}