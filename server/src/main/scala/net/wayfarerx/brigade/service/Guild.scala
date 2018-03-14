/*
 * Guild.scala
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

package net.wayfarerx.brigade.service

import akka.actor.{Actor, Props}
import akka.event.Logging

import net.wayfarerx.brigade.model_old.{Event, History, User, Role}

/**
 * An actor that manages the state of one guild's events.
 *
 * @param guildId The ID of the guild being managed.
 * @param storage The destination to store event data in.
 */
final class Guild(guildId: String, storage: Storage) extends Actor {

  import Guild._

  /** Logging support. */
  private val log = Logging(context.system, this)

  /** The events currently loaded for this guild. */
  private var events = Map[String, Event]()

  /* Process a command from the connection. */
  override def receive: Actor.Receive = {
    case Command.Open(channelId, eventId, slots) => onOpen(channelId, eventId, slots)
    case Command.Abort(channelId) => onAbort(channelId)
    case Command.Close(channelId) => onClose(channelId)
    case Command.Assign(channelId, assignments) => onAssign(channelId, assignments)
    case Command.Release(channelId, members) => onRelease(channelId, members)
    case Command.Volunteer(channelId, member, roles) => onVolunteer(channelId, member, roles)
    case Command.Drop(channelId, member, roles) => onDrop(channelId, member, roles)
    case Command.Query(channelId, messageId, member) => onQuery(channelId, messageId, member)
  }

  /**
   * Open a new event or update the existing one.
   *
   * @param channelId The channel to open the event for.
   * @param eventId   The ID of the event to open.
   * @param slots     The slots to initialize the roster with.
   */
  private def onOpen(channelId: String, eventId: String, slots: Vector[(Role, Int)]): Unit = {
    log.debug(s"Opening event for $guildId/$channelId.")
    val (event, roster) = loadEvent(channelId).open(eventId, slots)
    storage.putRoster(guildId, channelId, roster)
    events += channelId -> event
    sender ! Status.Opened(guildId, channelId, roster.eventId, roster, event.close()._2.get)
  }

  /**
   * Aborts any in-progress event.
   *
   * @param channelId The channel to abort the event on.
   */
  private def onAbort(channelId: String): Unit = {
    log.debug(s"Aborting event for $guildId/$channelId.")
    val (event, roster) = loadEvent(channelId).abort()
    storage.deleteRoster(guildId, channelId)
    events += channelId -> event
    roster foreach (r => sender ! Status.Aborted(guildId, channelId, r.eventId))
  }

  /**
   * Closes any in-progress event.
   *
   * @param channelId The channel to close the event on.
   */
  private def onClose(channelId: String): Unit = {
    log.debug(s"Closing event for $guildId/$channelId.")
    val old = loadEvent(channelId)
    val (tmp, team) = old.close()
    val event = tmp.copy(history = History(tmp.history.teams.take(MaxHistory)))
    val teamId = String.valueOf(Long.MaxValue - System.currentTimeMillis())
    team foreach (storage.putTeam(guildId, channelId, "0" * (20 - teamId.length) + teamId, _))
    storage.deleteRoster(guildId, channelId)
    events += channelId -> event
    for {
      r <- old.roster
      t <- team
    } sender ! Status.Closed(guildId, channelId, r.eventId, r, t)
  }

  /**
   * Assigns members to roles in an event.
   *
   * @param channelId   The channel of the event to assign to.
   * @param assignments The assignments to make.
   */
  private def onAssign(channelId: String, assignments: Vector[(User, Role)]): Unit = {
    log.debug(s"Assigning ${assignments.size} member(s) to event for $guildId/$channelId.")
    val event = loadEvent(channelId).assign(assignments)
    event.roster foreach (storage.putRoster(guildId, channelId, _))
    events += channelId -> event
    for {
      r <- event.roster
      t <- event.close()._2
    } sender ! Status.Updated(guildId, channelId, r.eventId, r, t)
  }

  /**
   * Releases assigned members from an event.
   *
   * @param channelId The channel of the event to release from.
   * @param members   The members to release.
   */
  private def onRelease(channelId: String, members: Set[User]): Unit = {
    log.debug(s"Releasing ${members.size} member(s) from event for $guildId/$channelId.")
    val event = loadEvent(channelId).release(members)
    event.roster foreach (storage.putRoster(guildId, channelId, _))
    events += channelId -> event
    for {
      r <- event.roster
      t <- event.close()._2
    } sender ! Status.Updated(guildId, channelId, r.eventId, r, t)
  }

  /**
   * Volunteers a member for one or more roles.
   *
   * @param channelId The channel of the event to volunteer for.
   * @param member    The member volunteering.
   * @param roles     The roles being volunteered for.
   */
  private def onVolunteer(channelId: String, member: User, roles: Vector[Role]): Unit = {
    log.debug(s"Volunteering $member to event for $guildId/$channelId.")
    val event = loadEvent(channelId).volunteer(member, roles)
    event.roster foreach (storage.putRoster(guildId, channelId, _))
    events += channelId -> event
    for {
      r <- event.roster
      t <- event.close()._2
    } sender ! Status.Updated(guildId, channelId, r.eventId, r, t)
  }

  /**
   * Drops a member from one or more roles.
   *
   * @param channelId    The channel of the event to drop from.
   * @param member       The member dropping.
   * @param limitToRoles The only roles to drop or empty to drop all roles.
   */
  private def onDrop(channelId: String, member: User, limitToRoles: Vector[Role]): Unit = {
    log.debug(s"Dropping $member from event for $guildId/$channelId.")
    val event = loadEvent(channelId).drop(member, limitToRoles)
    event.roster foreach (storage.putRoster(guildId, channelId, _))
    events += channelId -> event
    for {
      r <- event.roster
      t <- event.close()._2
    } sender ! Status.Updated(guildId, channelId, r.eventId, r, t)
  }

  /**
   * Queried the roles that a member has volunteered for.
   *
   * @param channelId The channel of the event to query.
   * @param messageId The ID of the message that contained the query.
   * @param member    The member being queried.
   */
  private def onQuery(channelId: String, messageId: String, member: User): Unit = {
    log.debug(s"Dropping $member from event for $guildId/$channelId.")
    val roles = loadEvent(channelId).roster map (_.volunteers.filter(_._1 == member).map(_._2).distinct)
    sender ! Status.Response(guildId, channelId, messageId, member, roles getOrElse Vector())
  }

  /**
   * Loads an event from the cache or the storage.
   *
   * @param channelId The channel to load the event for.
   * @return The event associated with the specified channel.
   */
  private def loadEvent(channelId: String): Event =
    events.getOrElse(channelId, {
      val event = Event(storage.getRoster(guildId, channelId), History(
        storage.listTeamIds(guildId, channelId, MaxHistory).flatMap(storage.getTeam(guildId, channelId, _))))
      events += channelId -> event
      event
    })

}

/**
 * Companion for guild actors.
 */
object Guild {

  /** The maximum number of historic teams to keep in memory. */
  val MaxHistory: Int = 4

  /**
   * Creates the properties for a guild actor.
   *
   * @param guildId The ID of the guild being managed.
   * @param storage The destination to store event data in.
   * @return The properties for a guild actor.
   */
  def apply(guildId: String, storage: Storage): Props =
    Props(new Guild(guildId, storage))

}
