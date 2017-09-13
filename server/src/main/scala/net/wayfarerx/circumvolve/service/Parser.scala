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

import java.util.StringTokenizer

import net.wayfarerx.circumvolve.model.{Member, Role}

/**
 * A wrapper around a string tokenizer that supports backtracking.
 *
 * @param buffer The stack of backtracked tokens.
 * @param tokens The sequence of remaining tokens.
 */
final class Parser private(buffer: Vector[String], tokens: StringTokenizer) {

  /**
   * Returns true if there are more tokens available.
   *
   * @return True if there are more tokens available.
   */
  def hasNext: Boolean =
    buffer.nonEmpty || tokens.hasMoreElements

  /**
   * Returns the next parser and the next token.
   *
   * @return The next parser and the next token.
   */
  def next(): (Parser, String) =
    if (buffer.nonEmpty) new Parser(buffer.drop(1), tokens) -> buffer.head
    else new Parser(buffer, tokens) -> tokens.nextToken()

  /**
   * Pushes a token onto the stack of backtracked tokens.
   *
   * @param token The token to push.
   * @return The next parser instance.
   */
  def push(token: String): Parser =
    new Parser(token +: buffer, tokens)

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
   * @param mentions Mentioned members indexed by their various identifiers.
   * @param message  The message text to parse.
   * @return The actions that were parsed from the message.
   */
  def apply(mentions: Map[String, Member], message: String): Vector[Action] = {

    /**
     * Scans all tokens in the parser extracting any actions found.
     *
     * @param result The collection of actions to append to.
     * @param parser The parser to read tokens from.
     * @return The collection of actions that were found.
     */
    @annotation.tailrec
    def scan(result: Vector[Action], parser: Parser): Vector[Action] =
    if (!parser.hasNext) result else {
      val (next, token) = parser.next()
      if (!token.startsWith(Signifier)) scan(result, next)
      else {
        val (last, action) = token.toLowerCase drop 1 match {
          case "event" => readEvent(next)
          case "drop" => readDrop(next)
          case "help" =>
            next -> Some(Action.Help)
          case _ =>
            val (pending, roles) = readRoles(Vector(Role(token drop 1)), next)
            pending -> Some(Action.Volunteer(roles))
        }
        scan(action map (result :+ _) getOrElse result, last)
      }
    }

    /**
     * Attempts to read a single event action from the parser.
     *
     * @param parser The parser to read from.
     * @return The next parser and any action that was found.
     */
    def readEvent(parser: Parser): (Parser, Option[Action]) =
      if (!parser.hasNext) parser -> None else {
        val (next, token) = parser.next()
        token.toLowerCase match {
          case "open" =>
            val (last, slots) = readSlots(Vector(), next)
            if (slots.isEmpty) last -> None
            else last -> Some(Action.Open(slots))
          case "assign" =>
            readAssign(next)
          case "release" =>
            readRelease(next)
          case "offer" =>
            readOffer(next)
          case "kick" =>
            readKick(next)
          case "abort" =>
            next -> Some(Action.Abort)
          case "close" =>
            next -> Some(Action.Close)
          case other =>
            next.push(other) -> None
        }
      }

    /**
     * Attempts to read a single assign action from the parser.
     *
     * @param parser The parser to read from.
     * @return The next parser and any action that was found.
     */
    def readAssign(parser: Parser): (Parser, Option[Action]) = {
      val (next, assignments) = readAssignments(Vector(), parser)
      if (assignments.isEmpty) next -> None
      else next -> Some(Action.Assign(assignments))
    }

    /**
     * Attempts to read a single release action from the parser.
     *
     * @param parser The parser to read from.
     * @return The next parser and any action that was found.
     */
    def readRelease(parser: Parser): (Parser, Option[Action]) = {
      val (next, members) = readMembers(Vector(), parser)
      if (members.isEmpty) next -> None
      else next -> Some(Action.Release(members.toSet))
    }

    /**
     * Attempts to read a single offer action from the parser.
     *
     * @param parser The parser to read from.
     * @return The next parser and any action that was found.
     */
    def readOffer(parser: Parser): (Parser, Option[Action]) = {
      val (next, member) = readMember(parser)
      member match {
        case Some(m) =>
          val (last, roles) = readRoles(Vector(), next)
          if (roles.isEmpty) last -> None
          else last -> Some(Action.Offer(m, roles))
        case None =>
          next -> None
      }
    }

    /**
     * Attempts to read a single drop action from the parser.
     *
     * @param parser The parser to read from.
     * @return The next parser and any action that was found.
     */
    def readKick(parser: Parser): (Parser, Option[Action]) = {
      val (next, member) = readMember(parser)
      member match {
        case Some(m) =>
          val (last, roles) = readRoles(Vector(), next)
          last -> Some(Action.Kick(m, roles))
        case None =>
          next -> None
      }
    }


    /**
     * Attempts to read a single drop action from the parser.
     *
     * @param parser The parser to read from.
     * @return The next parser and any action that was found.
     */
    def readDrop(parser: Parser): (Parser, Option[Action]) = {
      val (next, roles) = readRoles(Vector(), parser)
      next -> Some(Action.Drop(roles))
    }

    /**
     * Reads as many consecutive slots as possible from the parser.
     *
     * @param slots  The collection of slots to append to.
     * @param parser The parser to read from.
     * @return The next parser and the collection of slots that were found.
     */
    @annotation.tailrec
    def readSlots(slots: Vector[(Role, Int)], parser: Parser): (Parser, Vector[(Role, Int)]) = {
      val (next, role) = readRole(parser)
      role match {
        case Some(r) =>
          val (last, count) = readNumber(next)
          count match {
            case Some(c) =>
              readSlots(slots :+ (r -> c), last)
            case None =>
              last -> slots
          }
        case None =>
          next -> slots
      }
    }

    /**
     * Attempts to read a number.
     *
     * @param parser The parser to read from.
     * @return The next parser and the number if one was found.
     */
    def readNumber(parser: Parser): (Parser, Option[Int]) =
      if (!parser.hasNext) parser -> None else {
        val (next, token) = parser.next()
        try next -> Some(token.toInt) catch {
          case _: Exception => next.push(token) -> None
        }
      }

    /**
     * Attempts to read a member.
     *
     * @param parser The parser to read from.
     * @return The next parser and the member if one was found.
     */
    def readMember(parser: Parser): (Parser, Option[Member]) =
      if (!parser.hasNext) parser -> None else {
        val (next, token) = parser.next()
        mentions get token match {
          case Some(member) => next -> Some(member)
          case None => next.push(token) -> None
        }
      }

    /**
     * Reads as many consecutive members as possible from the parser.
     *
     * @param members The collection of members to append to.
     * @param parser  The parser to read from.
     * @return The next parser and the collection of members that were found.
     */
    @annotation.tailrec
    def readMembers(members: Vector[Member], parser: Parser): (Parser, Vector[Member]) = {
      val (next, member) = readMember(parser)
      member match {
        case Some(m) =>
          readMembers(members :+ m, next)
        case None =>
          next -> members
      }
    }

    /**
     * Attempts to read a prefixed role.
     *
     * @param parser The parser to read from.
     * @return The next parser and the role if one was found.
     */
    def readRole(parser: Parser): (Parser, Option[Role]) =
      if (!parser.hasNext) parser -> None else {
        val (next, token) = parser.next()
        if (token startsWith Signifier) next -> Some(Role(token drop 1))
        else next.push(token) -> None
      }

    /**
     * Reads as many consecutive roles as possible from the parser.
     *
     * @param roles  The collection of roles to append to.
     * @param parser The parser to read from.
     * @return The next parser and the collection of roles that were found.
     */
    @annotation.tailrec
    def readRoles(roles: Vector[Role], parser: Parser): (Parser, Vector[Role]) = {
      val (next, member) = readRole(parser)
      member match {
        case Some(r) =>
          readRoles(roles :+ r, next)
        case None =>
          next -> roles
      }
    }

    /**
     * Reads as many consecutive assignments as possible from the parser.
     *
     * @param assignments The collection of assignments to append to.
     * @param parser      The parser to read from.
     * @return The next parser and the collection of assignments that were found.
     */
    @annotation.tailrec
    def readAssignments(assignments: Vector[(Member, Role)], parser: Parser): (Parser, Vector[(Member, Role)]) = {
      val (next, member) = readMember(parser)
      member match {
        case Some(m) =>
          val (last, role) = readRole(next)
          role match {
            case Some(r) =>
              readAssignments(assignments :+ (m -> r), last)
            case None =>
              last -> assignments
          }
        case None =>
          next -> assignments
      }
    }

    scan(Vector(), new Parser(Vector(), new StringTokenizer(message)))
  }

}
