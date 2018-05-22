/*
 * Shards.scala
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

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ExecutorService, Executors, LinkedBlockingQueue, TimeoutException}

import concurrent.duration._
import util.{Failure, Try}

/**
 * Represents a collection of shards indexed by the masked hash of a key.
 *
 * @tparam K The type of key that selects shards.
 * @param mask           The mask to apply to all key hashes.
 * @param pool           The thread pool to manage.
 * @param queues         The shards indexed by masked key hashes.
 * @param disposeTimeout The amount of time to wait for shards to shut down.
 */
final class Shards[K: Shards.Hashed] private(
  mask: Int,
  pool: ExecutorService,
  queues: Vector[LinkedBlockingQueue[Shards.Work[_]]],
  disposeTimeout: FiniteDuration
) {

  import Shards._

  /** The scheduler to use when scheduling deadlines. */
  private val scheduler = Executors.newScheduledThreadPool(1)

  /**
   * Submits a function and call back to the shard for the specified key.
   *
   * @tparam T The type returned from the function.
   * @param key      The key to shard on.
   * @param f        The function to apply.
   * @param deadline The deadline that this operation must complete by.
   * @param callback The call back to report the outcome to.
   * @return True if the submission was accepted.
   */
  def apply[T](
    key: K,
    f: => T,
    deadline: Deadline = (disposeTimeout / 2).fromNow
  )(
    callback: Try[T] => Unit
  ): Boolean =
    enqueue(key, Task(() => f, callback), deadline)

  /**
   * Submits a collection of input, a function and a call back to the shard for the specified key.
   *
   * @tparam T The type returned from the function.
   * @tparam I The type of each task's input.
   * @param key      The key to shard on.
   * @param input    The input to apply the function to.
   * @param f        The function to apply to each input.
   * @param deadline The deadline that this operation must complete by.
   * @param callback The call back to report the outcome to.
   * @return True if the submission was accepted.
   */
  def each[I, T](
    key: K,
    input: Vector[I],
    f: I => T,
    deadline: Deadline = (disposeTimeout / 2).fromNow
  )(
    callback: Try[Vector[T]] => Unit
  ): Boolean =
    enqueue(key, Tasks(input, f, callback), deadline)

  /**
   * Disposes this collection of shards.
   */
  def dispose(): Unit =
    try pool.shutdown() finally
      try pool.awaitTermination(disposeTimeout.length, disposeTimeout.unit) catch {
        case _: InterruptedException =>
      } finally scheduler.shutdown()

  /**
   * Enqueues work to be performed.
   *
   * @tparam T The type produced by the work.
   * @param key      The key to shard on.
   * @param work     The work to perform.
   * @param deadline The deadline that this operation must complete by.
   * @return True if the submission was accepted.
   */
  private def enqueue[T](key: K, work: Work[T], deadline: Deadline): Boolean =
    if (pool.isShutdown) false else {
      val completed = new AtomicBoolean
      queues(implicitly[Hashed[K]].hash(key) & mask)
        .put(work.wrapCallback(f => t => if (completed.compareAndSet(false, true)) f(t)))
      deadline.timeLeft match {
        case remaining if remaining > Duration.Zero =>
          scheduler.schedule(new Runnable {
            override def run(): Unit =
              if (completed.compareAndSet(false, true))
                work.callback(Failure(new TimeoutException("Failed to return in time.")))
          }, remaining.length, remaining.unit)
        case _ =>
          if (completed.compareAndSet(false, true))
            work.callback(Failure(new TimeoutException("Failed to dispatch in time.")))
      }
      true
    }

}

/**
 * Factory for collections of shards.
 */
object Shards {

  /**
   * Creates a collection of shards indexed by keys.
   *
   * @tparam K The type of key that selects shards.
   * @param bits     The number of bits to use when hashing and masking keys.
   * @param retry    The retry configuration to use.
   * @param deadline The shutdown deadline to honor.
   * @return A collection of shards indexed by key hashes.
   */
  def apply[K: Shards.Hashed](bits: Int, retry: Retry, deadline: FiniteDuration): Shards[K] = {
    val factor = Math.max(1, Math.min(bits, 31))
    val size = 1 << factor
    val pool = Executors.newFixedThreadPool(size)
    val queues = Vector.fill(size)(new LinkedBlockingQueue[Shards.Work[_]])
    for (queue <- queues) pool.submit(new Runnable {
      @annotation.tailrec
      override def run(): Unit =
        if (!pool.isShutdown || !queue.isEmpty) {
          for {
            work <- try Option(queue.poll(retry.maximumBackOff.length, retry.maximumBackOff.unit)) catch {
              case _: InterruptedException => None
            }
          } work(retry)
          run()
        }
    })
    new Shards[K](0xFFFFFFFF >>> (32 - factor), pool, queues, deadline)
  }

  /**
   * A single unit of work submitted to a shard.
   *
   * @tparam T The type of the work's result.
   */
  sealed trait Work[T] {

    /** The callback to invoke when this work has completed. */
    def callback: Try[T] => Unit

    /**
     * Executes this unit of work with the specified retry configuration.
     *
     * @param retry The retry configuration to use.
     */
    def apply(retry: Retry): Unit

    /**
     * Returns a copy of this work with its callback wrapped.
     *
     * @param wrap The function that wraps the callback.
     * @return The new work with the callback wrapped.
     */
    def wrapCallback(wrap: (Try[T] => Unit) => (Try[T] => Unit)): Work[T]

  }

  /**
   * A single task submitted to a shard.
   *
   * @tparam T The type of the task's result.
   * @param f        The function to apply.
   * @param callback The receiver of the ultimate outcome.
   */
  case class Task[T](f: () => T, callback: Try[T] => Unit) extends Work[T] {

    /* Retry the function and report the outcome to the callback. */
    override def apply(retry: Retry): Unit = callback(retry(f))

    /* Wrap the callback. */
    override def wrapCallback(wrap: (Try[T] => Unit) => Try[T] => Unit): Task[T] = Task(f, wrap(callback))

  }

  /**
   * A single task submitted to a shard.
   *
   * @tparam I The type of each task's input.
   * @tparam T The type of the task's result.
   * @param input    The input for this collection of tasks.
   * @param f        The function to apply.
   * @param callback The receiver of the ultimate outcome.
   */
  case class Tasks[I, T](input: Vector[I], f: I => T, callback: Try[Vector[T]] => Unit) extends Work[Vector[T]] {

    /* Retry the function and report the outcome to the callback. */
    override def apply(retry: Retry): Unit = callback(retry.each(input, f))

    /* Wrap the callback. */
    override def wrapCallback(wrap: (Try[Vector[T]] => Unit) => Try[Vector[T]] => Unit): Tasks[I, T] =
      Tasks(input, f, wrap(callback))

  }

  /**
   * Support for hashing possible keys.
   *
   * @tparam T The type of key to hash.
   */
  trait Hashed[-T] {

    /**
     * Hashes the specified key.
     *
     * @param key The key to hash.
     * @return The hash of the specified key.
     */
    def hash(key: T): Int

  }

  /**
   * Defines the universal hash.
   */
  object Hashed {

    /** The default, universal hash. */
    implicit val Default: Hashed[Any] = _.hashCode

  }

}
