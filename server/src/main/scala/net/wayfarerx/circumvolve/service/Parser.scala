/*
 * Parser.scala
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

import net.wayfarerx.circumvolve.model_old.{User, Role}

/**
 * A wrapper around a string tokenizer that supports backtracking.
 *
 * @param tokens The iterator of pending tokens.
 */
final class Parser private(tokens: Iterator[Token]) {

  /** The next token to return. */
  private var pending: Option[Token] = None

  /**
   * Returns true if there are more tokens available.
   *
   * @return True if there are more tokens available.
   */
  def hasNext: Boolean =
    pending.nonEmpty || tokens.hasNext

  /**
   * Returns the next token.
   *
   * @return The next token.
   */
  def next(): Token =
    pending match {
      case Some(token) =>
        pending = None
        token
      case None =>
        tokens.next()
    }

  /**
   * Returns the next token if it can be transformed.
   *
   * @param f The function to transform the token with.
   * @return The next token if it can be transformed.
   */
  def advance[T](f: Token => Option[T]): Option[T] =
    if (!hasNext) None else {
      val token = next()
      f(token) orElse {
        pending = Some(token)
        None
      }
    }

}

/**
 * Implementation of the strategy that parses commands from messages.
 */
object Parser {

  /** The prefix that identifies commands. */
  val Signifier: String = "!"

  /**
   * Parses a message and its associated meta-data.
   *
   * @param message The message text to parse.
   * @return The actions that were parsed from the message.
   */
  def apply(message: String): Vector[Action] =
    readActions(new Parser(Token.iterate(message)))

  /**
   * Attempts to read a number.
   *
   * @param parser The parser to read from.
   * @return The number if one was found.
   */
  private def readNumber(parser: Parser): Option[Int] =
    parser.advance {
      case Token.Word(content) => try Some(content.toInt) catch {
        case _: Exception => None
      }
      case _ => None
    }

  /**
   * Attempts to read a prefixed role.
   *
   * @param parser The parser to read from.
   * @return The role if one was found.
   */
  private def readRole(parser: Parser): Option[Role] =
    parser.advance {
      case Token.Word(content) if content startsWith Signifier => Some(Role(content drop 1))
      case _ => None
    }

  /**
   * Reads as many consecutive roles as possible from the parser.
   *
   * @param parser The parser to read from.
   * @param roles  The collection of roles to append to.
   * @return The collection of roles that were found.
   */
  @annotation.tailrec
  private def readRoles(parser: Parser, roles: Vector[Role] = Vector()): Vector[Role] =
  readRole(parser) match {
    case Some(role) => readRoles(parser, roles :+ role)
    case None => roles
  }

  /**
   * Reads as many consecutive slots as possible from the parser.
   *
   * @param parser The parser to read from.
   * @param slots  The collection of slots to append to.
   * @return The collection of slots that were found.
   */
  @annotation.tailrec
  private def readSlots(parser: Parser, slots: Vector[(Role, Int)] = Vector()): Vector[(Role, Int)] =
  readRole(parser) match {
    case Some(role) => readNumber(parser) match {
      case Some(count) => readSlots(parser, slots :+ (role -> count))
      case None => slots
    }
    case None => slots
  }

  /**
   * Attempts to read a member.
   *
   * @param parser The parser to read from.
   * @return The member if one was found.
   */
  private def readMember(parser: Parser): Option[User] =
    parser.advance {
      case Token.Mention(id) => Some(User(id.toString))
      case _ => None
    }

  /**
   * Reads as many consecutive members as possible from the parser.
   *
   * @param parser  The parser to read from.
   * @param members The collection of members to append to.
   * @return The collection of members that were found.
   */
  @annotation.tailrec
  private def readMembers(parser: Parser, members: Vector[User] = Vector()): Vector[User] =
  readMember(parser) match {
    case Some(member) => readMembers(parser, members :+ member)
    case None => members
  }

