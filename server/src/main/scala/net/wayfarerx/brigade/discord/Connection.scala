/*
 * Connection.scala
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
package discord

import concurrent.Await
import concurrent.ExecutionContext.Implicits.global
import concurrent.duration._

import java.util.concurrent.CountDownLatch

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl._

import cats.effect.IO

import sx.blah.discord.api.{ClientBuilder, IDiscordClient}
import sx.blah.discord.api.events.IListener

/**
 * A connection to the Discord service.
 *
 * @param system The actor system managed by this connection.
 * @param client The Discord client managed by this connection.
 */
final class Connection private(client: IDiscordClient, system: ActorSystem[NotUsed], listeners: Vector[IListener[_]]) {

  /** The latch that signals termination is complete. */
  private val latch = new CountDownLatch(1)

  /**
   * Waits the specified amount of time for the connection to be closed.
   *
   * @param maxWaitTime The maximum amount of time to wait.
   * @return True if the connection was closed or false if the maximum wait time elapsed first.
   */
  def waitForDisconnect(maxWaitTime: Duration = Duration.Inf): IO[Boolean] = IO {
    maxWaitTime match {
      case duration: FiniteDuration =>
        latch.await(duration.length, duration.unit)
      case _ =>
        latch.await()
        true
    }
  }

  /**
   * Attempts to close this connection.
   *
   * @return The attempt to close this connection.
   */
  def disconnect(): IO[Unit] = IO {

    def unregister(listeners: Vector[IListener[_]]): Unit = if (listeners.nonEmpty)
      try client.getDispatcher.unregisterListener(listeners.head) finally unregister(listeners.tail)

    try {
      try unregister(listeners) finally {
        try Await.result(system.terminate(), 10.seconds) finally client.logout()
      }
    } finally latch.countDown()
  }

}

/**
 * Factory for Discord connections.
 */
object Connection {

  /**
   * Attempts to create a new Discord connection.
   *
   * @param token The token to use when logging in.
   * @param impl  The Discord implementation to use.
   * @return The attempt to create a new Discord connection.
   */
  def apply(token: String)(implicit impl: Discord): IO[Connection] = {

    /* The behavior of this actor. */
    val main: Behavior[NotUsed] = Behaviors.setup { context =>
      val incoming = context.spawn((new Incoming).behavior, "incoming")
      val outgoing = context.spawn((new Outgoing).behavior, "outgoing")
      context.watch(incoming)
      Behaviors.same
    }
    for {
      client <- impl.connect(token)
      system <- IO(ActorSystem(main, "brigade"))

    } yield {
      new Connection(client, system, ???)
    }
  }

  /**
   * Base class for Discord implementations.
   */
  trait Discord {

    /**
     * Attempts to connect to the Discord server with the specified token.
     *
     * @param token The token to connect to the server with.
     * @return The attempt to connect to the Discord server.
     */
    def connect(token: String): IO[IDiscordClient]

  }

  /**
   * Factory for Discord implementations.
   */
  object Discord {

    /** The default Discord implementation. */
    implicit val Default: Discord = Discord(token => IO(new ClientBuilder().withToken(token).login()))

    /**
     * Creates a new Discord implementation from the specified function.
     *
     * @param f The fiunction that defines the Discord implementation.
     * @return A new Discord implementation
     */
    def apply(f: String => IO[IDiscordClient]): Discord = f(_)

  }

}