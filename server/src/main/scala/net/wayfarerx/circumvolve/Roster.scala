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

package net.wayfarerx.circumvolve

import scala.collection.immutable.ListMap

/**
 * A roster describing the rules for building teams.
 *
 * @param slots The mapping of roles to the number of users needed in that role.
 * @param assignments The user and role assignments in the order they occurred.
 * @param volunteers The user and role volunteers in the order they occurred.
 */
case class Roster(
  slots: ListMap[Role, Int],
  assignments: Vector[(User, Role)] = Vector(),
  volunteers: Vector[(User, Role)] = Vector()
)
