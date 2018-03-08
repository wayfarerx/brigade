/*
 * Connection.scala
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

import collection.JavaConverters._
import language.implicitConversions

import akka.actor.{Actor, ActorRef, Props}

import net.wayfarerx.circumvolve.model.User

import sx.blah.discord.api.events.IListener
import sx.blah.discord.api.{ClientBuilder, IDiscordClient}
import sx.blah.discord.handle.impl.{events => discord}
import sx.blah.discord.handle.obj.{IChannel, IGuild, IMessage}

/**
 * The actor that manages the connection to the central discord server.
 *
 * @param token   The token to log in with.
 * @param storage The destination to store event information in.
 */
class Connection(token: String, storage: Storage) extends Actor {

  import Connection._

  /** The currently active Discord connection. */
  private var client: Option[IDiscordClient] = None

  /** The actors that power each connected guild's teams. */
  private var guilds = Map[Long, ActorRef]()

  /** The IDs of the event administrators of each channel. */
  private var channels = Map[Long, Set[Long]]()

  /** The IDs of the messages that have been sent by channel. */
  private var messages = Map[Long, Set[Long]]()

  /* Connect to Discord before the actor starts. */
  override def preStart(): Unit = {
    val discordClient = new ClientBuilder().setDaemon(true).withToken(token).login()
    try {
      val dispatcher = discordClient.getDispatcher
      dispatcher.registerListener(OnReady)
      dispatcher.registerListener(OnKickedFrom)
      dispatcher.registerListener(OnRemovedFrom)
      dispatcher.registerListener(OnReceived)
      dispatcher.registerListener(OnUpdated)
      dispatcher.registerListener(OnDeleted)
      dispatcher.registerListener(OnPinned)
      dispatcher.registerListener(OnUnpinned)
      client = Some(discordClient)
    } finally if (client.isEmpty) discordClient.logout()
  }

  /* Disconnect from Discord after the actor stops. */
  override def postStop(): Unit = {
    val c = client
    client = None
    guilds = Map()
    c.foreach(_.logout())
  }

  /* Handle all UI events. */
  override def receive: Actor.Receive = {
    case Ready(discordClient) => onReady(discordClient)
    case KickedFrom(guild) => onKickedFrom(guild)
    case RemovedFrom(channel) => onRemovedFrom(channel)
    case Received(event) => onReceived(event)
    case Updated(event) => onUpdated(event)
    case Deleted(event) => onDeleted(event)
    case Pinned(event) => onPinned(event)
    case Unpinned(event) => onUnpinned(event)
    case status: Status => onStatus(status)
  }

  /**
   * Called when the bot is connected to Discord.
   *
   * @param client The Discord client to use.
   */
  def onReady(client: IDiscordClient): Unit =
    ()

  /**
   * Called when the bot is kicked from a guild.
   *
   * @param guild The guild that the bot was kicked from.
   */
  def onKickedFrom(guild: IGuild): Unit =
    for (actor <- guilds get guild.getLongID) {
      guilds -= guild.getLongID
      context.stop(actor)
    }

  /**
   * Called when the bot is removed from a channel.
   *
   * @param channel The channel that the bot was removed from.
   */
  def onRemovedFrom(channel: IChannel): Unit =
    channels -= channel.getLongID

  /**
   * Called when the bot receives a message.
   *
   * @param event The event that was received.
   */
  def onReceived(event: discord.guild.channel.message.MessageReceivedEvent): Unit = {
    for {
      (guild, administrators) <- guildAndAdministratorsForChannel(event.getChannel)
    } {
      val channelId = event.getChannel.getStringID
      val administrator = administrators(event.getAuthor.getLongID)
      val author = User(event.getAuthor.getStringID)
      Parser(event.getMessage.getContent) foreach {
        case Action.Open(slots) if administrator =>
          guild ! Command.Open(channelId, pendingMessage(event.getMessage.reply(Messages.building)).getStringID, slots)
        case Action.Abort if administrator =>
          guild ! Command.Abort(channelId)
        case Action.Close if administrator =>
          guild ! Command.Close(channelId)
        case Action.Assign(assignments) if administrator =>
          guild ! Command.Assign(channelId, assignments)
        case Action.Release(members) if administrator =>
          guild ! Command.Release(channelId, members)
        case Action.Offer(member, roles) if administrator =>
          guild ! Command.Volunteer(channelId, member, roles)
        case Action.Kick(member, roles) if administrator =>
          guild ! Command.Drop(channelId, member, roles)
        case Action.Volunteer(roles) =>
          guild ! Command.Volunteer(channelId, author, roles)
        case Action.Drop(roles) =>
          guild ! Command.Drop(channelId, author, roles)
        case Action.Query(member) =>
          guild ! Command.Query(channelId, event.getMessage.getStringID, member getOrElse author)
        case Action.Help =>
          event.getMessage.reply(Messages.help)
        case _ =>
          event.getMessage.reply(Messages.unauthorized)
      }
    }
  }

