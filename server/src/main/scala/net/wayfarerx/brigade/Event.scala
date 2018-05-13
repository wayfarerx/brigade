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

import akka.actor.typed.ActorRef

/**
 * Base type for events sent to or from a channel.
 */
sealed trait Event {

  /** The instant that this event occurred. */
  def timestamp: Long

}

/**
 * Definition of the supported events and associated types.
 */
object Event {

  /** The implicit ordering of events by timestamp. */
  implicit val EventOrder: Ordering[Event] = _.timestamp compare _.timestamp

  /**
   * Extracts the data from the specified event.
   *
   * @param event The event to extract from.
   * @return The data extracted from the specified event.
   */
  def unapply(event: Event): Option[Long] = Some(event.timestamp)

  /**
   * Base type for events sent to a channel.
   */
  sealed trait Incoming extends Event

  /**
   * Extractor for incoming events.
   */
  object Incoming {

    /**
     * Extracts the data from the specified incoming event.
     *
     * @param incoming The incoming event to extract from.
     * @return The data extracted from the specified incoming event.
     */
    def unapply(incoming: Incoming): Option[Long] = Some(incoming.timestamp)

  }

  /**
   * Base type for events sent from a channel.
   */
  sealed trait Outgoing extends Event

  /**
   * Extractor for outgoing events.
   */
  object Outgoing {

    /**
     * Extracts the data from the specified outgoing event.
     *
     * @param outgoing The outgoing event to extract from.
     * @return The data extracted from the specified outgoing event.
     */
    def unapply(outgoing: Outgoing): Option[Long] = Some(outgoing.timestamp)

  }

  /**
   * Initializes a channel.
   *
   * @param session   The session that was loaded if one was found.
   * @param timestamp The instant that this event occurred.
   */
  case class Initialize(
    session: Option[Brigade.Session],
    timestamp: Long
  ) extends Incoming

  /**
   * Base type for signal events.
   */
  sealed trait Signal extends Incoming

  /**
   * Extractor for signal events.
   */
  object Signal {

    /**
     * Extracts the data from the specified signal event.
     *
     * @param signal The signal event to extract from.
     * @return The data extracted from the specified signal event.
     */
    def unapply(signal: Signal): Option[Long] = Some(signal.timestamp)

  }

  /**
   * Reconfigures a channel with the specified messages.
   *
   * @param messages  The messages that configure the channel.
   * @param timestamp The instant that this event occurred.
   */
  case class Configure(
    messages: Vector[Message],
    timestamp: Long
  ) extends Signal

  /**
   * Submits a message to a channel.
   *
   * @param message   The message to submit to the channel.
   * @param timestamp The instant that this event occurred.
   */
  case class Submit(
    message: Message,
    timestamp: Long
  ) extends Signal

  /**
   * Base type for effect events.
   */
  sealed trait Effect extends Outgoing

  /**
   * Extractor for effect events.
   */
  object Effect {

    /**
     * Extracts the data from the specified effect event.
     *
     * @param effect The effect event to extract from.
     * @return The data extracted from the specified effect event.
     */
    def unapply(effect: Effect): Option[Long] = Some(effect.timestamp)

  }

  /**
   * Saves the brigade's session.
   *
   * @param channelId The ID of the channel to save the session for.
   * @param session   The session to save.
   * @param timestamp The instant that this event occurred.
   */
  case class SaveSession(
    channelId: Channel.Id,
    session: Brigade.Session,
    timestamp: Long
  ) extends Effect

  /**
   * Prepends to the persistent history of the specified channel.
   *
   * @param channelId The ID of the channel to prepend to the history of.
   * @param teams     The team set to prepend to the channel's history.
   * @param timestamp The instant that this event occurred.
   */
  case class PrependToHistory(
    channelId: Channel.Id,
    teams: Vector[Team],
    timestamp: Long
  ) extends Effect

  /**
   * A message that instructs the server to post replies.
   *
   * @param channelId The ID of the channel to post in.
   * @param replies   The replies to post.
   * @param timestamp The instant that this event occurred.
   */
  case class PostReplies(
    channelId: Channel.Id,
    replies: Vector[Reply],
    timestamp: Long
  ) extends Effect

  /**
   * Base type for events that expect a response.
   */
  sealed trait Request[R <: Response] extends Outgoing {

    /** The actor to respond to. */
    def respondTo: ActorRef[R]

  }

  /**
   * Extractor for queries.
   */
  object Request {

    /**
     * Extracts the data from the specified request.
     *
     * @param request The request to extract from.
     * @return The data extracted from the specified request.
     */
    def unapply[R <: Response](request: Request[R]): Option[(ActorRef[R], Long)] =
      Some(request.respondTo -> request.timestamp)

  }

  /**
   * Notifies a channel that messages have been loaded.
   *
   * @param channelId    The if of the channel to load the messages for.
   * @param lastModified The last known time messages were received.
   * @param respondTo    The actor to respond to.
   * @param timestamp    The instant that this event occurred.
   */
  case class LoadMessages(
    channelId: Channel.Id,
    lastModified: Long,
    respondTo: ActorRef[MessagesLoaded],
    timestamp: Long
  ) extends Request[MessagesLoaded]

  /**
   * A request that loads a channel's history.
   *
   * @param channelId The if of the channel to load the history for.
   * @param depth     The number of recent team sets to load.
   * @param respondTo The actor to respond to.
   * @param timestamp The instant that this event occurred.
   */
  case class LoadHistory(
    channelId: Channel.Id,
    depth: Int,
    respondTo: ActorRef[HistoryLoaded],
    timestamp: Long
  ) extends Request[HistoryLoaded]

  /**
   * A message that instructs the server to prepare a message for displaying teams.
   *
   * @param channelId The ID of the channel to post to.
   * @param respondTo The actor to respond to.
   * @param timestamp The instant that this event occurred.
   */
  case class PrepareTeams(
    channelId: Channel.Id,
    respondTo: ActorRef[TeamsPrepared],
    timestamp: Long
  ) extends Request[TeamsPrepared]

  /**
   * Base type for events that respond to requests.
   */
  sealed trait Response extends Incoming

  /**
   * Extractor for queries.
   */
  object Response {

    /**
     * Extracts the data from the specified response.
     *
     * @param response The response to extract from.
     * @return The data extracted from the specified response.
     */
    def unapply(response: Response): Option[Long] = Some(response.timestamp)

  }

  /**
   * Notifies a channel that messages have been loaded.
   *
   * @param configuration The messages that configure the brigade.
   * @param submissions   The messages to submit to the brigade.
   * @param timestamp     The instant that this event occurred.
   */
  case class MessagesLoaded(
    configuration: Option[Configure],
    submissions: Vector[Submit],
    timestamp: Long
  ) extends Response

  /**
   * Notifies a channel that a history has been loaded.
   *
   * @param history   The history that was loaded.
   * @param timestamp The instant that this event occurred.
   */
  case class HistoryLoaded(
    history: History,
    timestamp: Long
  ) extends Response

  /**
   * Notifies a channel that a team display message is available.
   *
   * @param messageId The ID of the team display message.
   * @param timestamp The instant that this event occurred.
   */
  case class TeamsPrepared(
    messageId: Message.Id,
    timestamp: Long
  ) extends Response

}
