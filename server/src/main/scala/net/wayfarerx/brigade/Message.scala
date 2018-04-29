/*
 * Message.scala
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

import collection.immutable.ListMap

/**
 * Describes a message handled by the system.
 *
 * @param id     The ID of this message.
 * @param author The author of this message.
 * @param tokens The tokens contained in this message.
 */
case class Message(
  id: Message.Id,
  author: User,
  tokens: Vector[Message.Token]
) {

  /**
   * Attempts to extract configuration information from this message.
   *
   * @return Any organizers and history depth that were extracted from this message.
   */
  def extractConfiguration: (Set[User], Option[Int]) =
    Message.Parser.parseConfiguration(author, tokens)

  /**
   * Attempts to extract submission information from this message.
   *
   * @return Any events that were extracted from this message.
   */
  def extractSubmission: Vector[Command] =
    Message.Parser.parseSubmission(author, tokens)

}

/**
 * Definitions of the message tokens.
 */
object Message {

  /** The names of the supported commands. */
  private val Commands = Set(
    "!brigade",
    "!cycle",
    "!open",
    "!abort",
    "!close",
    "!?",
    "!assign",
    "!release",
    "!offer",
    "!kick",
    "!drop",
    "!help"
  )

  /**
   * Creates a message handled by the system.
   *
   * @param id     The ID of the message.
   * @param author The author of the message.
   * @param tokens The tokens contained in the message.
   */
  def apply(id: Message.Id, author: User, tokens: Message.Token*): Message =
    Message(id, author, tokens.toVector)

  /**
   * The ID of a message.
   *
   * @param value The underlying message ID value.
   */
  final class Id private(val value: Long) extends AnyVal {

    /* Convert to a string. */
    override def toString: String = s"Message.Id($value)"

  }

  /**
   * Factory for message IDs.
   */
  object Id {

    /**
     * Creates a new message ID.
     *
     * @param value The underlying message ID value.
     * @return a new message ID.
     */
    def apply(value: Long): Id = new Id(value)

  }

  /**
   * Base type for message tokens.
   */
  sealed trait Token

  /**
   * A token that mentions a user.
   *
   * @param user The user to mention.
   */
  case class Mention(user: User) extends Token

  /**
   * A token that represents a word.
   *
   * @param value The value of this word.
   */
  case class Word(value: String) extends Token

  /**
   * Definition of the message parsing routines.
   */
  private object Parser {

    /**
     * Parses the components of a configuration from a sequence of tokens.
     *
     * @param author The author of the tokens.
     * @param input The tokens to parse.
     * @param prefix The components that have already been parsed.
     * @return The components of the configuration.
     */
    @annotation.tailrec
    def parseConfiguration(
      author: User,
      input: Vector[Token],
      prefix: (Set[User], Option[Int]) = (Set(), None)
    ): (Set[User], Option[Int]) = input.headOption match {

      case Some(Word(cmd)) if cmd.equalsIgnoreCase("!brigade") =>
        val (users, output) = readUsers(input.tail)
        parseConfiguration(author, output, prefix._1 + author ++ users -> prefix._2)

      case Some(Word(cmd)) if cmd.equalsIgnoreCase("!cycle") =>
        readNumber(input.tail) match {
          case Some((number, output)) => parseConfiguration(author, output, prefix._1 -> Some(number))
          case None => parseConfiguration(author, input.tail, prefix._1 -> Some(2))
        }

      case Some(_) =>
        parseConfiguration(author, input.tail, prefix)

      case None =>
        prefix
    }

    /**
     * Parses the components of a submission from a sequence of tokens.
     *
     * @param author The author of the tokens.
     * @param input The tokens to parse.
     * @param prefix The components that have already been parsed.
     * @return The components of the submission.
     */
    @annotation.tailrec
    def parseSubmission(
      author: User,
      input: Vector[Token],
      prefix: Vector[Command] = Vector()
    ): Vector[Command] = input.headOption match {

      case Some(Word(cmd)) if cmd.equalsIgnoreCase("!help") =>
        parseSubmission(author, input.tail, prefix :+ Command.Help)

      case Some(Word(cmd)) if cmd.equalsIgnoreCase("!open") =>
        val (slots, output) = readSlots(input.tail)
        if (slots.isEmpty) parseSubmission(author, output, prefix)
        else parseSubmission(author, output, prefix :+ Command.Open(slots, None))

      case Some(Word(cmd)) if cmd.equalsIgnoreCase("!abort") =>
        parseSubmission(author, input.tail, prefix :+ Command.Abort)

      case Some(Word(cmd)) if cmd.equalsIgnoreCase("!close") =>
        parseSubmission(author, input.tail, prefix :+ Command.Close)

      case Some(Word(cmd)) if cmd.equalsIgnoreCase("!?") =>
        readUser(input.tail) match {
          case Some((user, output)) => parseSubmission(author, output, prefix :+ Command.Query(user))
          case None => parseSubmission(author, input.tail, prefix :+ Command.Query(author))
        }

      case Some(Word(cmd)) if cmd.equalsIgnoreCase("!assign") =>
        val (assignments, output) = readAssignments(input.tail)
        if (assignments.isEmpty) parseSubmission(author, output, prefix)
        else parseSubmission(author, output, prefix ++ assignments.map(a => Command.Assign(a._1, a._2)))

      case Some(Word(cmd)) if cmd.equalsIgnoreCase("!release") =>
        readUser(input.tail) match {
          case Some((user, output)) => parseSubmission(author, output, prefix :+ Command.Release(user))
          case None => parseSubmission(author, input.tail, prefix :+ Command.Query(author))
        }

      case Some(Word(cmd)) if cmd.equalsIgnoreCase("!offer") =>
        readUser(input.tail) match {
          case Some((user, next)) =>
            val (roles, output) = readRoles(next)
            parseSubmission(author, output, prefix ++ roles.map(Command.Volunteer(user, _)))
          case None =>
            parseSubmission(author, input.tail, prefix)
        }

      case Some(Word(cmd)) if cmd.equalsIgnoreCase("!kick") =>
        readUser(input.tail) match {
          case Some((user, next)) =>
            val (roles, output) = readRoles(next)
            if (roles.isEmpty) parseSubmission(author, output, prefix :+ Command.DropAll(user))
            else parseSubmission(author, output, prefix ++ roles.map(Command.Drop(user, _)))
          case None =>
            parseSubmission(author, input.tail, prefix)
        }

      case Some(Word(cmd)) if cmd.equalsIgnoreCase("!drop") =>
        val (roles, output) = readRoles(input.tail)
        if (roles.isEmpty) parseSubmission(author, output, prefix :+ Command.DropAll(author))
        else parseSubmission(author, output, prefix ++ roles.map(Command.Drop(author, _)))

      case Some(_) =>
        readRole(input) match {
          case Some((role, output)) => parseSubmission(author, output, prefix :+ Command.Volunteer(author, role))
          case None => parseSubmission(author, input.tail, prefix)
        }

      case None =>
        prefix
    }

