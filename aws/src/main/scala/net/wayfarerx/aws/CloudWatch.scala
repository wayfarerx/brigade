/*
 * CloudWatch.scala
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

package net.wayfarerx.aws

import concurrent.ExecutionContext
import concurrent.duration._
import util.Try

import akka.actor.Scheduler
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.model.{PutMetricDataRequest, PutMetricDataResponse}

/**
 * An actor that manages interacting with the AWS Cloud Watch service.
 *
 * @param client The client to interact with.
 */
final class CloudWatch private(client: CloudWatchAsyncClient) {

  import CloudWatch._

  /** The behavior of this actor. */
  private def behavior: Behavior[CloudWatch.Event] =
    Behaviors.receive[CloudWatch.Event] { (ctx, evt) =>
      implicit val scheduler: Scheduler = ctx.system.scheduler
      implicit val executionContext: ExecutionContext = ctx.system.executionContext
      evt match {
        case event@PutMetricData(_, _, _) => event(client.putMetricData)
      }
      Behaviors.same
    }

}

/**
 * Factory for AWS Cloud Watch actors.
 */
object CloudWatch {

  /**
   * Creates a new AWS Cloud Watch actor.
   *
   * @param client The client to interact with.
   * @return A new AWS Cloud Watch actor.
   */
  def apply(client: CloudWatchAsyncClient): Behavior[CloudWatch.Event] =
    new CloudWatch(client).behavior

  /**
   * Base type of all Cloud Watch events.
   */
  sealed trait Event

  /**
   * A put metric data event.
   *
   * @param request  The put metric data request.
   * @param respond  The put metric data response handler.
   * @param deadline The deadline that this event must be handled by.
   */
  case class PutMetricData(
    request: PutMetricDataRequest,
    respond: Try[PutMetricDataResponse] => Unit = _ => (),
    deadline: Deadline = 1.minute.fromNow
  ) extends Event with AwsEvent {
    override type Request = PutMetricDataRequest
    override type Response = PutMetricDataResponse
  }

}
