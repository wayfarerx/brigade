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

package net.wayfarerx.circumvolve.api

/**
 * Base type for the supported external commands.
 */
sealed trait Command

/**
 * Implementations of the possible external commands.
 */
object Command {

  /**
   * Opens a roster with the specified roles and counts.
   *
   * @param slots The mapping of required role names to the number of members needed per role.
   */
  case class Open(slots: Map[String, Int]) extends Command

  /**
   * Abandons any in-progress roster.
   */
  case object Abort extends Command

  /**
   * Abandons the in-progress roster and creates a team.
   */
  case object Close extends Command

  /**
   * Assigns members to the specified roles.
   *
   * @param assignments The collection of member names and their assigned role names.
   */
  case class Assign(assignments: Vector[(String, String)]) extends Command

  /**
   * Releases the specified members from their assigned roles.
   *
   * @param memberNames The mapping of role names to lists of member names.
   */
  case class Release(memberNames: Set[String]) extends Command

  /**
   * Volunteers the specified member for the supplied roles.
   *
   * @param memberName The name of the member that is volunteering.
   * @param roleNames The names of the roles that are being volunteered for.
   */
  case class Volunteer(memberName: String, roleNames: Vector[String]) extends Command

  /**
   * Drops a member with the specified name from the supplied roles in the in-progress roster.
   *
   * @param memberName The name of the member that is dropping.
   * @param roleNames The names of the roles that are being dropped.
   */
  case class Drop(memberName: String, roleNames: Vector[String]) extends Command

}
