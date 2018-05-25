/*
 * Program.scala
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

import java.nio.charset.Charset
import java.nio.file.{Path, Paths}

import collection.JavaConverters._
import concurrent.Await
import concurrent.duration._

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl._

import org.apache.commons.io.IOUtils

import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.s3.S3AsyncClient

import sx.blah.discord.api.{ClientBuilder, IDiscordClient}
import sx.blah.discord.api.events.EventDispatcher
import sx.blah.discord.handle.impl.events
import sx.blah.discord.handle.obj.{IChannel, IMessage}

import net.wayfarerx.aws.{CloudWatch, S3}


/**
 * Main entry point for the bot.
 */
object Program extends App {

  /** The pattern used to match AWS S3 storage locations. */
  private val S3Pattern =
    """s3\:\/\/([a-zA-Z0-9_\-]+)\/([a-zA-Z0-9_\-\/]+)""".r

  if (args.length >= 1 || args.exists(a => a.equalsIgnoreCase("-h") || a.equalsIgnoreCase("--help"))) {
    println("Usage: java -jar brigade.jar [<DIRECTORY> | s3://<S3_BUCKET>/<S3_PREFIX>]")
    println("  DIRECTORY The local directory to store data in, defaults to the current directory.")
    println("  S3_BUCKET The S3 bucket to use for storage.")
    println("  S3_PREFIX The S3 key prefix to use for storage.")
    System.exit(-1)
  } else {
    var result = 1
    try {
      val token = sys.env.getOrElse("DISCORD_TOKEN", IOUtils.readLines(System.in, "UTF-8").asScala.mkString("\n")).trim
      val settings = args match {
        case Array(S3Pattern(bucket, prefix)) => Right(bucket, prefix)
        case Array(path) => Left(Paths.get(path))
        case _ => Left(Paths.get("."))
      }
      val client = new ClientBuilder().withToken(token).build()
      val system = ActorSystem(behavior(client, settings), "brigade")
      sys.runtime.addShutdownHook(new Thread() {
        override def run(): Unit = Await.result(system.terminate(), 2.minutes)
      })
      client.login()
      result = 0
    } catch {
      case t: Throwable => t.printStackTrace()
    } finally if (result != 0) System.exit(result)
  }

  /**
   * Creates the behavior of the main actor.
   *
   * @param client   The Discord client to use.
   * @param settings The settings to use while building the system.
   * @return The behavior of the main actor.
   */
  private def behavior(client: IDiscordClient, settings: Either[Path, (String, String)]): Behavior[Action] =
    Behaviors.setup[Action] { ctx =>
      val (reporting, storage) = settings match {
        case Left(path) =>
          val reporting = ctx.spawn(Reporting(), "reporting")
          val storage = ctx.spawn(Storage(path, reporting), "storage")
          reporting -> storage
        case Right((bucket, prefix)) =>
          val cloudWatch = ctx.spawn(CloudWatch(CloudWatchAsyncClient.builder().build()), "cloud-watch")
          val s3 = ctx.spawn(S3(S3AsyncClient.builder().build()), "s3")
          val reporting = ctx.spawn(Reporting(cloudWatch), "reporting")
          val storage = ctx.spawn(Storage(s3, bucket, prefix, reporting), "storage")
          reporting -> storage
      }
      val discord = ctx.spawn(Discord(client, reporting), "discord")
      val router = ctx.spawn(new Routing(storage, discord).behavior, "router")
      new Main(client.getDispatcher, storage, router, ctx.self).behavior(Map()) receiveSignal {
        case (_, PostStop) ⇒
          client.logout()
          Behaviors.same
      }
    }

  /**
   * The main behavior that responds to Discord events.
   *
   * @param dispatcher The Discord dispatcher to use.
   * @param storage    The storage actor to use.
   * @param router     The router actor to use.
   */
  private final class Main(
    dispatcher: EventDispatcher,
    storage: ActorRef[Storage.Action],
    router: ActorRef[Event.Outgoing],
    self: ActorRef[Action]
  ) {

    // Register guild listeners.
    dispatcher.registerListener { event: events.guild.GuildCreateEvent =>
      event.getGuild.getChannels.asScala foreach (self ! Enter(_, System.currentTimeMillis))
    }
    dispatcher.registerListener { event: events.guild.GuildLeaveEvent =>
      event.getGuild.getChannels.asScala foreach (self ! Exit(_))
    }
    // TODO Ownership transfer.

    // Register channel listeners.
    dispatcher.registerListener { event: events.guild.channel.ChannelCreateEvent =>
      if (!event.getChannel.isPrivate) self ! Enter(event.getChannel, System.currentTimeMillis)
    }
    dispatcher.registerListener { event: events.guild.channel.ChannelDeleteEvent =>
      if (!event.getChannel.isPrivate) self ! Exit(event.getChannel)
    }

    // Register message listeners.
    dispatcher.registerListener { event: events.guild.channel.message.MessageReceivedEvent =>
      if (!event.getChannel.isPrivate) self ! Submit(event.getMessage, deleted = false, System.currentTimeMillis)
    }
    dispatcher.registerListener { event: events.guild.channel.message.MessageEditEvent =>
      if (!event.getChannel.isPrivate) self ! Submit(event.getMessage, deleted = false, System.currentTimeMillis)
    }
    dispatcher.registerListener { event: events.guild.channel.message.MessageUpdateEvent =>
      if (!event.getChannel.isPrivate) self ! Submit(event.getMessage, deleted = false, System.currentTimeMillis)
    }
    dispatcher.registerListener { event: events.guild.channel.message.MessageDeleteEvent =>
      if (!event.getChannel.isPrivate) self ! Submit(event.getMessage, deleted = true, System.currentTimeMillis)
    }
    dispatcher.registerListener { event: events.guild.channel.message.MessagePinEvent =>
      if (!event.getChannel.isPrivate) self ! Configure(event.getChannel, System.currentTimeMillis)
    }
    dispatcher.registerListener { event: events.guild.channel.message.MessageUnpinEvent =>
      if (!event.getChannel.isPrivate) self ! Configure(event.getChannel, System.currentTimeMillis)
    }

    // Register the ready listener.
    dispatcher.registerListener { event: events.ReadyEvent =>
      event.getClient.getChannels(false).asScala foreach (self ! Enter(_, System.currentTimeMillis))
    }

    /**
     * The behavior of the main program.
     *
     * @param channels The currently active channels.
     * @return The main application behavior.
     */
    def behavior(channels: Map[Channel.Id, ActorRef[Event.Incoming]]): Behaviors.Receive[Action] =
      Behaviors.receive[Action] { (ctx, act) =>
        act match {
          case Enter(channelId, owner, timestamp) =>
            if (channels contains channelId) Behaviors.same else {
              val channel = ctx.spawn(Channel(channelId, owner, router), s"channel:${channelId.value}")
              storage ! Storage.LoadSession(channelId, channel, timestamp)
              behavior(channels + (channelId -> channel))
            }
          case Configure(channelId, messages, timestamp) =>
            channels get channelId foreach (_ ! Event.Configure(messages, timestamp))
            Behaviors.same
          case Submit(channelId, message, timestamp) =>
            channels get channelId foreach (_ ! Event.Submit(message, timestamp))
            Behaviors.same
          case Exit(channelId) =>
            if (!(channels contains channelId)) Behaviors.same else {
              ctx.stop(channels(channelId))
              behavior(channels - channelId)
            }
        }
      }

  }

