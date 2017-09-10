/*
 * Team.scala
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

package net.wayfarerx.circumvolve.model

/**
 * A team of members in roles derived from a roster as well as any backup members and roles.
 *
 * @param members The members that have been assigned to a team by role.
 * @param backups The backup members and the roles they support.
 */
case class Team private[model] (members: Map[Role, Vector[Member]], backups: Map[Member, Vector[Role]]) {

  /**
   * Adds a member to this team under the specified role.
   *
   * @param role The role to add the member under.
   * @param member The member to add to this team.
   * @return This team with the supplied member added under the specified role.
   */
  def add(role: Role, member: Member): Team =
    copy(members = members + (role -> (members.getOrElse(role, Vector()) :+ member)))

}
