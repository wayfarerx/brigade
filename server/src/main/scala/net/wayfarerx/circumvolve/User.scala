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

package net.wayfarerx.circumvolve

/**
 * A user of the system identified by an ID.
 *
 * @param id The ID of the user in question.
 */
final class User private(val id: Long) extends AnyVal {

  /* Emit this user. */
  override def toString: String = User.encodeString(this)

}

/**
 * Factory for user objects.
 */
object User extends ValueCompanion[User] {

  /**
   * Creates a user with the specified ID.
   *
   * @param id The ID of the user to create.
   * @return A user with the specified ID.
   */
  def apply(id: Long): User = new User(id)

  /**
   * Attempts to create a user from the specified string.
   *
   * @param id The string to parse as a user.
   * @return A user if one could be found in the specified string.
   */
  def apply(id: String): User = decodeString(id)

  /**
   * Extracts the ID of a user if it exists.
   *
   * @param user The user to extract the ID of.
   * @return The ID of the user if it exists.
   */
  def unapply(user: User): Option[Long] = Some(user.id)

  /* Decode a user. */
  override protected def decode(string: String): User = new User(string.toLong)

  /* Encode the user. */
  override protected def encode(value: User): String = value.id.toString

}
