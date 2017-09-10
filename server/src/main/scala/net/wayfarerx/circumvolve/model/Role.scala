/*
 * Role.scala
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
 * A role that members of a team fill.
 *
 * @param name The name of the role in question.
 */
final class Role(val name: String) {

  /* Test for case-insensitive equality. */
  override def equals(that: Any): Boolean = that match {
    case Role(thatName) => name.equalsIgnoreCase(thatName)
    case _ => false
  }

  /* Hash based on case-insensitive equality. */
  override def hashCode(): Int =
    getClass.hashCode ^ name.toLowerCase.hashCode

  /* Return this role as a string. */
  override def toString: String =
    s"Role($name)"

}

/**
 * Factory for team roles.
 */
object Role {

  /**
   * Creates a new role.
   *
   * @param name The name of the role in question.
   * @return A new role.
   */
  def apply(name: String): Role =
    new Role(name)

  /**
   * Extracts a role.
   *
   * @param role The role to extract.
   * @return The name of this role or none.
   */
  def unapply(role: Role): Option[String] =
    Some(role.name)

}
