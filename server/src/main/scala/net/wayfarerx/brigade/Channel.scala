/*
 * Channel.scala
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
import akka.actor.typed.scaladsl._

/**
 * An actor that manages the brigade for a channel.
 *
 * @param id       The ID of this channel.
 * @param owner    The owner of this channel.
 * @param outgoing The actor to send all outgoing events to.
 */
final class Channel private(id: Channel.Id, owner: User, outgoing: ActorRef[Event.Outgoing]) {

  import Channel._

  /**
   * Returns the behavior to use while waiting for the messages to be loaded for a channel.
   *
   * @param session The current session.
   * @param buffer  The event buffer to use.
   * @return The behavior to use once the messages have been loaded for a channel.
   */
  private def onMessagesLoaded(
    session: Brigade.Session,
    buffer: Buffer
  ): Behaviors.Immutable[Event.Incoming] = buffer.buffering {
    case (ctx, next, Event.MessagesLoaded(configuration, submissions, _)) => drain(
      ctx,
      Brigade(Set(), Configuration.Default, session),
      configuration.map(next :+ _).getOrElse(next) :+ (submissions: _*)
    )
  }

  /**
   * Returns the behavior to use while waiting for the team history to be loaded.
   *
   * @param brigade    The current brigade.
   * @param buffer     The event buffer to use.
   * @param organizers The organizers for the brigade.
   * @return The behavior to use once the team history has been loaded.
   */
  private def onHistoryLoaded(
    brigade: Brigade,
    buffer: Buffer,
    organizers: Set[User]
  ): Behaviors.Immutable[Event.Incoming] = buffer.buffering {
    case (ctx, next, Event.HistoryLoaded(history, timestamp)) =>
      val (result, replies) = continueConfigure(brigade, organizers, history, timestamp)
      finishEvent(brigade, result, replies, timestamp)
      drain(ctx, brigade, next)
  }

  /**
   * Returns the behavior to use while waiting for a teams message to be prepared.
   *
   * @param brigade  The current brigade.
   * @param buffer   The event buffer to use.
   * @param event    The submit event that is pending.
   * @param commands The commands extracted from the submit event.
   * @return The behavior to use while waiting for a teams message to be prepared.
   */
  private def onTeamsPrepared(
    brigade: Brigade,
    buffer: Buffer,
    event: Event.Submit,
    commands: Vector[Command]
  ): Behaviors.Immutable[Event.Incoming] = buffer.buffering {
    case (ctx, next, Event.TeamsPrepared(teamsMsgId, timestamp)) =>
      val (result, replies) = continueSubmit(brigade, event, commands, teamsMsgId, timestamp)
      finishEvent(brigade, result, replies, timestamp)
      drain(ctx, brigade, next)
  }

  /**
   * Returns the behavior to use while waiting for events.
   *
   * @param brigade The current brigade.
   * @return The behavior to use while waiting for events.
   */
  private def waiting(
    brigade: Brigade
  ): Behaviors.Immutable[Event.Incoming] = Behaviors.immutable[Event.Incoming] { (ctx, evt) =>
    evt match {
      case configure@Event.Configure(_, _) => drain(ctx, brigade, Buffer() :+ configure)
      case submit@Event.Submit(_, _) => drain(ctx, brigade, Buffer() :+ submit)
      case _ => Behaviors.same
    }
  }

  /**
   * Drains as many events as possible from the buffer.
   *
   * @param ctx     The current actor context.
   * @param brigade The current brigade.
   * @param buffer  The event buffer to use.
   * @return The next behavior to use after draining.
   */
  @annotation.tailrec
  private def drain(
    ctx: ActorContext[Event.Incoming],
    brigade: Brigade,
    buffer: Buffer
  ): Behaviors.Immutable[Event.Incoming] = {
    val (next, evt) = buffer.next
    evt match {
      case Some(event@Event.Configure(_, _)) =>
        applyConfigure(brigade, event.copy(messages = event.messages.filter(_.author == owner))) match {
          case Left((result, replies)) =>
            finishEvent(brigade, result, replies, event.timestamp)
            drain(ctx, result, next)
          case Right((organizers, history)) =>
            outgoing ! Event.LoadHistory(id, history, ctx.self, event.timestamp)
            onHistoryLoaded(brigade, next, organizers)
        }
      case Some(evt@Event.Submit(_, _)) =>
        applySubmit(brigade, evt) match {
          case Left((result, replies)) =>
            finishEvent(brigade, result, replies, evt.timestamp)
            drain(ctx, result, next)
          case Right(commands) =>
            outgoing ! Event.PrepareTeams(id, ctx.self, evt.timestamp)
            onTeamsPrepared(brigade, next, evt, commands)
        }
      case None =>
        waiting(brigade)
    }
  }

