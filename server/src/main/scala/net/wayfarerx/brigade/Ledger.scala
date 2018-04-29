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
 * A recording of the mutations that occur during the sign-up phase of building a brigade.
 */
case class Ledger(entries: Vector[Ledger.Entry]) {

  import Ledger._

  /**
   * Appends an entry to this ledger.
   *
   * @param entry The ledger to append to.
   * @return The new ledger.
   */
  def :+ (entry: Entry): Ledger = copy(entries :+ entry)

  /**
   * Constructs a roster from this ledger.
   *
   * @param organizers The users that are authorized to organize the ledger.
   * @return A roster constructed from this ledger.
   */
  def buildRoster(organizers: Set[User]): Roster = {
    val instructions = entries.zipWithIndex.groupBy(_._1.msgId).values.flatMap { edits =>
      (Vector[(Entry, Int)]() /: edits) { (state, edit) =>
        val (incoming, incomingIndex) = edit
        val incomingCommands = incoming.mutations.toMap
        val filtered = state map {
          case (entry, index) => entry.copy(mutations = entry.mutations flatMap {
            case (cmd, _) => incomingCommands get cmd map (cmd -> _)
          }) -> index
        }
        val definedSet = filtered.flatMap(_._1.mutations.map(_._1)).toSet
        filtered :+ (incoming.copy(mutations = incoming.mutations filterNot (definedSet contains _._1)), incomingIndex)
      } filter (_._1.mutations.nonEmpty)
    }.toVector.sortBy(_._2).map(_._1)
    val roster = (Roster() /: instructions.flatMap(m => m.mutations map (c => (m.author, c._1, c._2)))) {
      (roster, command) =>
        command match {
          case (author, Command.Assign(user, role), _) =>
            if (!organizers(author)) roster else
              roster.copy(assignments = roster.assignments :+ (user, role))
          case (author, Command.Release(user), _) =>
            if (!organizers(author)) roster else
              roster.copy(assignments = roster.assignments filterNot (_._1 == user))
          case (author, Command.Volunteer(user, role), preference) =>
            if (author != user && !organizers(author)) roster else
              roster.copy(volunteers = roster.volunteers :+ (user, role, preference))
          case (author, Command.Drop(user, role), _) =>
            if (author != user && !organizers(author)) roster else
              roster.copy(volunteers = roster.volunteers filterNot (v => v._1 == user && v._2 == role))
          case (author, Command.DropAll(user), _) =>
            if (author != user && !organizers(author)) roster else
              roster.copy(volunteers = roster.volunteers filterNot (_._1 == user))
        }
    }
    val normalizedPreferences = roster.volunteers.groupBy(_._1).mapValues { entries =>
      entries.sortBy(_._3).map(_._2).zipWithIndex.toMap
    }
    roster.copy(volunteers = roster.volunteers map {
      case (user, role, _) => (user, role, normalizedPreferences(user)(role))
    })
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
   * @param msgId     The ID of the message this entry pertains to.
   * @param author    The author of this entry.
   * @param mutations The events that were included in the message and their assigned preference.
   */
  case class Entry(msgId: Message.Id, author: User, mutations: Vector[(Command.Mutation, Int)])

  /**
   * Factory for ledger entries.
   */
  object Entry {

    /**
     * Creates a new ledger entry.
     *
     * @param msgId     The ID of the message this entry pertains to.
     * @param author    The author of the entry.
     * @param mutations The events that were included in this message and their assigned preference.
     * @return A new ledger entry.
     */
    def apply(msgId: Message.Id, author: User, mutations: Command.Mutation*): Entry = {
      var preference = 0
      val volunteers = collection.mutable.HashSet[Command.Volunteer]()
      Entry(msgId, author, mutations.toVector map {
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
