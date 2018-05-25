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

  /** The backoff to use when retrying. */
  def backoff: FiniteDuration

  /** The deadline by when this event must be handled. */
  def deadline: Deadline

  /**
   * Applies the specified asynchronous action to this event's request and publishes this event's results.
   *
   * @param f  The function to apply.
   * @param s  The scheduler to use to schedule code to run in the future.
   * @param ec The execution context to use for executing code.
   */
  final def apply(f: Request => CompletableFuture[Response])(implicit s: Scheduler, ec: ExecutionContext): Unit =
    AwsEvent.Attempt(() => f(request), respond, backoff, deadline, None)()

}

/**
 * Definitions associated with AWS events.
 */
object AwsEvent {

  /**
   * An attempt at performing an AWS operation.
   *
   * @tparam Response The type of response.
   * @param f          The operation to perform.
   * @param respond    The function to respond to.
   * @param backoff    The backoff to use when retying failed operations.
   * @param deadline   The deadline for responding.
   * @param lastThrown The failure from the most recent attempt.
   */
  private case class Attempt[Response <: AnyRef](
    f: () => CompletableFuture[Response],
    respond: Try[Response] => Unit,
    backoff: FiniteDuration,
    deadline: Deadline,
    lastThrown: Option[Throwable]
  ) {

    /**
     * Executes this attempt.
     *
     * @param s  The scheduler to use to schedule code to run in the future.
     * @param ec The execution context to use for executing code.
     */
    def apply()(implicit s: Scheduler, ec: ExecutionContext): Unit = {
      lazy val timeout = new TimeoutException("AWS operation timed out.")
      deadline.timeLeft match {
        case timeLeft if timeLeft <= Duration.Zero =>
          respond(Failure(lastThrown getOrElse timeout))
        case timeLeft =>
          val latch = new AtomicBoolean
          s.scheduleOnce(timeLeft, new Runnable {
            override def run(): Unit =
              if (latch.compareAndSet(false, true))
                ec.execute(() => respond(Failure(lastThrown getOrElse timeout)))
          })
          f().whenComplete { (response: Response, thrown: Throwable) =>
            if (latch.compareAndSet(false, true)) {
              Option(response) map (Success(_)) getOrElse Failure(thrown) match {
                case Failure(t) if backoff.fromNow < deadline =>
                  s.scheduleOnce(backoff, new Runnable {
                    override def run(): Unit = ec.execute(() => copy(
                      backoff = (backoff.toMillis * 1.61803398875).floor.toLong.millis,
                      lastThrown = Some(t)
                    )())
                  })
                case result =>
                  respond(result)
              }
            }
          }
      }
    }

  }

}