  /**
   * Called when the bot is notified a message has been updated.
   *
   * @param event The event that was received.
   */
  def onUpdated(event: discord.guild.channel.message.MessageUpdateEvent): Unit =
    if (event.getMessage.isPinned && event.getAuthor.getLongID == event.getGuild.getOwnerLongID)
      channels -= event.getChannel.getLongID

  /**
   * Called when the bot is notified a message has been deleted.
   *
   * @param event The event that was received.
   */
  def onDeleted(event: discord.guild.channel.message.MessageDeleteEvent): Unit =
    if (event.getMessage.isPinned && event.getAuthor.getLongID == event.getGuild.getOwnerLongID)
      channels -= event.getChannel.getLongID

  /**
   * Called when the bot is notified a message has been pinned.
   *
   * @param event The event that was received.
   */
  def onPinned(event: discord.guild.channel.message.MessagePinEvent): Unit =
    if (event.getAuthor.getLongID == event.getGuild.getOwnerLongID)
      channels -= event.getChannel.getLongID

  /**
   * Called when the bot is notified a message has been unpinned.
   *
   * @param event The event that was received.
   */
  def onUnpinned(event: discord.guild.channel.message.MessageUnpinEvent): Unit =
    if (event.getAuthor.getLongID == event.getGuild.getOwnerLongID)
      channels -= event.getChannel.getLongID

  /**
   * Called when a guild actor reports its status.
   *
   * @param status The status that was reported.
   */
  private def onStatus(status: Status): Unit = {
    client foreach { discord =>
      val messageId = status.messageId.toLong
      val message = discord.getMessageByID(messageId)
      val author = User(message.getAuthor.getStringID)
      status match {
        case Status.Response(_, _, _, member, roles) =>
          message.reply(Messages.queryResponse(message.getGuild, author, member, roles))
        case rosterStatus: Status.TeamStatus =>
          (rosterStatus match {
            case Status.Opened(_, _, _, roster, team) => Some(roster -> team)
            case Status.Updated(_, _, _, roster, team) => Some(roster -> team)
            case Status.Closed(_, _, _, roster, team) => Some(roster -> team)
            case Status.Aborted(_, _, _) => None
          }) match {
            case Some((roster, team)) =>
              message.edit(Messages.show(message.getGuild, roster, team))
            case None =>
              message.edit(Messages.aborted)
          }
          val channelId = message.getChannel.getLongID
          messages get channelId foreach (_ - messageId foreach (discord.getMessageByID(_).delete()))
          messages -= channelId
      }
    }
  }

  /**
   * Returns the guild and administrators for the specified channel if that channel is enabled.
   *
   * @param channel The channel to find the guild and administrators for.
   * @return The guild and administrators for the specified channel if that channel is enabled.
   */
  private def guildAndAdministratorsForChannel(channel: IChannel): Option[(ActorRef, Set[Long])] = {
    val administrators = channels get channel.getLongID match {
      case Some(admins) => admins
      case None =>
        val admins = channel.getPinnedMessages.asScala.filter { message =>
          message.getAuthor.getLongID == message.getGuild.getOwnerLongID
        }.flatMap(extractAdministrators).toSet
        channels += channel.getLongID -> admins
        admins
    }
    if (administrators.isEmpty) None else guilds get channel.getGuild.getLongID map (_ -> administrators) orElse {
      val guild = context.actorOf(Guild(channel.getGuild.getStringID, storage))
      guilds += channel.getGuild.getLongID -> guild
      Some(guild -> administrators)
    }
  }

  /**
   * Returns the channel administrators if the specified message counts as an enabled message.
   *
   * @param message The message to examine.
   * @return The channel administrators if the specified message counts as an enabled message.
   */
  private def extractAdministrators(message: IMessage): Set[Long] =
    Set[Long](message.getGuild.getOwnerLongID) ++ {
      Token.iterate(message.getContent) dropWhile {
        case Token.Word(content) if content equalsIgnoreCase "!event" => false
        case _ => true
      } collect {
        case Token.Mention(userId) => userId
      }
    }

