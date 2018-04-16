/*
 * Connection.scala
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

import language.implicitConversions

/**
 * Base type for connection messages to the server.
 */
sealed trait Connection

/**
 * Definitions of connection messages sent to the server.
 */
object Connection {

  /**
   * A message that instructs the server to post a message.
   *
   * @param channelId The ID of the channel to post to.
   * @param teams     The teams to post.
   * @param callback  The optional callback to invoke with the new message ID.
   */
  case class PostTeams(
    channelId: Channel.Id,
    teams: Vector[Team],
    callback: Message.Id => Unit
  ) extends Connection

  /**
   * A message that instructs the server to update the displayed teams.
   *
   * @param channelId The ID of the channel to update in.
   * @param messageId The ID of the message to update.
   * @param teams     The teams to update.
   */
  case class UpdateTeams(
    channelId: Channel.Id,
    messageId: Message.Id,
    teams: Vector[Team]
  ) extends Connection

  /**
   * A message that instructs the server to post replies.
   *
   * @param channelId The ID of the channel to post in.
   * @param replies   The replies to post.
   */
  case class PostReplies(
    channelId: Channel.Id,
    replies: Vector[Reply]
  ) extends Connection

  /**
   * A message that instructs the server to post the help message.
   *
   * @param channelId The ID of the channel to post in.
   */
  case class PostHelp(
    channelId: Channel.Id
  ) extends Connection

  /**
   * A message that instructs the server to return pinned messages as well as message events since the specified time.
   *
   * @param channelId      The ID of the channel to scan.
   * @param afterTimestamp The timestamp to return message events since.
   * @param callback       The callback to invoke with the scanned directives and messages.
   */
  case class ScanMessages(
    channelId: Channel.Id,
    afterTimestamp: Long,
    callback: (Option[Event.UpdateDirectives], Vector[Event.HandleMessage]) => Unit
  ) extends Connection

}
