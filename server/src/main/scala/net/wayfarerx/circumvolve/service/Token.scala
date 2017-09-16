/*
 * Token.scala
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

package net.wayfarerx.circumvolve.service

import java.util.StringTokenizer
import java.util.regex.Pattern

import sx.blah.discord.handle.obj.IMessage

/**
 * Base class for tokens extracted from Discord messages.
 */
sealed trait Token

/**
 * Implementation of the message token type.
 */
object Token {

  /** The pattern used to identify mentions. */
  private val MentionPattern: Pattern = Pattern.compile("<@(\\d+)>")

  /**
   * Converts a message to an iterator of tokens.
   *
   * @param message The message to tokenize.
   * @return An iterator over the tokens in the specified message.
   */
  def iterate(message: String): Iterator[Token] = {
    val tokens = new StringTokenizer(message)
    new Iterator[Token] {

      override def hasNext: Boolean =
        tokens.hasMoreTokens

      override def next(): Token = {
        val token = tokens.nextToken()
        if (MentionPattern.matcher(token).matches()) Mention(token.substring(2, token.length - 1).toLong)
        else Word(token)
      }

    }
  }

  /**
   * A token that represents a regular word.
   *
   * @param content The content of the token.
   */
  case class Word(content: String) extends Token

  /**
   * A token that represents a user mention.
   *
   * @param userId The ID of the mentioned user.
   */
  case class Mention(userId: Long) extends Token

}
