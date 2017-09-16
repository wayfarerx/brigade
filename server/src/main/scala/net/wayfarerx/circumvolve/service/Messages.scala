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

import net.wayfarerx.circumvolve.model.{Roster, Team}
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
   * The message displayed when the attempt to build a tem is aborted.
   *
   * @param guild The Discord guild to look up users with.
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
          builder.append("@")
          builder.append(guild.getClient.getUserByID(m.id.toLong).getDisplayName(guild))
        case None =>
          builder.append("unassigned")
      }
    }
    builder.append("```\r\n").toString
  }

  /** The message displayed in response to the help action. */
  def help: String =
    """hi, I'm the Circumvolve team builder, built by the inimitable wayfarerx.
      |I see you already set up this channel to build teams, good work! Here's the commands you can use:
      |```
      | !open (!role count)+     -- Sets up the team roster.
      | !abort                   -- Abandons the roster and does not build a team.
      | !close                   -- Closes the roster and finalizes a team.
      | !assign (member !role)+  -- Assigns members to roles in the team.
      | !release member+         -- Releases previously assigned members.
      | !offer member !role+     -- Volunteers a member for one or more roles.
      | !kick member !role*      -- Kicks a member from one or more roles (or all roles if you don't list any).```
      |Note that you have to be an event administrator to use the above commands. Here's the commands for everyone:
      |```
      | !role+                   -- Volunteers YOU for one or more roles.
      | !drop !role*             -- Drops YOU from one or more roles (or all roles if you don't list any).
      | !help                    -- Repeats this message for whenever you feel like you miss me.```
    """.stripMargin

}
