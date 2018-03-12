/*
 * package.scala
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

package net.wayfarerx

import reflect.ClassTag

import java.text.Normalizer

/**
 * Global definitions for the circumvolve project.
 */
package object circumvolve {

  /**
   * Normalizes the specified string.
   *
   * @param string The string to normalize.
   * @return The normal form of the specified string.
   */
  def normalize(string: String): String = {
    Normalizer.normalize(string, Normalizer.Form.NFD)
      .replaceAll("""[^\p{ASCII}]+""", "")
      .replaceAll("""[\`\'\"\(\)\[\]\{\}\<\>]+""", "")
      .replaceAll("""[^a-zA-Z0-9]+""", "-")
      .replaceAll("""^[\-]+""", "")
      .replaceAll("""[\-]+$""", "")
      .toLowerCase
  }

  /**
   * Base class for value object companions.
   *
   * @tparam T The type of the value object.
   */
  abstract class ValueCompanion[T: ClassTag] {

    /** The pattern that identifies stringified objects. */
    private lazy val Pattern = s"""$typeName[(]([^)]+)[)]""".r

    /**
     * Converts a string prefixed by its type name to a value object.
     *
     * @param string The prefixed string to convert.
     * @return The decoded value object.
     */
    final def decodeString(string: String): T = string match {
      case Pattern(content) => decode(content)
      case content => decode(content)
    }

    /**
     * Converts a value object to a string prefixed by its type name.
     *
     * @param value The value object to convert.
     * @return The prefixed string.
     */
    final def encodeString(value: T) =
      s"$typeName(${encode(value)})"

    /**
     * Decodes an underlying value.
     *
     * @param string The string to encode.
     * @return The decoded value.
     */
    protected def decode(string: String): T

    /**
     * Encodes a value into a string.
     *
     * @param value The value to encode.
     * @return The encoded value.
     */
    protected def encode(value: T): String

    /** Returns the name of the value type. */
    private def typeName: String =
      implicitly[ClassTag[T]].runtimeClass.getSimpleName

  }

}
