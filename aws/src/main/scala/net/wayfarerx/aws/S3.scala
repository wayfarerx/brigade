/*
 * S3.scala
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

import concurrent.duration._
import concurrent.ExecutionContext
import util.Try

import akka.actor.Scheduler
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

import software.amazon.awssdk.core.async.AsyncResponseHandler
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model._

/**
 * An actor that manages interacting with the AWS S3 service.
 *
 * @param client The client to interact with.
 */
final class S3 private(client: S3AsyncClient) {

  import S3._

  /** The behavior of this actor. */
  private def behavior: Behavior[S3.Event] =
    Behaviors.receive[S3.Event] { (ctx, evt) =>
      implicit val scheduler: Scheduler = ctx.system.scheduler
      implicit val executionContext: ExecutionContext = ctx.system.executionContext
      evt match {
        case event: ListObjects =>
          event(client.listObjects)
        case event: GetObject =>
          event(client.getObject(_, new InjectResponseAsyncResponseHandlerWrapper[GetObjectResponse, Array[Byte]](
            AsyncResponseHandler.toByteArray[GetObjectResponse])))
        case event: PutObject =>
          event(r => client.putObject(r._1, new SingleByteArrayAsyncRequestProvider(r._2)))
      }
      Behaviors.same
    }

}

/**
 * Factory for AWS S3 actors.
 */
object S3 {

  /**
   * Creates a new AWS Cloud Watch actor.
   *
   * @param client The client to interact with.
   * @return A new AWS Cloud Watch actor.
   */
  def apply(client: S3AsyncClient): Behavior[S3.Event] =
    new S3(client).behavior

  /**
   * Base type of all S3 events.
   */
  sealed trait Event

  /**
   * A lists object event.
   *
   * @param request  The list objects request.
   * @param respond  The list objects response handler (defaults to no effect).
   * @param backoff  The initial amount of time to wait between retries (defaults to 1 second).
   * @param deadline The deadline that this event must be handled by (defaults to 15 seconds from now).
   */
  case class ListObjects(
    request: ListObjectsRequest,
    respond: Try[ListObjectsResponse] => Unit = _ => (),
    backoff: FiniteDuration = 1.second,
    deadline: Deadline = 15.seconds.fromNow
  ) extends Event with AwsEvent {
    override type Request = ListObjectsRequest
    override type Response = ListObjectsResponse
  }

  /**
   * A get object event.
   *
   * @param request  The get object request.
   * @param respond  The get object response handler (defaults to no effect).
   * @param backoff  The initial amount of time to wait between retries (defaults to 1 second).
   * @param deadline The deadline that this event must be handled by (defaults to 15 seconds from now).
   */
  case class GetObject(
    request: GetObjectRequest,
    respond: Try[(GetObjectResponse, Array[Byte])] => Unit = _ => (),
    backoff: FiniteDuration = 1.second,
    deadline: Deadline = 15.seconds.fromNow
  ) extends Event with AwsEvent {
    override type Request = GetObjectRequest
    override type Response = (GetObjectResponse, Array[Byte])
  }

  /**
   * A put object event.
   *
   * @param request  The put object request.
   * @param respond  The put object response handler (defaults to no effect).
   * @param backoff  The initial amount of time to wait between retries (defaults to 1 second).
   * @param deadline The deadline that this event must be handled by (defaults to 15 seconds from now).
   */
  case class PutObject(
    request: (PutObjectRequest, Array[Byte]),
    respond: Try[PutObjectResponse] => Unit = _ => (),
    backoff: FiniteDuration = 1.second,
    deadline: Deadline = 15.seconds.fromNow
  ) extends Event with AwsEvent {
    override type Request = (PutObjectRequest, Array[Byte])
    override type Response = PutObjectResponse
  }

}
