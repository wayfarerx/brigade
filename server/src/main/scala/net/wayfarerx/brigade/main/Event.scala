/*
 * Event.scala
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

import sx.blah.discord.handle.obj.{IChannel, IGuild}
import sx.blah.discord.handle.impl.events._

/**
 * Base type for Discord events and derived events.
 */
sealed trait Event

/**
 * Definitions of the various events.
 */
object Event {

  /**
   * The initial connect event.
   */
  case object Connect extends Event

  /**
   * The final disconnect event.
   */
  case object Disconnect extends Event

  /**
   * Event when the bot has been added to a channel.
   *
   * @param channel The Discord channel the bot was added to.
   */
  case class AddedTo(channel: IChannel) extends Event

  /**
   * Event when the bot has been removed from a channel.
   *
   * @param channel The Discord channel the bot was removed from.
   */
  case class RemovedFrom(channel: IChannel) extends Event

  /**
   * The event that notifies the bot that a message was received.
   *
   * @param event The message event that was received.
   */
  case class Received(event: guild.channel.message.MessageReceivedEvent) extends Event

  /**
   * The event that notifies the bot that a message was updated.
   *
   * @param event The message event that was updated.
   */
  case class Updated(event: guild.channel.message.MessageUpdateEvent) extends Event

  /**
   * The event that notifies the bot that a message was deleted.
   *
   * @param event The message event that was deleted.
   */
  case class Deleted(event: guild.channel.message.MessageDeleteEvent) extends Event

  /**
   * The event that notifies the bot that a message was pinned.
   *
   * @param event The message event that was pinned.
   */
  case class Pinned(event: guild.channel.message.MessagePinEvent) extends Event

  /**
   * The event that notifies the bot that a message was unpinned.
   *
   * @param event The message event that was unpinned.
   */
  case class Unpinned(event: guild.channel.message.MessageUnpinEvent) extends Event

}
