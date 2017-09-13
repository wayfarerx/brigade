/*
 * Event.scala
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
 * The top-level event sign-up and history tracker.
 *
 * @param roster  The roster for this event.
 * @param history The history of teams for this event.
 */
case class Event(roster: Option[Roster] = None, history: History = History()) {

  /**
   * Opens this event by assigning or reassigning the available slots.
   *
   * @param eventId The ID assigned to the incarnation of the event.
   * @param slots   The slots that are available to fill in this event.
   * @return A copy of this event and its new roster.
   */
  def open(eventId: String, slots: Vector[(Role, Int)]): (Event, Roster) = roster match {
    case Some(r) =>
      val rr = r.copy(eventId = eventId, slots = slots)
      copy(roster = Some(rr)) -> rr
    case None =>
      val r = Roster(eventId, slots)
      copy(roster = Some(r)) -> r
  }

  /**
   * Assigns members to the specified roles in this roster.
   *
   * @param assignments The collection of members and their assigned roles.
   * @return A copy of this roster with the specified assignments.
   */
  def assign(assignments: Vector[(Member, Role)]): Event =
    copy(roster = roster map (r => r.copy(assignments = r.assignments ++ assignments)))

  /**
   * Releases the specified members from their assigned roles.
   *
   * @param members The members to release from their assigned roles.
   * @return A copy of this roster with the specified members released from their assigned roles.
   */
  def release(members: Set[Member]): Event =
    copy(roster = roster map (r => r.copy(assignments = r.assignments.filterNot(a => members(a._1)))))

  /**
   * Volunteers the specified member for the supplied roles.
   *
   * @param member The member that is volunteering.
   * @param roles  The roles that are being volunteered for.
   * @return A copy of this roster with the specified members volunteered for the supplied roles.
   */
  def volunteer(member: Member, roles: Vector[Role]): Event =
    copy(roster = roster map (r => r.copy(volunteers = r.volunteers ++ roles.map(member -> _))))

  /**
   * Drops a member from the specified roles in this roster.
   *
   * @param member       The member that is dropping.
   * @param limitToRoles The only roles to drop or empty to drop all roles.
   * @return A copy of this roster with the specified members dropping the supplied roles.
   */
  def drop(member: Member, limitToRoles: Vector[Role]): Event =
    copy(roster = roster map { r =>
      r.copy(volunteers = r.volunteers filterNot { v =>
        v._1 == member && (limitToRoles.isEmpty || limitToRoles.contains(v._2))
      })
    })

  /**
   * Aborts this event, discarding any roster that is associated with it.
   *
   * @return A copy of this event with no roster with the roster that was aborted.
   */
  def abort(): (Event, Option[Roster]) =
    copy(roster = None) -> roster

  /**
   * Closes this event, transforming its roster into a team if it exists, aborting otherwise.
   *
   * @return A copy of this event with no roster along side any team that was created for it.
   */
  def close(): (Event, Option[Team]) =
    roster map (Solver(_, history)) map { team =>
      copy(roster = None, history = history.include(team)) -> Some(team)
    } getOrElse this -> None

}