  /**
   * Completes the processing of an event by sending effect messages.
   *
   * @param brigade   The previous brigade.
   * @param result    The current brigade.
   * @param replies   The replies that were generated.
   * @param timestamp The instant this event occurred at.
   */
  private def finishEvent(
    brigade: Brigade,
    result: Brigade,
    replies: Vector[Reply],
    timestamp: Long
  ): Unit = {
    if (brigade.session != result.session) outgoing ! Event.SaveSession(id, result.session, timestamp)
    replies collect {
      case Reply.FinalizeTeams(_, teams) => Event.PrependToHistory(id, teams, timestamp)
    } foreach (outgoing ! _)
    if (replies.nonEmpty) outgoing ! Event.PostReplies(id, replies, timestamp)
  }

}

/**
 * Factory for channel behaviors.
 */
object Channel {

  /**
   * Creates the initial behavior of a new channel.
   *
   * @param id       The ID of this channel.
   * @param owner    The owner of this channel.
   * @param outgoing The actor to send all outgoing events to.
   * @return The behavior of a new channel.
   */
  def apply(id: Id, owner: User, outgoing: ActorRef[Event.Outgoing]): Behaviors.Immutable[Event.Incoming] =
    Behaviors.immutable { (ctx, evt) =>
      evt match {
        case Event.Initialize(session, timestamp) =>
          outgoing ! Event.LoadMessages(id, session map (_.lastModified) getOrElse 0, ctx.self, timestamp)
          new Channel(id, owner, outgoing).onMessagesLoaded(session getOrElse Brigade.Inactive(0), Buffer())
        case _ =>
          Behaviors.same
      }
    }

  /**
   * Applies a configure event and returns either the outcome or the state pending a history reload.
   *
   * @param brigade The current brigade.
   * @param event   The event to apply.
   * @return Either the outcome or the state pending a history reload.
   */
  private def applyConfigure(
    brigade: Brigade,
    event: Event.Configure
  ): Either[(Brigade, Vector[Reply]), (Set[User], Int)] = {
    val (organizers, history) = ((Set[User](), None: Option[Int]) /: event.messages) { (previous, message) =>
      val (oldUsers, oldHistory) = previous
      val (newUsers, newHistory) = message.extractConfiguration
      (oldUsers ++ newUsers, newHistory orElse oldHistory)
    }
    history map { d =>
      brigade.configuration match {
        case Configuration.Cycle(h) if h.teams.length >= d => Left(Configuration.Cycle(History(h.teams take d)))
        case Configuration.Default => Right(d)
      }
    } getOrElse Left(Configuration.Default) fold(
      c => Left(brigade.configure(organizers, c, event.timestamp)),
      d => Right(organizers -> d))
  }

  /**
   * Continues applying a configure event after a history reload.
   *
   * @param brigade    The current brigade.
   * @param organizers The organizers for the brigade.
   * @param history    The history to use for the brigade.
   * @param timestamp  The instant that this event occurred.
   * @return The outcome of applying the event.
   */
  private def continueConfigure(
    brigade: Brigade,
    organizers: Set[User],
    history: History,
    timestamp: Long
  ): (Brigade, Vector[Reply]) =
    brigade.configure(organizers, Configuration.Cycle(history), timestamp)

  /**
   * Applies a submit event and returns either the outcome or the state pending a prepared teams message ID.
   *
   * @param brigade The current brigade.
   * @param event   The event to apply.
   * @return Either the outcome or the state pending prepared teams message ID.
   */
  private def applySubmit(
    brigade: Brigade,
    event: Event.Submit
  ): Either[(Brigade, Vector[Reply]), Vector[Command]] = {
    val commands = event.message.extractSubmission
    if (commands.isEmpty) Left(brigade -> Vector()) else brigade.session match {
      case Brigade.Inactive(_)
        if brigade.organizers(event.message.author) &&
          commands.collect { case cmd@Command.Open(_, _) => cmd }.nonEmpty =>
        Right(commands)
      case _ =>
        Left(brigade.submit(event.message.author, event.message.id, commands, event.timestamp))
    }
  }

