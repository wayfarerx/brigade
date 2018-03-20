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
 * Base type for all brigades.
 */
sealed trait Brigade {

  /** True if this brigade is currently active. */
  def isActive: Boolean

  /**
   * Calculates the outcome of this brigade receiving the specified message.
   *
   * @param message The message to apply to this brigade.
   * @param config  The build configuration.
   * @return The outcome of this brigade receiving the specified message
   */
  final def apply(message: Message, config: Roster.Config = Roster.Fill): Brigade.Outcome = apply(
    message.id,
    message.commands collect { case cmd@Command.Lifecycle() => cmd },
    config)

  /**
   * Calculates the outcome of this brigade receiving the specified message information.
   *
   * @param msgId    The ID of the message to apply to this brigade.
   * @param commands The commands to apply to this brigade.
   * @param config   The build configuration.
   * @return The outcome of this brigade receiving the specified message
   */
  protected def apply(msgId: Message.Id, commands: Vector[Command.Lifecycle], config: Roster.Config): Brigade.Outcome

}

/**
 * Definitions of the brigade types.
 */
object Brigade {

  /**
   * Collects the transaction commands and any trailing terminal command from the specified collection.
   *
   * @param commands The collection of commands to filter.
   * @return The transaction commands and any trailing terminal command.
   */
  private def collectTransactions(
    commands: Vector[Command.Lifecycle]
  ): (Vector[Command.Transaction], Option[Command.Terminal]) = {
    val input = commands collect {
      case cmd@(Command.Transaction() | Command.Terminal()) => cmd
    }
    val transactions = input takeWhile {
      case Command.Transaction() => true
      case _ => false
    } collect {
      case cmd@Command.Transaction() => cmd
    }
    transactions -> input.drop(transactions.size).headOption.collect {
      case cmd@Command.Terminal() => cmd
    }
  }

  /**
   * Runs the specified transactions against a ledger.
   *
   * @param msgId    The ID of the source message.
   * @param commands The transactions to run.
   * @param ledger   The ledger to run against.
   * @return The resulting ledger and any generated replies.
   */
  private def runTransactions(
    msgId: Message.Id,
    commands: Vector[Command.Transaction],
    ledger: Ledger
  ): (Ledger, Vector[Reply]) = {
    val (mutations, queries) = commands partition {
      case Command.Mutation() => true
      case Command.Query(_) => false
    }
    val newLedger = if (mutations.isEmpty) ledger else {
      ledger.copy(entries = ledger.entries :+ Ledger.Entry(msgId, mutations collect {
        case cmd@Command.Mutation() => cmd
      }: _*))
    }
    val roster = newLedger.buildRoster()
    newLedger -> queries.distinct.collect {
      case Command.Query(user) => Reply(
        user,
        roster.assignments.filter(_._1 == user).map(_._2).distinct,
        roster.volunteers.filter(_._1 == user).sortBy(_._3).map(_._2).distinct
      )
    }
  }

  /**
   * Resumes an active brigade.
   *
   * @param msgId    The ID of the incoming message.
   * @param commands The commands to process.
   * @param ledger   The ledger to modify.
   * @return Any terminal command, the modified ledger and any generated replies.
   */
  private def resume(
    openMsgId: Message.Id,
    slots: ListMap[Role, Int],
    msgId: Message.Id,
    commands: Vector[Command.Lifecycle],
    config: Roster.Config,
    ledger: Ledger
  ): Outcome = {
    val (transactions, terminal) = collectTransactions(commands)
    val (newLedger, replies) = runTransactions(msgId, transactions, ledger)
    terminal match {
      case Some(Command.Close) =>
        Outcome(Inactive, newLedger.buildRoster().buildTeams(slots, config), replies)
      case Some(Command.Abort) =>
        Outcome(Inactive, replies = replies)
      case None =>
        Outcome(Active(openMsgId, slots, newLedger), newLedger.buildRoster().buildTeams(slots, config), replies)
    }
  }

  /**
   * The outcome of applying a message to a brigade.
   *
   * @param brigade The resulting brigade.
   * @param teams   The teams that were built.
   * @param replies The replies that were generated.
   */
  case class Outcome(
    brigade: Brigade,
    teams: Vector[Team] = Vector(),
    replies: Vector[Reply] = Vector()
  )

  /**
   * The inactive brigade.
   */
  case object Inactive extends Brigade {

    /* Always inactive. */
    override def isActive: Boolean = false

    /* Try to become active. */
    override def apply(msgId: Message.Id, commands: Vector[Command.Lifecycle], config: Roster.Config): Outcome =
      commands indexWhere {
        case Command.Open(_) => true
        case _ => false
      } match {
        case index if index >= 0 =>
          val slots = commands(index).asInstanceOf[Command.Open].slots
          resume(msgId, slots, msgId, commands drop index + 1, config, Ledger())
        case _ => Outcome(Inactive)
      }

  }

  /**
   * An active brigade.
   *
   * @param openMsgId The ID of the message that opened this brigade.
   * @param slots     The slots specified for this brigade.
   * @param ledger    The ledger that stores the state of this brigade.
   */
  case class Active(openMsgId: Message.Id, slots: ListMap[Role, Int], ledger: Ledger) extends Brigade {

    /* Always inactive. */
    override def isActive: Boolean = true

    /* Try to become active. */
    override def apply(msgId: Message.Id, commands: Vector[Command.Lifecycle], config: Roster.Config): Outcome =
      if (msgId == openMsgId) {
        commands indexWhere {
          case Command.Open(_) => true
          case _ => false
        } match {
          case index if index >= 0 =>
            val slots = commands(index).asInstanceOf[Command.Open].slots
            resume(openMsgId, slots, msgId, commands drop index + 1, config, ledger)
          case _ => Outcome(Inactive)
        }
      } else {
        resume(openMsgId, slots, msgId, commands, config, ledger)
      }

  }

}
