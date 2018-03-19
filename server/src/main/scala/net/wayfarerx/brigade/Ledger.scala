/*
 * Ledger.scala
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

/**
 * A recording of the events that occur during the sign-up phase of an event.
 */
case class Ledger(entries: Vector[Ledger.Entry]) {

  import Ledger._

  /**
   * Constructs a roster from this ledger.
   *
   * @return A roster constructed from this ledger.
   */
  def buildRoster(): Roster = {
    val instructions = entries.zipWithIndex.groupBy(_._1.msgId).values.flatMap { edits =>
      (Vector[(Entry, Int)]() /: edits) { (state, edit) =>
        val (incoming, incomingIndex) = edit
        val incomingCommands = incoming.commands.toMap
        val filtered = state map {
          case (entry, index) => entry.copy(commands = entry.commands flatMap {
            case (cmd, _) => incomingCommands get cmd map (cmd -> _)
          }) -> index
        }
        val definedSet = filtered.flatMap(_._1.commands.map(_._1)).toSet
        filtered :+ (incoming.copy(commands = incoming.commands filterNot (definedSet contains _._1)), incomingIndex)
      } filter (_._1.commands.nonEmpty)
    }.toVector.sortBy(_._2).map(_._1)
    (Roster() /: instructions.flatMap(_.commands)) { (roster, command) =>
      command match {
        case (Command.Assign(user, role), _) =>
          roster.copy(assignments = roster.assignments :+ (user, role))
        case (Command.Release(user), _) =>
          roster.copy(assignments = roster.assignments filterNot (_._1 == user))
        case (Command.Volunteer(user, role), p) =>
          roster.copy(volunteers = roster.volunteers :+ (user, role, p))
        case (Command.Drop(user, role), _) =>
          roster.copy(volunteers = roster.volunteers filterNot (v => v._1 == user && v._2 == role))
        case (Command.DropAll(user), _) =>
          roster.copy(volunteers = roster.volunteers filterNot (_._1 == user))
      }
    }
  }

}

/**
 * Defines the ledger entry type.
 */
object Ledger {


  /**
   * Creates a ledger with the specified entries.
   *
   * @param entries The entries in the ledger.
   * @return A ledger with the specified entries.
   */
  def apply(entries: Entry*): Ledger =
    Ledger(entries.toVector)

  /**
   * An entry in the ledger corresponding to a message post, edit or delete event.
   *
   * @param msgId The ID of the message this entry pertains to.
   * @param commands  The commands that were included in the message and their assigned preference.
   */
  case class Entry(msgId: Message.Id, commands: Vector[(Command.Mutation, Int)])

  /**
   * Factory for ledger entries.
   */
  object Entry {

    /**
     * Creates a new ledger entry.
     *
     * @param msgId The ID of the message this entry pertains to.
     * @param commands  The commands that were included in this message and their assigned preference.
     * @return A new ledger entry.
     */
    def apply(msgId: Message.Id, commands: Command.Mutation*): Entry = {
      var preference = 0
      val volunteers = collection.mutable.HashSet[Command.Volunteer]()
      Entry(msgId, commands.toVector map {
        case cmd@Command.Volunteer(_, _) if volunteers.add(cmd) =>
          val result = cmd -> preference
          preference += 1
          result
        case cmd =>
          cmd -> 0
      })
    }

  }

}
