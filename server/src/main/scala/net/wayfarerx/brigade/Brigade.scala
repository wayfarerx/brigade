/*
 * Brigade.scala
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

import scala.collection.immutable.ListMap

/**
 * The state of a single brigade team builder.
 *
 * @param organizers    The organizers of this brigade.
 * @param configuration The configuration for this brigade.
 * @param session       The current attempt to build teams from this brigade.
 */
case class Brigade(
  organizers: Set[User],
  configuration: Configuration,
  session: Brigade.Session
) {

  import Brigade._

  /**
   * Configures this brigade.
   *
   * @param organizers    The users that can administer the brigade.
   * @param configuration The configuration that specifies how teams are built.
   * @param timestamp     The instant that this configuration was created at.
   * @return The configured brigade and any generated replies.
   */
  def configure(
    organizers: Set[User],
    configuration: Configuration,
    timestamp: Long
  ): (Brigade, Vector[Reply]) = session match {
    case s@Inactive(_) =>
      Brigade(organizers, configuration, s.copy(lastModified = timestamp)) ->
        Vector()
    case s@Active(organizer, _, teamsMsgId, slots, _, _) if organizers(organizer) =>
      Brigade(organizers, configuration, s.copy(lastModified = timestamp)) ->
        Vector(Reply.UpdateTeams(teamsMsgId, slots, s.currentTeams(organizers, configuration)))
    case Active(_, _, teamsMsgId, _, _, _) =>
      Brigade(organizers, configuration, Inactive(timestamp)) ->
        Vector(Reply.AbandonTeams(teamsMsgId))
  }

  /**
   * Submits commands to this brigade.
   *
   * @param author    The author of the commands.
   * @param messageId The ID of the message the commands came from.
   * @param commands  The commands to submit.
   * @param timestamp The instant that this submission was created at.
   * @return The resulting brigade and any generated replies.
   */
  def submit(
    author: User,
    messageId: Message.Id,
    commands: Vector[Command],
    timestamp: Long
  ): (Brigade, Vector[Reply]) = {
    val (nextSession, replies) = session(organizers, configuration, author, messageId, commands, timestamp)
    val nextConfiguration = replies.collect {
      case Reply.FinalizeTeams(_, teams) => teams
    }.headOption map { teams =>
      configuration match {
        case Configuration.Cycle(history) => Configuration.Cycle(History(teams +: history.teams.init))
        case Configuration.Default => Configuration.Default
      }
    } getOrElse configuration
    copy(configuration = nextConfiguration, session = nextSession) -> Reply.normalize(replies)
  }

}

/**
 * Definitions associated with brigades.
 */
object Brigade {

  /**
   * Takes all elements of the input until one satisfies the filter.
   *
   * @param input  The input to scan.
   * @param filter The filter to apply to the input.
   * @tparam T The type of input.
   * @tparam E The type of input to find.
   * @return The taken elements and the desired input if found.
   */
  private def scan[T, E <: T](input: Vector[T])(filter: PartialFunction[T, E]): (Vector[T], Option[(E, Vector[T])]) = {
    val lifted = filter.lift

    @annotation.tailrec
    def seek(prefix: Vector[T], suffix: Vector[T]): (Vector[T], Option[(E, Vector[T])]) =
      if (suffix.isEmpty) prefix -> None else lifted(suffix.head) match {
        case Some(result) => prefix -> Some(result, suffix.tail)
        case None => seek(prefix :+ suffix.head, suffix.tail)
      }

    seek(Vector(), input)
  }

  /**
   * Base type for the state of a brigade's session.
   */
  sealed trait Session {

    /** The last time this session was modified. */
    def lastModified: Long

    /**
     * Submits the specified commands to this session and returns a modified session and any replies.
     *
     * @param organizers    The organizers of this brigade.
     * @param configuration The configuration for this brigade.
     * @param author        The author of the commands.
     * @param messageId     The ID of the message the commands came from.
     * @param commands      The commands to submit.
     * @param timestamp     The instant that the supplied commands were generated at.
     * @return A modified session and any replies.
     */
    private[Brigade] def apply(
      organizers: Set[User],
      configuration: Configuration,
      author: User,
      messageId: Message.Id,
      commands: Vector[Command],
      timestamp: Long
    ): (Session, Vector[Reply])

  }

  /**
   * Extractor for sessions.
   */
  object Session {

    /**
     * Extracts the data from a session.
     *
     * @param session The session to extract.
     * @return The data extracted from the session.
     */
    def unapply(session: Session): Option[Long] = Some(session.lastModified)

  }

  /**
   * The state of an inactive brigade session.
   *
   * @param lastModified The last time this session was modified.
   */
  case class Inactive(
    lastModified: Long
  ) extends Session {

    /* Apply the specified commands. */
    override private[Brigade] def apply(
      organizers: Set[User],
      configuration: Configuration,
      author: User,
      messageId: Message.Id,
      commands: Vector[Command],
      timestamp: Long
    ): (Session, Vector[Reply]) = {
      val (prefix, openAndRemainder) = scan(commands) { case cmd@Command.Open(_, Some(_)) if organizers(author) => cmd }
      val (result, replies) = openAndRemainder collect {
        case (Command.Open(slots, Some(teamsMsgId)), remainder) =>
          val (result, replies) = Active(
            author,
            messageId,
            teamsMsgId,
            slots,
            Ledger(),
            Math.max(lastModified, timestamp)
          ).continue(
            organizers,
            configuration,
            author,
            messageId,
            remainder,
            Math.max(lastModified, timestamp)
          )
          result -> (Reply.UpdateTeams(teamsMsgId, slots, Vector(Team(ListMap()))) +: replies)
      } getOrElse Inactive(Math.max(lastModified, timestamp)) -> Vector()
      result -> (if (prefix contains Command.Help) Reply.Usage +: replies else replies)
    }

  }

