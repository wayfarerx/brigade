/*
 * Team.scala
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
 * A team of users assigned to roles.
 *
 * @param members The collection of users assigned to each role.
 */
final class Team private (val members: ListMap[Role, Vector[User]]) extends AnyVal {

  /* Return this role as a string. */
  override def toString: String = s"Team($members)"

}

/**
 * Factory for teams.
 */
object Team {

  /**
   * Creates a new team.
   *
   * @param members The collection of users assigned to each role.
   * @return A new team.
   */
  def apply(members: ListMap[Role, Vector[User]] = ListMap()): Team = new Team(members)

  /**
   * Extracts the members of the specified team.
   *
   * @param team The team to extract from.
   * @return The members of the specified team.
   */
  def unapply(team: Team): Option[ListMap[Role, Vector[User]]] = Some(team.members)

}
