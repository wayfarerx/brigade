package net.wayfarerx.brigade

import scala.collection.immutable.ListMap

/**
 * The state of a single brigade team builder.
 *
 * @param lastModified The time stamp for when this brigade was last modified.
 * @param organizers The organizers of this brigade.
 * @param session The current team building session.
 */
case class Brigade(
  lastModified: Long,
  organizers: Set[User],
  session: Option[Brigade.Session]
) {

  def updateDirectives(messages: Vector[Message]): Brigade = {


    ???
  }

  def handleMessage(message: Message): Brigade = {


    ???
  }

}

object Brigade {

  /**
   * The state of an active brigade session.
   *
   * @param openMsgId
   * @param teamsMsgId
   * @param slots
   * @param history The history to use when scoring candidates.
   * @param ledger
   */
  case class Session(
    organizer: User,
    openMsgId: Message.Id,
    teamsMsgId: Message.Id,
    slots: ListMap[Role, Int],
    history: History,
    ledger: Ledger
  ) {

    def :+ (msgId: Message.Id, author: User, mutations: Command.Mutation*): Session =
      if (mutations.isEmpty) this else copy(ledger = ledger.copy(
        entries = ledger.entries :+ Ledger.Entry(msgId, author, mutations: _*)
      ))

  }

}