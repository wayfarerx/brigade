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

import scala.collection.immutable.ListMap

/**
 * Describes a message handled by the system.
 *
 * @param author The author of this message.
 * @param tokens The tokens contained in the message.
 */
case class Message(author: User, tokens: Vector[Message.Token]) {

  import Message._

  /** This message parsed into a sequence of commands. */
  lazy val commands: Vector[Command] = {

    val input = tokens.iterator.buffered

    /* Attempt to read a single user from the input. */
    def readUser(): Option[User] = input.headOption collect {
      case Mention(user) =>
        input.next()
        user
    }

    /* Attempt to read a single role from the input. */
    def readRole(): Option[Role] = input.headOption collect {
      case Word(role) if role.startsWith("!") && role.length > 1 =>
        input.next()
        Role(role substring 1)
    }

    /* Attempt to read a single number from the input. */
    def readNumber(): Option[Int] = input.headOption flatMap {
      case Word(number) => try Some(number.toInt) catch {
        case _: NumberFormatException => None
      }
      case _ => None
    } map { number =>
      input.next()
      number
    }

    /* Attempt to read as many consecutive users as possible from the input. */
    @annotation.tailrec
    def readUsers(prefix: Vector[User] = Vector()): Vector[User] = readUser() match {
      case Some(user) => readUsers(prefix :+ user)
      case _ => prefix
    }

    /* Attempt to read as many consecutive roles as possible from the input. */
    @annotation.tailrec
    def readRoles(prefix: Vector[Role] = Vector()): Vector[Role] = readRole() match {
      case Some(role) => readRoles(prefix :+ role)
      case _ => prefix
    }

    /* Attempt to read as many consecutive slots as possible from the input. */
    @annotation.tailrec
    def readSlots(prefix: ListMap[Role, Int] = ListMap()): ListMap[Role, Int] = readRole() match {
      case Some(role) => readSlots(prefix + (role -> (readNumber() map (Math.max(0, _)) getOrElse 1)))
      case None => prefix
    }

    /* Attempt to read as many consecutive slots as possible from the input. */
    @annotation.tailrec
    def readAssignments(prefix: Vector[(User, Role)] = Vector()): Vector[(User, Role)] =
      readUser() flatMap (u => readRole() map (u -> _)) match {
        case Some(assignment) => readAssignments(prefix :+ assignment)
        case None => prefix
      }

    /* Scan the entirety of this message and extracts all commands. */
    @annotation.tailrec
    def scan(prefix: Vector[Command]): Vector[Command] = if (!input.hasNext) prefix else input.next() match {

      case Word(tag) if tag.equalsIgnoreCase("!event") =>
        scan(prefix :+ Command.Event(readUsers().toSet))

      case Word(tag) if tag.equalsIgnoreCase("!open") =>
        val slots = readSlots()
        scan(if (slots.isEmpty) prefix else prefix :+ Command.Open(slots))

      case Word(tag) if tag.equalsIgnoreCase("!abort") =>
        scan(prefix :+ Command.Abort)

      case Word(tag) if tag.equalsIgnoreCase("!close") =>
        scan(prefix :+ Command.Close)

      case Word(tag) if tag.equalsIgnoreCase("!help") =>
        scan(prefix :+ Command.Help)

      case Word(tag) if tag.equalsIgnoreCase("!?") =>
        scan(prefix :+ Command.Query(readUser() getOrElse author))

      case Word(tag) if tag.equalsIgnoreCase("!assign") =>
        scan(prefix ++ readAssignments().map(Command.Assign.tupled))

      case Word(tag) if tag.equalsIgnoreCase("!release") =>
        scan(prefix ++ readUsers().map(Command.Release))

      case Word(tag) if tag.equalsIgnoreCase("!offer") =>
        scan(prefix ++ readUser().toVector.flatMap(u => readRoles() map (Command.Volunteer(u, _))))

      case Word(tag) if tag.equalsIgnoreCase("!kick") =>
        scan(prefix ++ readUser().toVector.flatMap { user =>
          readRoles() match {
            case roles if roles.isEmpty => Vector(Command.DropAll(user))
            case roles => roles map (Command.Drop(user, _))
          }
        })

      case Word(tag) if tag.equalsIgnoreCase("!drop") => scan(prefix ++ (readRoles() match {
        case roles if roles.isEmpty => Vector(Command.DropAll(author))
        case roles => roles map (Command.Drop(author, _))
      }))

      case Word(role) if role.startsWith("!") && role.length > 1 =>
        scan(prefix :+ Command.Volunteer(author, Role(role substring 1)))

      case _ =>
        scan(prefix)

    }

    scan(Vector())
  }

}

/**
 * Definitions of the message tokens.
 */
object Message {

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

}