  /**
   * Marks a message as one to be deleted if it is not the main team message.
   *
   * @param message The message to mark.
   * @return The message that was supplied.
   */
  private def pendingMessage(message: IMessage): IMessage = {
    val channelId = message.getChannel.getLongID
    messages get channelId match {
      case Some(ids) => messages += channelId -> (ids + message.getLongID)
      case None => messages += channelId -> Set(message.getLongID)
    }
    message
  }

  /**
   * The listener that dispatches ready events.
   */
  private object OnReady extends IListener[discord.ReadyEvent] {
    override def handle(e: discord.ReadyEvent): Unit = self ! Ready(e.getClient)
  }

  /**
   * The listener that dispatches guild leave events.
   */
  private object OnKickedFrom extends IListener[discord.guild.GuildLeaveEvent] {
    override def handle(e: discord.guild.GuildLeaveEvent): Unit = self ! KickedFrom(e.getGuild)
  }

  /**
   * The listener that dispatches channel deleted events.
   */
  private object OnRemovedFrom extends IListener[discord.guild.channel.ChannelDeleteEvent] {
    override def handle(e: discord.guild.channel.ChannelDeleteEvent): Unit = self ! RemovedFrom(e.getChannel)
  }

  /**
   * The listener that dispatches message received events.
   */
  private object OnReceived extends IListener[discord.guild.channel.message.MessageReceivedEvent] {
    override def handle(e: discord.guild.channel.message.MessageReceivedEvent): Unit = self ! Received(e)
  }

  /**
   * The listener that dispatches message updated events.
   */
  private object OnUpdated extends IListener[discord.guild.channel.message.MessageUpdateEvent] {
    override def handle(e: discord.guild.channel.message.MessageUpdateEvent): Unit = self ! Updated(e)
  }

  /**
   * The listener that dispatches message deleted events.
   */
  private object OnDeleted extends IListener[discord.guild.channel.message.MessageDeleteEvent] {
    override def handle(e: discord.guild.channel.message.MessageDeleteEvent): Unit = self ! Deleted(e)
  }

  /**
   * The listener that dispatches message pinned events.
   */
  private object OnPinned extends IListener[discord.guild.channel.message.MessagePinEvent] {
    override def handle(e: discord.guild.channel.message.MessagePinEvent): Unit = self ! Pinned(e)
  }

  /**
   * The listener that dispatches message unpinned events.
   */
  private object OnUnpinned extends IListener[discord.guild.channel.message.MessageUnpinEvent] {
    override def handle(e: discord.guild.channel.message.MessageUnpinEvent): Unit = self ! Unpinned(e)
  }

}

/**
 * Companion to the connection actor.
 */
object Connection {

  /** The accepted whitespace characters. */
  val Whitespace: Set[Char] = Set(' ', '\t', '\n', '\r', '\f')

  /**
   * Creates the properties for a connection actor.
   *
   * @param token   The token to log in with.
   * @param storage The destination to store event information in.
   * @return Properties for a connection actor.
   */
  def apply(token: String, storage: Storage): Props =
    Props(new Connection(token, storage))

  /**
   * Base class for Discord events.
   */
  sealed trait DiscordEvent

  /**
   * The initial ready event.
   *
   * @param client The ready Discord client.
   */
  case class Ready(client: IDiscordClient) extends DiscordEvent

  /**
   * Event when the bot has been kicked from a guild.
   *
   * @param guild The Discord guild the bot was kicked from.
   */
  case class KickedFrom(guild: IGuild) extends DiscordEvent

  /**
   * Event when the bot has been removed from a channel.
   *
   * @param channel The Discord channel the bot was removed from.
   */
  case class RemovedFrom(channel: IChannel) extends DiscordEvent

  /**
   * The event that notifies the bot that a message was received.
   *
   * @param event The message event that was received.
   */
  case class Received(event: discord.guild.channel.message.MessageReceivedEvent) extends DiscordEvent

  /**
   * The event that notifies the bot that a message was updated.
   *
   * @param event The message event that was updated.
   */
  case class Updated(event: discord.guild.channel.message.MessageUpdateEvent) extends DiscordEvent

  /**
   * The event that notifies the bot that a message was deleted.
   *
   * @param event The message event that was deleted.
   */
  case class Deleted(event: discord.guild.channel.message.MessageDeleteEvent) extends DiscordEvent

  /**
   * The event that notifies the bot that a message was pinned.
   *
   * @param event The message event that was pinned.
   */
  case class Pinned(event: discord.guild.channel.message.MessagePinEvent) extends DiscordEvent

  /**
   * The event that notifies the bot that a message was unpinned.
   *
   * @param event The message event that was unpinned.
   */
  case class Unpinned(event: discord.guild.channel.message.MessageUnpinEvent) extends DiscordEvent

}
