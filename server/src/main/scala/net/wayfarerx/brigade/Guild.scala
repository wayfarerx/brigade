/*
 * Guild.scala
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
 * Definition of a guild.
 *
 * @param id    The ID of this guild.
 * @param owner The owner of this guild.
 */
case class Guild(id: Guild.Id, owner: User)

/**
 * Defines the guild ID.
 */
object Guild {

  /**
   * The ID of a guild.
   *
   * @param value The underlying guild ID value.
   */
  final class Id private(val value: Long) extends AnyVal

  /**
   * Factory for guild IDs.
   */
  object Id {

    /**
     * Creates a new guild ID.
     *
     * @param value The underlying guild ID value.
     * @return A new guild ID.
     */
    def apply(value: Long): Id = new Id(value)

  }

}