    /**
     * Attempt to read as many consecutive assignments as possible from the input.
     *
     * @param input  The input to read from.
     * @param prefix The previously read assignments.
     * @return All the assignments that were read and the remaining input.
     */
    @annotation.tailrec
    private def readAssignments(
      input: Vector[Token],
      prefix: Vector[(User, Role)] = Vector()
    ): (Vector[(User, Role)], Vector[Token]) =
      readUser(input) match {
        case Some((user, next)) => readRole(next) match {
          case Some((role, output)) => readAssignments(output, prefix :+ (user -> role))
          case None => prefix -> input
        }
        case None => prefix -> input
      }

    /**
     * Attempt to read as many consecutive slots as possible from the input.
     *
     * @param input  The input to read from.
     * @param prefix The previously read slots.
     * @return All the slots that were read and the remaining input.
     */
    @annotation.tailrec
    private def readSlots(
      input: Vector[Token],
      prefix: ListMap[Role, Int] = ListMap()
    ): (ListMap[Role, Int], Vector[Token]) =
      readRole(input) match {
        case Some((role, next)) => readNumber(next) match {
          case Some((number, output)) => readSlots(output, prefix get role map { previous =>
            prefix + (role -> (previous + Math.max(0, number)))
          } getOrElse {
            prefix + (role -> Math.max(0, number))
          })
          case None => readSlots(next, prefix get role map { previous =>
            prefix + (role -> (previous + 1))
          } getOrElse {
            prefix + (role -> 1)
          })
        }
        case None => prefix -> input
      }

    /**
     * Attempt to read as many consecutive users as possible from the input.
     *
     * @param input  The input to read from.
     * @param prefix The previously read users.
     * @return All the users that were read and the remaining input.
     */
    @annotation.tailrec
    private def readUsers(
      input: Vector[Token],
      prefix: Vector[User] = Vector()
    ): (Vector[User], Vector[Token]) =
      readUser(input) match {
        case Some((user, output)) => readUsers(output, prefix :+ user)
        case None => prefix -> input
      }

    /**
     * Attempt to read as many consecutive roles as possible from the input.
     *
     * @param input  The input to read from.
     * @param prefix The previously read roles.
     * @return All the roles that were read and the remaining input.
     */
    @annotation.tailrec
    private def readRoles(
      input: Vector[Token],
      prefix: Vector[Role] = Vector()
    ): (Vector[Role], Vector[Token]) =
      readRole(input) match {
        case Some((role, output)) => readRoles(output, prefix :+ role)
        case None => prefix -> input
      }

    /**
     * Attempt to read a user from the specified input.
     *
     * @param input The input to attempt to read a user from.
     * @return The user that was read and the remaining input if a user was read.
     */
    private def readUser(input: Vector[Token]): Option[(User, Vector[Token])] =
      input.headOption collect { case Mention(user) => user -> input.tail }

    /**
     * Attempt to read a role from the specified input.
     *
     * @param input The input to attempt to read a role from.
     * @return The role that was read and the remaining input if a role was read.
     */
    private def readRole(input: Vector[Token]): Option[(Role, Vector[Token])] =
      input.headOption collect {
        case Word(role) if role.length > 1 && role(0) == '!' && !Commands(role.toLowerCase) =>
          Role(role substring 1) -> input.tail
      }

    /**
     * Attempt to read a number from the specified input.
     *
     * @param input The input to attempt to read a number from.
     * @return The number that was read and the remaining input if a number was read.
     */
    private def readNumber(input: Vector[Token]): Option[(Int, Vector[Token])] =
      input.headOption flatMap {
        case Word(number) => try Some(number.toInt) catch {
          case _: NumberFormatException => None
        }
        case _ => None
      } map (_ -> input.tail)

  }

}