  /**
   * The state of an active brigade session.
   *
   * @param organizer    The organizer of this brigade.
   * @param openMsgId    The ID of the message that contained the open command.
   * @param teamsMsgId   The ID of the message used to display the brigade state.
   * @param slots        The slots available in a team.
   * @param ledger       The ledger of team members.
   * @param lastModified The last time this session was modified.
   */
  case class Active(
    organizer: User,
    openMsgId: Message.Id,
    teamsMsgId: Message.Id,
    slots: ListMap[Role, Int],
    ledger: Ledger,
    lastModified: Long
  ) extends Session {

    /* Apply the specified commands. */
    override private[Brigade] def apply(
      organizers: Set[User],
      configuration: Configuration,
      author: User,
      messageId: Message.Id,
      commands: Vector[Command],
      timestamp: Long
    ): (Session, Vector[Reply]) =
      if (messageId == openMsgId) {
        val (_, openAndRemainder) = scan(commands) { case cmd@Command.Open(_, Some(_)) if organizers(author) => cmd }
        openAndRemainder collect {
          case (Command.Open(newSlots, Some(newTeamsMsgId)), remainder) =>
            Active(
              author,
              messageId,
              newTeamsMsgId,
              newSlots,
              ledger,
              Math.max(lastModified, timestamp)
            ).continue(
              organizers,
              configuration,
              author,
              messageId,
              remainder,
              Math.max(lastModified, timestamp)
            )
        } getOrElse Inactive(Math.max(lastModified, timestamp)) -> Vector(Reply.AbandonTeams(teamsMsgId))
      } else {
        continue(
          organizers,
          configuration,
          author,
          messageId,
          commands,
          Math.max(lastModified, timestamp)
        )
      }

    /**
     * Continues processing commands after any open commands.
     *
     * @param organizers    The organizers of this brigade.
     * @param configuration The configuration for this brigade.
     * @param author        The author of the commands.
     * @param messageId     The ID of the message the commands came from.
     * @param commands      The commands to submit.
     * @param timestamp     The instant that the supplied commands were generated at.
     * @return A modified session and any replies.
     */
    private[Brigade] def continue(
      organizers: Set[User],
      configuration: Configuration,
      author: User,
      messageId: Message.Id,
      commands: Vector[Command],
      timestamp: Long
    ): (Session, Vector[Reply]) = {
      val (prefix, terminal) = scan(commands) { case cmd@Command.Terminal() if organizers(author) => cmd }
      val (mutations, replies) = ((Vector[Command.Mutation](), Vector[Reply]()) /: prefix) { (previous, command) =>
        val (_mutations, _replies) = previous
        command match {
          // Display usage for help commands.
          case Command.Help =>
            _mutations -> (_replies :+ Reply.Usage)
          // Answer all queries using the current state.
          case Command.Query(user) =>
            val roster = (ledger :+ Ledger.Entry(messageId, author, _mutations: _*)).buildRoster(organizers)
            _mutations -> (_replies :+ Reply.Status(
              user,
              roster.assignments.filter(_._1 == user).map(_._2).distinct,
              roster.volunteers.filter(_._1 == user).sortBy(_._3).map(_._2).distinct
            ))
          // collect all mutations.
          case cmd@Command.Mutation() =>
            (_mutations :+ cmd) -> _replies
          // Ignore all other commands.
          case _ =>
            previous
        }
      }
      terminal match {
        // Abort the brigade.
        case Some((Command.Abort, _)) =>
          Inactive(Math.max(lastModified, timestamp)) -> (replies :+ Reply.AbandonTeams(teamsMsgId))
        case notAborted =>
          val next = if (mutations.isEmpty) copy(lastModified = Math.max(lastModified, timestamp)) else
            copy(ledger = ledger :+ Ledger.Entry(messageId, author, mutations: _*),
              lastModified = Math.max(lastModified, timestamp))
          notAborted match {
            // Finalize the brigade.
            case Some(_) => Inactive(Math.max(lastModified, timestamp)) -> (replies :+
              Reply.FinalizeTeams(teamsMsgId, next.currentTeams(organizers, configuration, finalize = true)))
            // Update the brigade.
            case None => next -> (replies :+
              Reply.UpdateTeams(teamsMsgId, slots, next.currentTeams(organizers, configuration)))
          }
      }
    }

    /**
     * Generates the teams currently configured for this brigade.
     *
     * @param organizers    The organizers of the brigade.
     * @param configuration The configuration of the brigade.
     * @param finalize      If true remove incomplete teams.
     * @return The teams currently configured for this brigade.
     */
    private[Brigade] def currentTeams(
      organizers: Set[User],
      configuration: Configuration,
      finalize: Boolean = false
    ): Vector[Team] = {
      val teams = ledger.buildRoster(organizers)
        .buildTeams(slots, configuration match {
          case Configuration.Default => History()
          case Configuration.Cycle(history) => history
        })
      if (finalize) teams.filter(_.members.values.map(_.size).sum == slots.values.sum) else teams
    }

  }

}