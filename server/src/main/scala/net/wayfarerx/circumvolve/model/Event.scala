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
 * @param name    The name of this event.
 * @param roster  The roster for this event.
 * @param history The history of teams for this event.
 */
case class Event(name: String, roster: Option[Roster] = None, history: History = History()) {

  /**
   * Opens this event by assigning or reassigning the available slots.
   *
   * @param slots The slots that are available to fill in this event.
   * @return A copy of this event with the specified slots available.
   */
  def open(slots: Map[Role, Int]): Event = roster match {
    case Some(r) => copy(roster = Some(r.copy(slots = slots)))
    case None => copy(roster = Some(Roster(slots)))
  }

  /**
   * Aborts this event, discarding any roster that is associated with it.
   *
   * @return A copy of this event with no roster.
   */
  def abort(): Event =
    copy(roster = None)

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
