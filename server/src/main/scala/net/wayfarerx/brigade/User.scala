/*
 * User.scala
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
 * A user of the system identified by an ID.
 *
 * @param id The ID of the user in question.
 */
final class User private(val id: Long) extends AnyVal {

  /* Return this user as a string. */
  override def toString: String = s"User($id)"

}

/**
 * Factory for user objects.
 */
object User {

  /**
   * Creates a user with the specified ID.
   *
   * @param id The ID of the user to create.
   * @return A user with the specified ID.
   */
  def apply(id: Long): User = new User(id)

  /**
   * Extracts the ID of a user if it exists.
   *
   * @param user The user to extract the ID of.
   * @return The ID of the user if it exists.
   */
  def unapply(user: User): Option[Long] = Some(user.id)

}
