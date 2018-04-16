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

sealed trait Event {

  /** The instant that this event occurred. */
  def timestamp: Long

}

object Event {

  implicit val EventOrder: Ordering[Event] = _.timestamp compare _.timestamp

  case class Initialize(timestamp: Long) extends Event

  case class UpdateDirectives(timestamp: Long, messages: Vector[Message]) extends Event

  case class HandleMessage(timestamp: Long, message: Message) extends Event

  case class BrigadeLoaded(timestamp: Long, brigade: Brigade) extends Event

  case class HistoryLoaded(timestamp: Long, history: History) extends Event

  case class MessageSent(timestamp: Long, id: Message.Id) extends Event

  case class MessagesScanned(timestamp: Long, directives: Option[UpdateDirectives], messages: Vector[HandleMessage])
    extends Event

}
