/*
 * History.scala
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

package net.wayfarerx.brigade.model_old

/**
 * The history of teams that were created for an event.
 *
 * @param teams The teams that were created for an event, most-recent first.
 */
case class History (teams: Vector[Team] = Vector()) {

  /**
   * Includes the specified team as the most recent member of this history.
   *
   * @param team The team to prepend to this history.
   * @return A history that reflects this history prepended with the specified team.
   */
  def include(team: Team): History =
    copy(team +: teams)

}
