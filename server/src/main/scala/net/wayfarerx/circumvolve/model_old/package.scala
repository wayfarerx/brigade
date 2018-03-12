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

package net.wayfarerx.circumvolve

import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write}

/**
 * Shared definitions for the model package.
 */
package object model_old {

  /**
   * The global JSON formats.
   */
  implicit private[model_old] val JsonFormats: Formats = Serialization.formats(NoTypeHints) + new RoleSerializer

  /**
   * Reads an object from a string.
   *
   * @param json The string to read from.
   * @tparam T The type of object to read.
   * @return The object that was read from the string.
   */
  private[model_old] def readJson[T: Manifest](json: String): T =
    read(json)

  /**
   * Writes an object to a string.
   *
   * @param value The object to write.
   * @tparam T The type of object to write.
   * @return The string that was written.
   */
  private[model_old] def writeJson[T <: AnyRef](value: T): String =
    write(value)

  /**
   * Custom serializer for the non-case-class role object.
   */
  private[model_old] class RoleSerializer extends CustomSerializer[Role](_ => ( {
    case JObject(JField("name", JString(name)) :: Nil) => Role(name)
  }, {
    case role: Role => JObject(JField("name", JString(role.name)) :: Nil)
  }
  ))

}
