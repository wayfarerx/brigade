/*
 * Messages.scala
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
package discord

import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.MessageTokenizer

/**
 * Driver for parsing Discord messages.
 */
object Messages {

  import Message._

  def decode(message: IMessage): Message = {
    val tokenizer = new MessageTokenizer(message)
    var results = Vector[Token]()
    while (tokenizer.hasNext) {
      if (tokenizer.hasNextMention) results :+= (tokenizer.nextMention() match {
        case user: MessageTokenizer.UserMentionToken => Mention(User(user.getMentionObject.getLongID))
        case role: MessageTokenizer.RoleMentionToken => Word(role.getContent)
        case channel: MessageTokenizer.ChannelMentionToken => Word(channel.getContent)
      })
      else results :+= Word(tokenizer.nextWord().getContent)
    }
    ??? // Message(results)
  }

}
