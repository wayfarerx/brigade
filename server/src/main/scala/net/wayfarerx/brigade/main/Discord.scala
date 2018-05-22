/*
 * Discord.scala
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

import java.time.Instant

import collection.JavaConverters._
import concurrent.duration._
import util.{Failure, Success}

import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.actor.typed.scaladsl._

import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.util.{EmbedBuilder, RequestBuffer}

/**
 * An actor that handles sending events to Discord.
 *
 * @param driver    The driver used to interact with the Discord layer.
 * @param reporting The actor to report outcomes to.
 */
final class Discord private(driver: Discord.Driver, reporting: ActorRef[Reporting.Action]) {

  import Discord._

  /** The sharded thread pool that does the actual IO work. */
  private val shards = Shards[Channel.Id](ChannelShardBits, Retry(maximumBackOff = 40.seconds), 2.minutes)

  /** The behavior that handles Discord actions. */
  private def behavior: Behavior[Action] = Behaviors.receive[Action] { (_, act) =>
    act match {

      case PostReplies(Event.PostReplies(channelId, replies, timestamp)) =>
        shards.each(channelId, replies, driver.postReply(channelId, _: Reply)) {
          case Success(_) =>
            reporting ! Reporting.PostedReplies(channelId, timestamp)
          case Failure(thrown) =>
            reporting ! Reporting.FailedToPostReplies(thrown.getMessage, channelId, timestamp)
        }

      case LoadMessages(Event.LoadMessages(channelId, lastModified, respondTo, timestamp)) =>
        shards(channelId, driver.loadMessages(channelId, lastModified)) {
          case Success((configure, submissions)) =>
            reporting ! Reporting.LoadedMessages(channelId, timestamp)
            respondTo ! Event.MessagesLoaded(configure, submissions, timestamp)
          case Failure(thrown) =>
            reporting ! Reporting.FailedToLoadMessages(thrown.getMessage, channelId, timestamp)
            respondTo ! Event.MessagesLoaded(Event.Configure(Vector(), timestamp), Vector(), timestamp)
        }

      case PrepareTeams(Event.PrepareTeams(channelId, respondTo, timestamp)) =>
        shards(channelId, driver.prepareTeams(channelId)) {
          case Success(teamsMsgId) =>
            reporting ! Reporting.PreparedTeams(channelId, timestamp)
            respondTo ! Event.TeamsPrepared(Some(teamsMsgId), timestamp)
          case Failure(thrown) =>
            reporting ! Reporting.FailedToPrepareTeams(thrown.getMessage, channelId, timestamp)
            respondTo ! Event.TeamsPrepared(None, timestamp)
        }

    }
    Behaviors.same
  } receiveSignal {
    case (_, PostStop) ⇒
      shards.dispose()
      Behaviors.same
  }

}

/**
 * Factory for Discord actors.
 */
object Discord {

  /**
   * Creates a Discord behavior using a connection to the server.
   *
   * @param client    The client to communicate with.
   * @param reporting The actor to report outcomes to.
   * @return A Discord behavior using a connection to the server.
   */
  def apply(
    client: IDiscordClient,
    reporting: ActorRef[Reporting.Action]
  ): Behavior[Discord.Action] =
    new Discord(Driver.Connection(client), reporting).behavior

  /**
   * Base class for Discord messages.
   */
  sealed trait Action

  /**
   * Posts replies to Discord.
   *
   * @param event The event that describes this action.
   */
  case class PostReplies(event: Event.PostReplies) extends Action


  /**
   * Loads messages from Discord.
   *
   * @param event The event that describes this action.
   */
  case class LoadMessages(event: Event.LoadMessages) extends Action

  /**
   * Prepares a teams message in Discord.
   *
   * @param event The event that describes this action.
   */
  case class PrepareTeams(event: Event.PrepareTeams) extends Action

  /**
   * Base class for Discord drivers.
   */
  sealed trait Driver {

    /**
     * Posts a reply to a channel.
     *
     * @param channelId The ID of the channel to post replies on.
     * @param reply     The reply to post.
     */
    def postReply(channelId: Channel.Id, reply: Reply): Unit

    /**
     * Loads messages from a channel.
     *
     * @param channelId    The ID of the channel to load messages from.
     * @param lastModified The point in time to start loading messages at.
     * @return Any pinned messages and the messages that were loaded.
     */
    def loadMessages(channelId: Channel.Id, lastModified: Long): (Event.Configure, Vector[Event.Submit])

    /**
     * Prepare a message for posting team updates.
     *
     * @param channelId The ID of the channel to prepare a teams message for.
     * @return The ID of the message that was created.
     */
    def prepareTeams(channelId: Channel.Id): Message.Id

  }

  /**
   * Factory for Discord drivers.
   */
  object Driver {

