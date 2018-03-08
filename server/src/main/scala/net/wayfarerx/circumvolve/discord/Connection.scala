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

package net.wayfarerx.circumvolve
package discord

import concurrent.{Await, ExecutionContext, Future}
import concurrent.duration._
import java.io.Closeable

import cats.effect.IO
import sx.blah.discord.api.{ClientBuilder, IDiscordClient}

/**
 * A connection to the Discord service.
 *
 * @param system The actor system managed by this connection.
 * @param client The Discord client managed by this connection.
 */
final class Connection private(system: Closeable, client: IDiscordClient)(implicit ec: ExecutionContext) {

  /**
   * Attempts to close this connection.
   *
   * @return The attempt to close this connection.
   */
  def dispose(): IO[Unit] =
    IO(try Await.result(Future(system.close()), 10.seconds) finally client.logout())

}

/**
 * Factory for Discord connections.
 */
object Connection {

  /**
   * Attempts to create a new Discord connection.
   *
   * @param token   The token to use when logging in.
   * @param handler The Discord event handler.
   * @param impl    The Discord implementation to use.
   * @return The attempt to create a new Discord connection.
   */
  def apply(token: String)(handler: Event => IO[Unit])(implicit impl: Discord): IO[Connection] = {
    for {
      client <- impl.connect(token)

    } yield new Connection(???, client)
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