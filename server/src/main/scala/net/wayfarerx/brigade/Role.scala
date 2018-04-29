/*
 * Role.scala
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
 * A role in a team that users can fill identified by an ID.
 *
 * @param id The ID of the role in question.
 */
final class Role private(val id: String) extends AnyVal {

  /* Return this role as a string. */
  override def toString: String = s"Role($id)"

}

/**
 * Factory for role objects.
 */
object Role {

  /**
   * Creates a new role.
   *
   * @param id The ID of the role in question.
   * @return A new role.
   */
  def apply(id: String): Role = new Role(id)

  /**
   * Extracts the ID of a role if it exists.
   *
   * @param role The role to extract the ID of.
   * @return The ID of the role if it exists.
   */
  def unapply(role: Role): Option[String] = Some(role.id)

}
