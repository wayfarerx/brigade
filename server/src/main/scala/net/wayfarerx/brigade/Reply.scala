/*
 * Reply.scala
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

/**
 * Base type for replies produced when sending events to a brigade.
 */
sealed trait Reply

/**
 * Definitions of the supported replies.
 */
object Reply {

  /**
   * Normalizes a sequence of replies, keeping only the first usage reply, the last status reply for each user and the
   * last team reply.
   *
   * @param replies The replies to normalize.
   * @return The normalized sequence of replies.
   */
  def normalize(replies: Vector[Reply]): Vector[Reply] = {
    val indexed = replies.zipWithIndex
    val firstUsage = indexed.collect {
      case item@(Usage, _) => item
    }.headOption.toVector
    val lastStatuses = indexed.collect {
      case (status@Status(_, _, _), index) => status -> index
    }.groupBy(_._1.user).values.map(_.last).toVector
    val lastTeam = indexed.collect {
      case item@(UpdateTeams(_, _), _) => item
      case item@(FinalizeTeams(_, _), _) => item
      case item@(AbandonTeams(_), _) => item
    }.lastOption.toVector
    (firstUsage ++ lastStatuses ++ lastTeam).sortBy(_._2).map(_._1)
  }

  /**
   * A reply that prints the brigade usage information.
   */
  case object Usage extends Reply

  /**
   * A reply that prints the status of a particular user in the brigade.
   *
   * @param user        The user that was queried.
   * @param assigned    The roles the user is assigned to.
   * @param volunteered The roles the user has volunteered for.
   */
  case class Status(user: User, assigned: Vector[Role], volunteered: Vector[Role]) extends Reply

  /**
   * Base type for replies that report the current teams.
   */
  sealed trait TeamsChanged extends Reply {

    /** Returns the ID of the display message to update. */
    def teamsMsgId: Message.Id

    /** Returns the teams that have been assembled for the brigade. */
    def teams: Vector[Team]

  }

  /**
   * Extractor for replies that change the published brigade.
   */
  object TeamsChanged {

    /**
     * Extracts the specified reply.
     *
     * @param reply The reply to extract.
     * @return The contents of the specified reply.
     */
    def unapply(reply: TeamsChanged): Option[(Message.Id, Vector[Team])] =
      Some(reply.teamsMsgId -> reply.teams)

  }

  /**
   * A reply that updates the listing of teams in a brigade.
   *
   * @param teamsMsgId The ID of the display message to update.
   * @param teams      The teams that have been assembled for the brigade.
   */
  case class UpdateTeams(teamsMsgId: Message.Id, teams: Vector[Team]) extends TeamsChanged

  /**
   * A reply that finalizes the listing of teams in a brigade.
   *
   * @param teamsMsgId The ID of the display message to update.
   * @param teams      The teams that have been assembled for the brigade.
   */
  case class FinalizeTeams(teamsMsgId: Message.Id, teams: Vector[Team]) extends TeamsChanged

  /**
   * A reply that abandons the listing of teams in a brigade.
   *
   * @param teamsMsgId The ID of the display message to update.
   */
  case class AbandonTeams(teamsMsgId: Message.Id) extends Reply

}
