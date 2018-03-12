package net.wayfarerx.circumvolve

import scala.collection.immutable.ListMap

/**
 * A recording of the events that occur during the sign-up phase of an event.
 */
case class Ledger(entries: Vector[Ledger.Entry]) {

  import Ledger._

  /**
   * Constructs a roster from this ledger.
   *
   * @param slots The mapping of roles to the number of users needed in that role.
   * @return A roster constructed from this ledger.
   */
  def toRoster(slots: ListMap[Role, Int]): Roster = {
    val instructions = entries.zipWithIndex.groupBy(_._1.messageId).values.flatMap { edits =>
      (Vector[(Entry, Int)]() /: edits) { (state, edit) =>
        val (incoming, incomingIndex) = edit
        val incomingSet = incoming.commands.toSet
        val filtered = state map { case (entry, entryIndex) =>
          entry.copy(commands = entry.commands filter incomingSet) -> entryIndex
        }
        val definedSet = filtered.flatMap(_._1.commands).toSet
        filtered :+ (incoming.copy(commands = incoming.commands filterNot definedSet), incomingIndex)
      } filter (_._1.commands.nonEmpty)
    }.toVector.sortBy(_._2).map(_._1)
    (Roster(slots) /: instructions.flatMap(_.commands)) { (roster, command) =>
      command match {
        case Command.Assign(assignments) => roster.copy(assignments = roster.assignments ++ assignments)
        case Command.Release(users) => roster.copy(assignments = roster.assignments filterNot (users contains _._1))
        case Command.Volunteer(user, roles) => roster.copy(volunteers = roster.volunteers ++ roles.map(user -> _))
        case Command.Drop(user, roles) =>
          if (roles.isEmpty) roster.copy(volunteers = roster.volunteers filterNot (_._1 == user))
          else roster.copy(volunteers = roster.volunteers filterNot roles.map(user -> _).toSet)
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
   * @param messageId The ID of the message this entry pertains to.
   * @param commands  The commands that were included in the message.
   */
  case class Entry(messageId: Long, commands: Vector[Command.Mutation])

  /**
   * Factory for ledger entries.
   */
  object Entry {

    /**
     * Creates a new ledger entry.
     *
     * @param messageId The ID of the message this entry pertains to.
     * @param commands  The commands that were included in this message.
     * @return A new ledger entry.
     */
    def apply(messageId: Long, commands: Command.Mutation*): Entry =
      Entry(messageId, commands.toVector)

  }

}