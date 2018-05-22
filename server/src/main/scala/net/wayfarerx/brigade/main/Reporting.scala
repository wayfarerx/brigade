/*
 * Reporting.scala
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
package main

import collection.JavaConverters._

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl._

import software.amazon.awssdk.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest, StandardUnit}

import net.wayfarerx.aws.CloudWatch

/**
 * An actor that handles reporting on the state of the system.
 *
 * @param driver The driver that handles reports.
 */
final class Reporting private(driver: Reporting.Driver) {

  import Reporting._

  /** The behavior of this actor. */
  private def behavior: Behavior[Action] = Behaviors.receive[Action] { (_, act) =>
    driver.report(act)
    Behaviors.same
  }

}

/**
 * Factory for reporting actors.
 */
object Reporting {

  /** Factory for reporting successfully posting replies. */
  lazy val PostedReplies: Action.Success = Action.Success("post", " replies", "discord")

  /** Factory for reporting successfully loading messages. */
  lazy val LoadedMessages: Action.Success = Action.Success("load", " messages", "discord")

  /** Factory for reporting successfully preparing teams. */
  lazy val PreparedTeams: Action.Success = Action.Success("prepare", " teams", "discord")

  /** Factory for reporting failures to post replies. */
  lazy val FailedToPostReplies: Action.Failure = Action.Failure("post", "replies", "discord")

  /** Factory for reporting failures to load messages. */
  lazy val FailedToLoadMessages: Action.Failure = Action.Failure("load", " messages", "discord")

  /** Factory for reporting failures to prepare teams. */
  lazy val FailedToPrepareTeams: Action.Failure = Action.Failure("prepare", " teams", "discord")

  /** Factory for reporting successfully loading sessions. */
  lazy val SessionLoaded: Action.Success = Action.Success("load", " session", "storage")

  /** Factory for reporting successfully skipping sessions. */
  lazy val SessionSkipped: Action.Success = Action.Success("skip", " session", "storage")

  /** Factory for reporting successfully saving sessions. */
  lazy val SessionSaved: Action.Success = Action.Success("save", " session", "storage")

  /** Factory for reporting successfully loading histories. */
  lazy val HistoryLoaded: Action.Success = Action.Success("load", " history", "storage")

  /** Factory for reporting successfully prepending to histories. */
  lazy val HistoryPrepended: Action.Success = Action.Success("prepend", " history", "storage")

  /** Factory for reporting failures to decode sessions. */
  lazy val FailedToDecodeSession: Action.Failure = Action.Failure("decode", "session", "storage")

  /** Factory for reporting failures to parse sessions. */
  lazy val FailedToParseSession: Action.Failure = Action.Failure("parse", "session", "storage")

  /** Factory for reporting failures to load sessions. */
  lazy val FailedToLoadSession: Action.Failure = Action.Failure("load", "session", "storage")

  /** Factory for reporting failures to save sessions. */
  lazy val FailedToSaveSession: Action.Failure = Action.Failure("save", "session", "storage")

  /** Factory for reporting failures to decode teams. */
  lazy val FailedToDecodeTeams: Action.Failure = Action.Failure("decode", "teams", "storage")

  /** Factory for reporting failures to parse teams. */
  lazy val FailedToParseTeams: Action.Failure = Action.Failure("parse", "teams", "storage")

  /** Factory for reporting failures to load histories. */
  lazy val FailedToLoadHistory: Action.Failure = Action.Failure("load", "history", "storage")

  /** Factory for reporting failures to prepend to histories. */
  lazy val FailedToPrependHistory: Action.Failure = Action.Failure("prepend", "history", "storage")

  /**
   * Creates a reporting behavior that uses the system output.
   *
   * @return A reporting behavior that uses the system output.
   */
  def apply(): Behavior[Action] =
    new Reporting(Driver.Default).behavior

  /**
   * Creates a reporting behavior that uses Amazon CloudWatch.
   *
   * @param cloudWatch The Cloud Watch actor to use.
   * @return A reporting behavior that uses Amazon CloudWatch.
   */
  def apply(cloudWatch: ActorRef[CloudWatch.Event]): Behavior[Action] =
    new Reporting(Driver.Cloud(cloudWatch)).behavior

  /**
   * Base class for all reporting actions.
   *
   * @param message    The message to report.
   * @param dimensions The dimensions for the report.
   * @param channelId  The channel that caused the report.
   * @param timestamp  The instant that this action was reported.
   */
  case class Action private(message: String, dimensions: Map[String, String], channelId: Channel.Id, timestamp: Long)

  /**
   * Definitions associated with actions.
   */
  object Action {

    /** A factory for reporting successful actions. */
    type Success = (Channel.Id, Long) => Action

    /** A factory for reporting failed actions. */
    type Failure = (String, Channel.Id, Long) => Action

    /**
     * Creates a factory for reporting successful actions that uses the specified message and tags.
     *
     * @param action The action being performed.
     * @param target The target of the action being performed.
     * @param system The system that the action occurred in.
     * @return An action factory that uses the specified tags.
     */
    def Success(action: String, target: String, system: String): Success =
      Action(s"${action.capitalize} $target.",
        Map("action" -> action, "target" -> target, "system" -> system, "outcome" -> "success"), _, _)

    /**
     * Creates a factory for reporting failed actions that uses the specified tags.
     *
     * @param action The action being performed.
     * @param target The target of the action being performed.
     * @param system The system that the action occurred in.
     * @return An action factory that uses the specified tags.
     */
    def Failure(action: String, target: String, system: String): Failure =
      Action(_, Map("action" -> action, "target" -> target, "system" -> system, "outcome" -> "failure"), _, _)

  }

  /**
   * Base class for reporting drivers.
   */
  sealed trait Driver {

    /**
     * Reports the specified action.
     *
     * @param action The action to report.
     */
    def report(action: Action): Unit

  }

  /**
   * Factory for reporting drivers.
   */
  object Driver {

    /**
     * The driver that uses the system IO.
     */
    case object Default extends Driver {

      /* Report to the appropriate system stream. */
      override def report(action: Action): Unit =
        (if (action.dimensions("outcome") == "success") System.out else System.err).println(
          s"${action.message} ${action.dimensions.mkString("{", ",", "}")} @${action.channelId} !${action.timestamp}"
        )

    }

    /**
     * The driver that uses Amazon CloudWatch.
     *
     * @param cloudWatch The Cloud Watch actor to use.
     */
    case class Cloud(cloudWatch: ActorRef[CloudWatch.Event]) extends Driver {

      /* Report to the appropriate system stream. */
      override def report(a: Action): Unit =
        cloudWatch ! CloudWatch.PutMetricData(
          PutMetricDataRequest.builder()
            .namespace(s"brigade/${a.dimensions("system")}")
            .metricData(
              MetricDatum.builder()
                .metricName(s"${a.dimensions("action")}-${a.dimensions("target")}-${a.dimensions("outcome")}")
                .unit(StandardUnit.COUNT)
                .value(1.0)
                .dimensions((a.dimensions.toVector.map {
                  case (n, v) => Dimension.builder().name(n).value(v).build()
                } :+ Dimension.builder().name("channel").value(a.channelId.value.toString).build()).asJavaCollection
                ).build()
            ).build()
        )

    }

  }

}