/*
 * Retry.scala
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

import concurrent.duration._
import util.{Failure, Success, Try}

/**
 * A tool for retrying operations on failure.
 *
 * @param initialBackOff The initial back off duration.
 * @param maximumBackOff The maximum back off duration.
 */
case class Retry(
  initialBackOff: FiniteDuration = 1.second,
  maximumBackOff: FiniteDuration = 10.seconds
) {

  /**
   * Retries the specified operation, backing off after failures.
   *
   * @tparam T The type of result returned by the operation.
   * @param f The operation to retry on failure.
   * @return The outcome of retrying the operation.
   */
  def apply[T](f: () => T): Try[T] = {

    @annotation.tailrec
    def attempt(currentBackoff: FiniteDuration, totalBackOff: FiniteDuration): Try[T] =
      Try(f()) match {
        case result@Success(_) =>
          result
        case Failure(thrown) =>
          backoff(thrown, currentBackoff, totalBackOff) match {
            case Success((next, nextTotal)) => attempt(next, nextTotal)
            case Failure(t) => Failure(t)
          }
      }

    attempt(initialBackOff, Duration.Zero)
  }

  /**
   *
   * @tparam I The type of input object.
   * @tparam T The type of output object.
   * @param input The collection of input objects.
   * @param f     The function to apply to input objects.
   * @return The result of applying the function to each input object.
   */
  def each[I, T](input: Vector[I], f: I => T): Try[Vector[T]] = {

    @annotation.tailrec
    def attempt(
      remaining: Vector[I],
      collected: Vector[T],
      currentBackoff: FiniteDuration,
      totalBackOff: FiniteDuration
    ): Try[Vector[T]] =
      if (remaining.isEmpty) Success(collected) else Try(f(remaining.head)) match {
        case Success(result) =>
          attempt(remaining.tail, collected :+ result, currentBackoff, totalBackOff)
        case Failure(thrown) =>
          backoff(thrown, currentBackoff, totalBackOff) match {
            case Success((next, nextTotal)) => attempt(remaining, collected, next, nextTotal)
            case Failure(t) => Failure(t)
          }

      }

    attempt(input, Vector(), initialBackOff, Duration.Zero)
  }

  /**
   * Calculates the subsequent backoff information and pauses the thread if allowed.
   *
   * @param thrown         The cause of the backoff.
   * @param currentBackoff The current backoff value.
   * @param totalBackOff   The current backoff total.
   * @return The attempt to calculate the subsequent backoff information and pause the thread.
   */
  private def backoff(
    thrown: Throwable,
    currentBackoff: FiniteDuration,
    totalBackOff: FiniteDuration
  ): Try[(FiniteDuration, FiniteDuration)] = {
    val next = (currentBackoff.toMillis * 1.61803398875).floor.toLong.millis
    val nextTotal = totalBackOff + currentBackoff
    if (nextTotal > maximumBackOff) Failure(thrown) else {
      try Thread.sleep(currentBackoff.toMillis) catch {
        case _: InterruptedException => ()
      }
      Success(next, nextTotal)
    }
  }

}