  /**
   * Continues applying a submit event after a teams message ID is prepared.
   *
   * @param brigade   The current brigade.
   * @param event     The event to apply.
   * @param commands  The commands contained in the event.
   * @param timestamp The instant that this event occurred.
   * @return The outcome of applying the event.
   */
  private def continueSubmit(
    brigade: Brigade,
    event: Event.Submit,
    commands: Vector[Command],
    teamsMsgId: Message.Id,
    timestamp: Long
  ): (Brigade, Vector[Reply]) = {
    brigade.submit(event.message.author, event.message.id, commands map {
      case cmd@Command.Open(_, _) => cmd.copy(teamsMsgId = Some(teamsMsgId))
      case cmd => cmd
    }, timestamp)
  }

  /**
   * The ID of a channel.
   *
   * @param value The underlying channel ID value.
   */
  final class Id private(val value: Long) extends AnyVal {

    /* Convert to a string. */
    override def toString: String = s"Channel.Id($value)"

  }

  /**
   * Factory for channel IDs.
   */
  object Id {

    /**
     * Creates a new channel ID.
     *
     * @param value The underlying channel ID value.
     * @return a new channel ID.
     */
    def apply(value: Long): Id = new Id(value)

  }

  /**
   * A buffer that keeps the most recent configure event and a sorted, distinct sequence of submit events.
   *
   * @param configuration The buffered configure event.
   * @param submissions   The buffered submit events.
   */
  case class Buffer private(configuration: Option[Event.Configure], submissions: Vector[Event.Submit]) {

    /**
     * Modifies the buffered configure signal.
     *
     * @param signal The new configure signal.
     * @return The modified configure signal.
     */
    def :+(signal: Event.Configure): Buffer =
      copy(configuration = configuration filter (Event.EventOrder.gt(_, signal)) orElse Some(signal))

    /**
     * Modifies the buffered submit signals.
     *
     * @param signals The new submit signals.
     * @return The modified submit signals.
     */
    def :+(signals: Event.Submit*): Buffer =
      copy(submissions = (submissions /: signals) ((s, e) => Buffer.insert(s, e, 0, s.length)))

    /**
     * Dequeue the next signal from this buffer.
     *
     * @return The resulting buffer and the next signal if one is available.
     */
    def next: (Buffer, Option[Event.Signal]) = configuration match {
      case configure@Some(_) => copy(configuration = None) -> configure
      case None if submissions.nonEmpty => copy(submissions = submissions.tail) -> Some(submissions.head)
      case None => this -> None
    }

    /**
     * Returns a behavior that buffers events until the partial function is satisfied.
     *
     * @param done The partial function that must be satisfied for buffering to end.
     * @return A behavior that buffers events until the partial function is satisfied.
     */
    def buffering(
      done: PartialFunction[(ActorContext[Event.Incoming], Buffer, Event.Incoming), Behaviors.Immutable[Event.Incoming]]
    ): Behaviors.Immutable[Event.Incoming] =
      Behaviors.immutable[Event.Incoming] { (ctx, evt) =>
        done.applyOrElse((ctx, this, evt), (from: (ActorContext[Event.Incoming], Buffer, Event.Incoming)) =>
          from._3 match {
            case signal@Event.Configure(_, _) => (this :+ signal).buffering(done)
            case signal@Event.Submit(_, _) => (this :+ signal).buffering(done)
            case _ => Behaviors.same
          })
      }

  }

  /**
   * Factory for event buffers.
   */
  object Buffer {

    /**
     * Creates a new buffer.
     *
     * @return A new, empty buffer.
     */
    def apply(): Buffer = Buffer(None, Vector())

    /**
     * Inserts an event into a sorted, distinct vector of events.
     *
     * @param vector The sorted vector to insert into.
     * @param event  The event to insert.
     * @param start  The inclusive index to start searching at.
     * @param end    The exclusive index to stop searching at.
     * @return The new vector with the specified event inserted.
     */
    @annotation.tailrec
    private def insert(
      vector: Vector[Event.Submit],
      event: Event.Submit,
      start: Int,
      end: Int
    ): Vector[Event.Submit] =
      if (start != end) {
        val index = start + (end - start) / 2
        if (Event.EventOrder.gt(vector(index), event)) insert(vector, event, start, index)
        else insert(vector, event, index + 1, end)
      } else if (start > 0 && vector(start - 1) == event) {
        vector
      } else start match {
        case 0 => event +: vector
        case last if last == vector.length => vector :+ event
        case index => (vector.take(index) :+ event) ++ vector.drop(index)
      }

  }

}