    /** The message that describes how to use brigade. */
    private def usageMsg =
      """Hello, I'm the Brigade team builder, progeny of the inimitable wayfarerx.
        |I see this channel has been set up to build teams, excellent! Here's the commands you can use:
        |```
        | !open (!ROLE COUNT)+     -- Sets up the team roster.
        | !abort                   -- Abandons the roster and does not build a team.
        | !close                   -- Closes the roster and finalizes a team.
        | !assign (@USER !ROLE)+   -- Assigns members to roles in the team.
        | !release @USER+          -- Releases previously assigned members.
        | !offer @USER !ROLE+      -- Volunteers a member for one or more roles.
        | !kick @USER !ROLE*       -- Kicks a member from one or more roles (or all roles if you don't list any).```
        |Note that you have to be an event administrator to use the above commands. Here's the commands for everyone:
        |```
        | !ROLE+                   -- Volunteers YOU for one or more roles.
        | !drop !ROLE*             -- Drops YOU from one or more roles (or all roles if you don't list any).
        | !? @USER?                -- Lists the roles that @USER has volunteered for or YOUR roles if @USER is omitted.
        | !help                    -- Repeats this message for whenever you feel like you miss me.```
        |For a more detailed tutorial about how this bot works see https://github.com/wayfarerx/circumvolve.
      """.stripMargin

    /**
     * Generates a message that reports the status of a user in the brigade.
     *
     * @param user        The user to generate the status for.
     * @param assigned    The roles the user is assigned to.
     * @param volunteered The roles the user has volunteered for.
     * @return The message that reports the status of a user in the brigade.
     */
    private def statusMsg(user: User, assigned: Vector[Role], volunteered: Vector[Role]) = {

      def render(prefix: String, suffix: Vector[Role]): String = prefix + (suffix match {
        case Vector(v) => v.id
        case v => s"${v.init map (_.id) mkString ", "} & ${v.last.id}"
      })

      def isAssignedTo: String = render("is assigned to ", assigned)

      def isVolunteeredFor: String = render("has volunteered for ", volunteered)

      (assigned, volunteered) match {
        case (Vector(), Vector()) => s"<@!${user.id}> is not a member of this brigade."
        case (_, Vector()) => s"<@!${user.id}> $isAssignedTo."
        case (Vector(), _) => s"<@!${user.id}> $isVolunteeredFor."
        case (_, _) => s"<@!${user.id}> $isAssignedTo and $isVolunteeredFor."
      }
    }

    /** The message used when updating teams. */
    private def teamsChangedMsg(teams: Vector[Team], finalized: Boolean) = {
      val builder = new EmbedBuilder
      builder.withTitle(if (finalized) "Final Teams" else "Current Teams")
      builder.withColor(114, 137, 218)
      teams.zipWithIndex foreach { case (team, index) =>
        val content = team.members flatMap { case (role, users) =>
          users map (u => s"$role: <@!${u.id}>")
        } mkString "\r\n"
        builder.appendField(s"Team ${index + 1}", content, true)
      }
      builder.build()
    }

    /** The message used when abandoning teams. */
    private def abandonTeamsMsg = "This brigade was abandoned."

    /** The message used when preparing teams. */
    private def prepareTeamsMsg = "Preparing brigade..."

    /**
     * The driver that is connected to the Discord servers.
     *
     * @param client The Discord client to use.
     */
    case class Connection(client: IDiscordClient) extends Driver {

      /* Post the description of the reply. */
      override def postReply(channelId: Channel.Id, reply: Reply): Unit = reply match {
        case Reply.Usage =>
          send(channelId, _.sendMessage(usageMsg))
        case Reply.Status(user, assigned, volunteered) =>
          send(channelId, _.sendMessage(statusMsg(user, assigned, volunteered)))
        case Reply.UpdateTeams(teamsMsgId, teams) =>
          send(channelId, _.getMessageByID(teamsMsgId.value).edit(teamsChangedMsg(teams, finalized = false)))
        case Reply.FinalizeTeams(teamsMsgId, teams) =>
          send(channelId, _.getMessageByID(teamsMsgId.value).edit(teamsChangedMsg(teams, finalized = true)))
        case Reply.AbandonTeams(teamsMsgId) =>
          send(channelId, _.getMessageByID(teamsMsgId.value).edit(abandonTeamsMsg))
      }

      /* Load messages from Discord. */
      override def loadMessages(
        channelId: Channel.Id,
        lastModified: Long
      ): (Event.Configure, Vector[Event.Submit]) = {
        val pinned = send(channelId, _.getPinnedMessages).asScala.toVector
        val history = send(channelId,
          _.getMessageHistoryFrom(Instant.ofEpochMilli(math.max(lastModified, -6.hours.fromNow.time.toMillis))))
        Event.Configure(pinned map Utils.transformMessage, pinned.map(_.getTimestamp.toEpochMilli).max) ->
          history.asArray.toVector.map(m => Event.Submit(Utils.transformMessage(m), m.getTimestamp.toEpochMilli))
      }

      /* Send the prepare message and return the message ID. */
      override def prepareTeams(channelId: Channel.Id): Message.Id =
        Message.Id(send(channelId, _.sendMessage(prepareTeamsMsg)).getLongID)

      /**
       * Sends a message to a channel while honoring rate limits.
       *
       * @tparam T The type of result from sending a message.
       * @param channelId The ID of the channel to send to.
       * @param f         The function that sends a message to a channel.
       * @return The result of sending a message.
       */
      private def send[T](channelId: Channel.Id, f: IChannel => T): T =
        RequestBuffer.request { () => f(client.getChannelByID(channelId.value)) }.get()

    }

  }

}