  /**
   * Reads as many consecutive assignments as possible from the parser.
   *
   * @param parser      The parser to read from.
   * @param assignments The collection of assignments to append to.
   * @return The collection of assignments that were found.
   */
  @annotation.tailrec
  private def readAssignments(parser: Parser, assignments: Vector[(User, Role)] = Vector()): Vector[(User, Role)] =
  readMember(parser) match {
    case Some(member) => readRole(parser) match {
      case Some(role) => readAssignments(parser, assignments :+ (member -> role))
      case None => assignments
    }
    case None => assignments
  }

  /**
   * Attempts to read a single open action from the parser.
   *
   * @param parser The parser to read from.
   * @return Any action that was found.
   */
  def readOpen(parser: Parser): Option[Action] = {
    val slots = readSlots(parser)
    if (slots.isEmpty) None
    else Some(Action.Open(slots))
  }

  /**
   * Attempts to read a single assign action from the parser.
   *
   * @param parser The parser to read from.
   * @return Any action that was found.
   */
  def readAssign(parser: Parser): Option[Action] = {
    val assignments = readAssignments(parser)
    if (assignments.isEmpty) None
    else Some(Action.Assign(assignments))
  }

  /**
   * Attempts to read a single release action from the parser.
   *
   * @param parser The parser to read from.
   * @return Any action that was found.
   */
  def readRelease(parser: Parser): Option[Action] = {
    val members = readMembers(parser)
    if (members.isEmpty) None
    else Some(Action.Release(members.toSet))
  }

  /**
   * Attempts to read a single offer action from the parser.
   *
   * @param parser The parser to read from.
   * @return Any action that was found.
   */
  def readOffer(parser: Parser): Option[Action] =
    readMember(parser) flatMap { member =>
      val roles = readRoles(parser)
      if (roles.isEmpty) None
      else Some(Action.Offer(member, roles))
    }

  /**
   * Attempts to read a single drop action from the parser.
   *
   * @param parser The parser to read from.
   * @return Any action that was found.
   */
  def readKick(parser: Parser): Option[Action] =
    readMember(parser) map (Action.Kick(_, readRoles(parser)))


  /**
   * Attempts to read a single drop action from the parser.
   *
   * @param parser The parser to read from.
   * @return Any action that was found.
   */
  def readDrop(parser: Parser): Option[Action] =
    Some(Action.Drop(readRoles(parser)))


  /**
   * Attempts to read a single query action from the parser.
   *
   * @param parser The parser to read from.
   * @return Any action that was found.
   */
  def readQuery(parser: Parser): Option[Action] =
    Some(Action.Query(readMember(parser)))

  /**
   * Attempts to read a single event action from the parser.
   *
   * @param parser The parser to read from.
   * @return Any action that was found.
   */
  def readAction(parser: Parser): Option[Action] =
    parser.advance {
      case Token.Word(content) if content startsWith Signifier => Some(content.drop(1).toLowerCase)
      case _ => None
    } filterNot (_.isEmpty) flatMap {
      case "help" => Some(Action.Help)
      case "open" => readOpen(parser)
      case "abort" => Some(Action.Abort)
      case "close" => Some(Action.Close)
      case "assign" => readAssign(parser)
      case "release" => readRelease(parser)
      case "offer" => readOffer(parser)
      case "kick" => readKick(parser)
      case "drop" => readDrop(parser)
      case "?" => readQuery(parser)
      case role => Some(Action.Volunteer(readRoles(parser, Vector(Role(role)))))
    }

  /**
   * Scans all tokens in the parser extracting any actions found.
   *
   * @param parser  The parser to read tokens from.
   * @param actions The collection of actions to append to.
   * @return The collection of actions that were found.
   */
  @annotation.tailrec
  def readActions(parser: Parser, actions: Vector[Action] = Vector()): Vector[Action] =
  readAction(parser) match {
    case Some(action) =>
      readActions(parser, actions :+ action)
    case None if parser.hasNext =>
      parser.next()
      readActions(parser, actions)
    case None =>
      actions
  }

}
