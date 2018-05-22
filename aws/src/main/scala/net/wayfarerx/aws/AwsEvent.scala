/*
 * AwsEvent.scala
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

import java.util.concurrent.{CompletableFuture, TimeoutException}
import java.util.concurrent.atomic.AtomicBoolean

import concurrent.ExecutionContext
import concurrent.duration._
import util.{Failure, Success, Try}

import akka.actor.Scheduler

/**
 * Base type for events sent to AWS actors.
 */
trait AwsEvent {

  /** The type of request object. */
  type Request

  /** The type of result object. */
  type Response <: AnyRef

  /** The request object. */
  def request: Request

  /** The handler for the result object. */
  def respond: Try[Response] => Unit

  /** The deadline by when this event must be handled. */
  def deadline: Deadline

  /**
   * Applies the specified asynchronous action to this event's request and publishes this event's results.
   *
   * @param action THe action to apply.
   * @param s      The scheduler to use to track the deadline.
   */
  final def apply(action: Request => CompletableFuture[Response])(implicit s: Scheduler, ec: ExecutionContext): Unit =
    deadline.timeLeft match {
      case timeLeft if timeLeft <= Duration.Zero =>
        respond(Failure(new TimeoutException))
      case timeLeft =>
        val latch = new AtomicBoolean
        s.scheduleOnce(timeLeft, new Runnable {
          override def run(): Unit =
            if (latch.compareAndSet(false, true))
              respond(Failure(new TimeoutException))
        })
        action(request).whenComplete((response: Response, thrown: Throwable) =>
          if (latch.compareAndSet(false, true))
            respond(Option(response) map (Success(_)) getOrElse Failure(thrown))
        )
    }

}
