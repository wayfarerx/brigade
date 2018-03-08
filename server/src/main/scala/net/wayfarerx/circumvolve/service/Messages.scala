/*
 * Messages.scala
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

import net.wayfarerx.circumvolve.model.{User, Role, Roster, Team}
import sx.blah.discord.handle.obj.IGuild

/**
 * Definitions of the messages sent to Discord.
 */
object Messages {

  /**
   * The message displayed when the bot is updating the roster.
   *
   * @return The message displayed when the bot is updating the roster.
   */
  def building: String =
    s"I'm setting up a new roster."

  /**
   * The message displayed when a member attempts an unauthorized action.
   *
   * @return The message displayed when a member attempts an unauthorized action.
   */
  def unauthorized: String =
    s"you're not authorized to do that."

  /**
   * The message displayed when the attempt to build a tem is aborted.
   *
   * @return The message displayed when the attempt to build a tem is aborted.
   */
  def aborted: String =
    s"This attempt to build a team was aborted."

  /**
   * The message displayed in response to a user's role query.
   *
   * @param guild  The Discord guild to look up users with.
   * @param author The member issued the query.
   * @param member The member that was queried.
   * @param roles  The roles that were found for the member.
   * @return The message displayed when the attempt to build a tem is aborted.
   */
  def queryResponse(guild: IGuild, author: User, member: User, roles: Vector[Role]): String = {
    val memberInfo = if (author == member) "you are" else
      s"the user ${guild.getClient.getUserByID(member.id.toLong).getDisplayName(guild)} is"
    if (roles.isEmpty) s"$memberInfo not volunteered for any roles."
    else if (roles.size == 1) s"$memberInfo volunteered for ${roles.head.name}."
    else s"$memberInfo volunteered for ${roles.init.map(_.name).mkString(", ")} & ${roles.last.name}."
  }

  /**
   * The message displayed when the attempt to build a tem is aborted.
   *
   * @param guild  The Discord guild to look up users with.
   * @param roster The roster to be filled.
   * @param team   The team selected to fill the roster.
   * @return The message displayed when the attempt to build a tem is aborted.
   */
  def show(guild: IGuild, roster: Roster, team: Team): String = {
    val template = roster.normalized
    val assignments = team.members.toMap
    val requirements = template.slots.toMap
    val builder = new StringBuilder("Here's the current roster:```")
    val output = for {
      role <- template.slots map (_._1)
      members = assignments(role)
      index <- 0 until requirements(role)
    } yield s"${role.name} ${index + 1}: " -> members.drop(index).headOption
    val max = output.map(_._1.length).max
    for ((label, member) <- output) {
      builder.append("\r\n")
      builder.append(label)
      for (_ <- 0 until max - label.length) builder.append(' ')
      member match {
        case Some(m) =>
          builder.append(guild.getClient.getUserByID(m.id.toLong).getDisplayName(guild))
        case None =>
          builder.append("--")
      }
    }
    builder.append("```\r\n")
    if (team.backups.nonEmpty) {
      builder.append("Here's the folks registered as backups:```")
      for ((member, roles) <- team.backups) {
        builder.append("\r\n")
        builder.append(guild.getClient.getUserByID(member.id.toLong).getDisplayName(guild))
        builder.append(":")
        roles foreach { role =>
          builder.append(role)
          builder.append(' ')
        }
      }
      builder.append("```\r\n")
    }
    builder.toString
  }

  /** The message displayed in response to the help action. */
  def help: String =
    """hi, I'm the Circumvolve team builder, progeny of the inimitable wayfarerx.
      |I see this channel has been set up to build teams, good work! Here's the commands you can use:
      |```
      | !open (!ROLE COUNT)+     -- Sets up the team roster.
      | !abort                   -- Abandons the roster and does not build a team.
      | !close                   -- Closes the roster and finalizes a team.
      | !assign (@USER !ROLE)+   -- Assigns members to roles in the team.
      | !release @USER+          -- Releases previously assigned members.
      | !offer @USER !ROLE+      -- Volunteers a member for one or more roles.
      | !kick @USER !ROLE*       -- Kicks a member from one or more roles (or all roles if you don't list any).```
      |Note that you have to be an event administrator to use the above commands. Here's the commands for everyone:
      |```
      | !ROLE+                   -- Volunteers YOU for one or more roles.
      | !drop !ROLE*             -- Drops YOU from one or more roles (or all roles if you don't list any).
      | !? @USER?                -- Lists the roles that @USER has volunteered for or YOUR roles if @USER is omitted.
      | !help                    -- Repeats this message for whenever you feel like you miss me.```
      |For a more detailed tutorial about how this bot works see https://github.com/wayfarerx/circumvolve.
    """.stripMargin

}
