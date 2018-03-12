/*
 * Solver.scala
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
 * Implementation of the team solving algorithm and data structures.
 */
object Solver {

  /**
   * Derives a team from this solver's event roster and event history.
   *
   * @param roster  The event roster to solve for.
   * @param history The event history to solve for.
   * @return A team from this solver's event roster and event history.
   */
  def apply(roster: Roster, history: History): Team = {
    val normal = roster.normalized
    val team = (Team(normal.slots.map(_._1 -> Vector[User]()), Vector()) /: normal.assignments) { (t, a) =>
      t.add(a._2, a._1)
    }
    solve(team,
      for ((r, c) <- normal.slots.toMap) yield r -> (team.members find (_._1 == r) map (c - _._2.size) getOrElse c),
      Candidate.from(normal.volunteers, history))
  }

  /**
   * Attempts to fill all of the specified openings on the supplied team from a collection of candidates.
   *
   * @param team The team that has already been assembled.
   * @param openings The number of openings per role that remain.
   * @param candidates The candidates available to fill openings with.
   * @return The fully assembled team.
   */
  @annotation.tailrec
  private def solve(team: Team, openings: Map[Role, Int], candidates: Vector[Candidate]): Team =
    selectRole(openings, candidates) match {
      case Some(role) =>
        val member = fillRole(role, candidates)
        solve(team.add(role, member),
          openings + (role -> (openings(role) - 1)),
          candidates filterNot (_.member == member))
      case None =>
        val backups = candidates groupBy (_.member) mapValues (_ map { candidate =>
          candidate.role -> candidate.preference
        } sortBy (_._2) map (_._1))
        team.copy(backups = candidates.map(_.member).distinct.map(m => m -> backups(m)))
    }

  /**
   * Selects the next role to fill from the available candidates.
   *
   * @param openings The openings for each role to fill.
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
   * Chooses a member to fill a role from the specified candidates.
   *
   * @param role The role to fill.
   * @param candidates The candidates to choose from.
   * @return A member to fill the specified role.
   */
  private def fillRole(role: Role, candidates: Vector[Candidate]): User =
    chooseMember(candidates filter (_.role == role))

  /**
   * Choose a member at or above the current depth or continue down.
   *
   * @param from The candidates to choose from.
   * @param depth The depth to search candidates' preferences.
   * @return The most desirable member.
   */
  @annotation.tailrec
  private def chooseMember(from: Vector[Candidate], depth: Int = 0): User = {
    val (current, next) = from partition (_.preference <= depth)
    if (current.nonEmpty) current.head.member else chooseMember(next, depth + 1)
  }

  /**
   * Represents a single candidate for inclusion in a team.
   *
   * @param member     The member that volunteered.
   * @param role       The role that was volunteered for.
   * @param score      The score derived from previous teams where the member filled the specified role.
   * @param preference The preference that the member showed for the specified role.
   */
  private case class Candidate(member: User, role: Role, score: Int, preference: Int)

  /**
   * Factory for collections of candidates.
   */
  private object Candidate {

    /**
     * Converts a collection of volunteers to a collection of candidates taking the event history into account.
     *
     * @param volunteers The volunteers for the event.
     * @param history    The history of member participation in the event.
     * @return A collection of candidates for the event.
     */
    def from(volunteers: Vector[(User, Role)], history: History): Vector[Candidate] = {
      val preferences = volunteers.groupBy(_._1).mapValues(_.map(_._2).zipWithIndex.toMap)
      val candidates = for {
        (member, role) <- volunteers
        scores = for {
          (team, index) <- history.teams.reverse.zipWithIndex
          (_, members) <- team.members.find(_._1 == role) if members contains member
        } yield index + 1
      } yield Candidate(member, role, scores.sum, preferences(member)(role))
      candidates sortBy (_.score)
    }

  }

}
