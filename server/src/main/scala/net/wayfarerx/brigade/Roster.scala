/*
 * Roster.scala
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
 * A roster describing the rules for building teams.
 *
 * @param slots       The mapping of roles to the number of users needed in that role.
 * @param assignments The user and role assignments in the order they occurred.
 * @param volunteers  The user and role volunteers in the order they occurred.
 */
case class Roster(
  slots: ListMap[Role, Int],
  assignments: Vector[(User, Role)] = Vector(),
  volunteers: Vector[(User, Role)] = Vector()
) {

  /**
   * Builds teams by first normalizing this roster then recursively building teams.
   *
   * @param config The build configuration.
   * @return The collection of teams that were built and the users that were left unassigned.
   */
  def buildTeams(config: Roster.Config): (Vector[Team], Vector[User]) = {
    val _slots = slots filter (_._2 > 0)
    val assigned = collection.mutable.HashSet[User]()
    var assign = Vector[(User, Role)]()
    for ((user, role) <- assignments) if (_slots.contains(role) && assigned.add(user)) assign :+= user -> role
    Roster.build(
      Roster(_slots, assign, volunteers.filter(v => (_slots contains v._2) && !(assigned contains v._1)).distinct),
      config,
      Vector()
    )
  }

}

/**
 * Definitions associated with solving rosters.
 */
object Roster {

  /**
   * Base type for team building configurations.
   */
  sealed trait Config

  /**
   * The team building configuration that ignores any history.
   */
  case object Fill extends Config

  /**
   * The team building configuration searches a history.
   *
   * @param history The history to work with.
   */
  case class Rotate(history: History) extends Config {

    /** The calculated scores for each team member and role in the selected history. */
    private[Roster] lazy val scoring = history.teams.reverse.zipWithIndex.flatMap {
      case (teams, score) => teams.flatMap(_.members.map(_ -> (score + 1)))
    }.flatMap {
      case ((role, users), score) => users map (_ -> role -> score)
    }.groupBy(_._1).mapValues(_.map(t => t._2).sum)

  }

  /**
   * Recursivly builds as many teams as possible from the given roster and configuration.
   *
   * @param roster   The roster to build from.
   * @param config   The configuration to honor.
   * @param previous The previously built teams.
   * @return The collection of teams and the unassigned users.
   */
  @annotation.tailrec
  private def build(roster: Roster, config: Roster.Config, previous: Vector[Team]): (Vector[Team], Vector[User]) = {
    val assignedRoles = roster.assignments.groupBy(_._2).mapValues(_.map(_._1))
    val assigned = roster.slots map { case (k, v) => k -> assignedRoles.getOrElse(k, Vector()).take(v) }
    Roster.Candidate.solve(
      Team(assigned),
      roster.slots map { case (k, v) => k -> (v - assigned(k).length) },
      Candidate.from(roster.volunteers, config)
    ) match {
      case Some(team) =>
        val members = team.members.values.flatten.toSet
        build(Roster(
          roster.slots,
          roster.assignments filterNot (members contains _._1),
          roster.volunteers filterNot (members contains _._1)
        ), config, previous :+ team)
      case None =>
        previous -> (roster.assignments.map(_._1) ++ roster.volunteers.map(_._1)).distinct
    }
  }

  /**
   * Represents a single candidate for inclusion in a team.
   *
   * @param user       The member user volunteered.
   * @param role       The role that was volunteered for.
   * @param score      The score derived from previous teams where the member filled the specified role.
   * @param preference The preference that the member showed for the specified role.
   */
  private case class Candidate(user: User, role: Role, score: Int, preference: Int)

  /**
   * Factory for collections of candidates.
   */
  private object Candidate {

    /**
     * Converts a collection of volunteers to a collection of candidates taking the event history into account.
     *
     * @param volunteers The volunteers for the event.
     * @param config     The configuration for scoring the history of member participation in the event.
     * @return A collection of candidates for the event.
     */
    private[Roster] def from(volunteers: Vector[(User, Role)], config: Config): Vector[Candidate] = {
      val preferences = volunteers.groupBy(_._1).mapValues(_.map(_._2).zipWithIndex.toMap)
      val candidates = for ((user, role) <- volunteers) yield {
        val score = config match {
          case Fill => 0
          case scores@Rotate(_) => scores.scoring.getOrElse(user -> role, 0)
        }
        Candidate(user, role, score, preferences(user)(role))
      }
      candidates sortBy (_.score)
    }

    /**
     * Attempts to fill all of the specified openings on the supplied team from a collection of candidates.
     *
     * @param team       The team that has already been assembled.
     * @param openings   The number of openings per role that remain.
     * @param candidates The candidates available to fill openings with.
     * @return The assembled team.
     */
    @annotation.tailrec
    private[Roster] def solve(team: Team, openings: Map[Role, Int], candidates: Vector[Candidate]): Option[Team] =
      if (openings.values.sum == 0) Some(team) else selectRole(openings, candidates) match {
        case Some(role) =>
          val user = chooseUser(candidates filter (_.role == role))
          solve(
            team.copy(members = team.members get role match {
              case Some(users) => team.members + ((role, users :+ user))
              case None => team.members + ((role, Vector(user)))
            }),
            openings + (role -> (openings(role) - 1)),
            candidates filterNot (_.user == user))
        case None => None
      }

    /**
     * Selects the next role to fill from the available candidates.
     *
     * @param openings   The openings for each role to fill.
     * @param candidates The candidates to fill roles from.
     * @return The next role to fill or none if no more roles can be filled.
     */
    private def selectRole(openings: Map[Role, Int], candidates: Vector[Candidate]): Option[Role] = {
      val filtered = candidates filter (c => openings(c.role) > 0)
      if (filtered.isEmpty) None else {
        val preference = filtered.map(_.preference).min
        val withPreference = filtered filter (_.preference == preference)
        val deltas = for {
          (role, requested) <- openings if requested > 0
          available = withPreference.count(_.role == role) if available > 0
        } yield role -> (requested - available)
        if (deltas.isEmpty) None else Some(deltas.maxBy(_._2)._1)
      }
    }

    /**
     * Choose a user at or above the current depth or continue down.
     *
     * @param from  The candidates to choose from.
     * @param depth The depth to search candidates' preferences.
     * @return The most desirable user.
     */
    @annotation.tailrec
    private def chooseUser(from: Vector[Candidate], depth: Int = 0): User = {
      val (current, next) = from partition (_.preference <= depth)
      if (current.nonEmpty) current.head.user else chooseUser(next, depth + 1)
    }

  }

}