  /**
   * The behavior that routes events from channels.
   *
   * @param storage The storage actor to use.
   * @param discord The Discord actor to use.
   */
  private final class Routing(storage: ActorRef[Storage.Action], discord: ActorRef[Discord.Action]) {

    /** Routes outgoing events to the relevant actor. */
    def behavior: Behavior[Event.Outgoing] = Behaviors.receive { (_, evt) =>
      evt match {
        case event: Event.SaveSession => storage ! Storage.SaveSession(event)
        case event: Event.PrependToHistory => storage ! Storage.PrependToHistory(event)
        case event: Event.PostReplies => discord ! Discord.PostReplies(event)
        case event: Event.LoadMessages => discord ! Discord.LoadMessages(event)
        case event: Event.LoadHistory => storage ! Storage.LoadHistory(event)
        case event: Event.PrepareTeams => discord ! Discord.PrepareTeams(event)
      }
      Behaviors.same
    }

  }

  /**
   * Base type for actions received from Discord.
   */
  private sealed trait Action

  /**
   * The action sent when the bot has been added to a channel.
   *
   * @param channelId The ID of the channel the bot was added to.
   * @param owner     The owner of the channel.
   * @param timestamp The instant that this action occurred.
   */
  private case class Enter(channelId: Channel.Id, owner: User, timestamp: Long) extends Action

  /**
   * Factory for enter events.
   */
  private object Enter {

    /**
     * Creates a new enter event.
     *
     * @param channel   The channel being entered.
     * @param timestamp The timestamp of the event.
     * @return A new enter event.
     */
    def apply(channel: IChannel, timestamp: Long): Enter =
      Enter(Channel.Id(channel.getLongID), User(channel.getGuild.getOwnerLongID), timestamp)

  }

  /**
   * The action that notifies the bot that a message was received.
   *
   * @param channelId The ID of the channel.
   * @param messages  The messages that configure the channel.
   * @param timestamp The instant that this action occurred.
   */
  private case class Configure(channelId: Channel.Id, messages: Vector[Message], timestamp: Long) extends Action

  /**
   * Factory for configure events.
   */
  private object Configure {

    /**
     * Creates a new configure event.
     *
     * @param channel   The channel being configured.
     * @param timestamp The timestamp of the event.
     * @return A new configure event.
     */
    def apply(channel: IChannel, timestamp: Long): Configure = Configure(
      Channel.Id(channel.getLongID),
      channel.getPinnedMessages.asScala.toVector map Utils.transformMessage,
      timestamp
    )

  }

  /**
   * The action that notifies the bot that a message was sent, updated or deleted.
   *
   * @param channelId The ID of the channel.
   * @param message   The message to submit to the channel.
   * @param timestamp The instant that this action occurred.
   */
  private case class Submit(channelId: Channel.Id, message: Message, timestamp: Long) extends Action

  /**
   * Factory for submit events.
   */
  private object Submit {

    /**
     * Creates a new submit event.
     *
     * @param message   The message being submitted.
     * @param deleted   True if the message was deleted.
     * @param timestamp The timestamp of the event.
     * @return A new submit event.
     */
    def apply(message: IMessage, deleted: Boolean, timestamp: Long): Submit = {
      val msg =
        if (deleted) Message(Message.Id(message.getLongID), User(message.getAuthor.getLongID))
        else Utils.transformMessage(message)
      Submit(Channel.Id(message.getChannel.getLongID), msg, timestamp)
    }

  }

  /**
   * The action sent when the bot has been removed from a channel.
   *
   * @param channelId The ID of the channel the bot was removed from.
   */
  private case class Exit(channelId: Channel.Id) extends Action

  /**
   * Factory for exit events.
   */
  private object Exit {

    /**
     * Creates a new exit event.
     *
     * @param channel The channel being exited.
     * @return A new exit event.
     */
    def apply(channel: IChannel): Exit =
      Exit(Channel.Id(channel.getLongID))

  }


}
