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

package net.wayfarerx.circumvolve.model_old

/**
 * A team of members in roles derived from a roster as well as any backup members and roles.
 *
 * @param members The members that have been assigned to a team by role.
 * @param backups The backup members and the roles they support.
 */
case class Team(members: Vector[(Role, Vector[User])], backups: Vector[(User, Vector[Role])]) {

  /**
   * Adds a member to this team under the specified role.
   *
   * @param role   The role to add the member under.
   * @param member The member to add to this team.
   * @return This team with the supplied member added under the specified role.
   */
  def add(role: Role, member: User): Team =
    copy(members = members.indexWhere(_._1 == role) match {
      case index if index >= 0 =>
        val membersInRole = members(index)._2
        if (membersInRole.contains(member)) members else
          (members.take(index) :+ (members(index)._1 -> (membersInRole :+ member))) ++ members.drop(index + 1)
      case _ =>
        members :+ (role, Vector(member))
    })

  /**
   * Writes this team to a string.
   *
   * @return This team written to a string.
   */
  def write(): String =
    writeJson[Team](this)

}

/**
 * Factory for team objects.
 */
object Team {

  /**
   * Reads a team from a string.
   *
   * @param string The string to read from.
   * @return The team that was read.
   */
  def read(string: String): Team =
    readJson[Team](string)

}
