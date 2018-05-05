/*
 * History.scala
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
 * A view of the history of teams that were created for an event.
 *
 * @param teams The sets of teams that were created for an event, most-recent first.
 */
final class History private (val teams: Vector[Vector[Team]]) extends AnyVal {

  /* Return this role as a string. */
  override def toString: String = s"History(${teams map (_.mkString("(", ", ", ")")) mkString ","})"

}

/**
 * Factory for team histories.
 */
object History {

  /**
   * Creates a new team history.
   *
   * @param teams The sets of teams in the history.
   * @return A new team history.
   */
  def apply(teams: Vector[Vector[Team]] = Vector()): History = new History(teams)

  /**
   * Extracts the sets of teams from the specified history.
   *
   * @param history The history to extract from.
   * @return The sets of teams in the specified history.
   */
  def unapply(history: History): Option[Vector[Vector[Team]]] = Some(history.teams)

}
