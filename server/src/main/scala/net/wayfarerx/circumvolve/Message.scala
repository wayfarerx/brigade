/*
 * Message.scala
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
 * Describes a message handled by the system.
 *
 * @param tokens The tokens contained in the message.
 */
case class Message(tokens: Vector[Message.Token])

/**
 * Definitions of the message tokens.
 */
object Message {

  /**
   * Base type for message tokens.
   */
  sealed trait Token

  /**
   * A token that mentions a user.
   *
   * @param user The user to mention.
   */
  case class Mention(user: User) extends Token

  /**
   * A token that represents a word.
   *
   * @param value The value of this word.
   */
  case class Word(value: String) extends Token

}
