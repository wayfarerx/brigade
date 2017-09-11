/*
 * Roster.scala
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
 * A roster for an event that is to be filled.
 *
 * @param slots       The available roles and the number of members required for each role.
 * @param assignments The assignments of specific roles to members.
 * @param volunteers  The roles that have been volunteered for by members.
 */
case class Roster(
  slots: Map[Role, Int] = Map(),
  assignments: Vector[(Member, Role)] = Vector(),
  volunteers: Vector[(Member, Role)] = Vector()) {

  /**
   * Normalizes this roster by removing empty slots, superfluous assignments and invalid volunteer roles.
   *
   * @return a normalized copy of this roster.
   */
  def normalized: Roster = {
    // Remove any roles with non-positive counts.
    val normalizedSlots = for ((role, count) <- slots if count > 0) yield role -> count

    // Remove any unneeded roles and any superfluous assignments by role.
    val filteredAssignments = for {
      (member, role) <- assignments if normalizedSlots.contains(role)
    } yield member -> role
    val boundedAssignments = for {
      (role, members) <- filteredAssignments.groupBy(_._2).mapValues(_.map(_._1))
    } yield role -> members.take(normalizedSlots(role)).toSet
    val normalizedAssignments = for {
      (member, role) <- filteredAssignments if boundedAssignments(role)(member)
    } yield member -> role

    // Remove any assigned volunteers or volunteers for roles that were not requested.
    val normalizedVolunteers = {
      for ((member, role) <- volunteers
           if !normalizedAssignments.exists(_._1 == member) && normalizedSlots.contains(role))
        yield member -> role
    }.distinct

    // Create the normalized roster.
    Roster(normalizedSlots, normalizedAssignments, normalizedVolunteers)
  }

}
