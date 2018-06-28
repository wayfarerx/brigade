/*
 * Utils.scala
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
package main

import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.MessageTokenizer

/**
 * Utilities used by the brigade application.
 */
object Utils {

  /**
   * Transforms a Discord message into a Brigade message.
   *
   * @param message The Discord message to transform.
   * @return The resulting Brigade message.
   */
  def transformMessage(message: IMessage): Message =
    if (message.getContent.isEmpty)
      Message(Message.Id(message.getLongID), User(message.getAuthor.getLongID))
    else {
      val tokens = new MessageTokenizer(message)

      @annotation.tailrec
      def translate(prefix: Vector[Message.Token]): Vector[Message.Token] = if (!tokens.hasNext) prefix else {
        if (tokens.hasNextEmoji) {
          tokens.nextEmoji()
          translate(prefix)
        } else if (tokens.hasNextInvite) {
          tokens.nextInvite()
          translate(prefix)
        } else if (tokens.hasNextMention) {
          tokens.nextMention() match {
            case user: MessageTokenizer.UserMentionToken =>
              translate(prefix :+ Message.Mention(User(user.getMentionObject.getLongID)))
            case _ =>
              translate(prefix)
          }
        } else translate(prefix :+ Message.Word(tokens.nextWord().getContent))
      }

      Message(Message.Id(message.getLongID), User(message.getAuthor.getLongID), translate(Vector()): _*)
    }

}
