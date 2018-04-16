/*
 * Program.scala
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

import java.io.{BufferedReader, InputStreamReader}

import collection.JavaConverters._
import concurrent.Await
import concurrent.duration._

import akka.actor.typed.{ActorSystem, PostStop}
import akka.actor.typed.scaladsl._

import sx.blah.discord.api.{ClientBuilder, IDiscordClient}
import sx.blah.discord.api.events.IListener

/**
 * Main entry point for the bot.
 */
object Program {

  /**
   * Main entry point for the bot.
   *
   * @param args The command-line arguments.
   */
  def main(args: Array[String]): Unit =
    if (args.length != 0 && args.length != 2) {
      println("Usage: brigade (S3_BUCKET S3_PATH)")
      println("  The Discord token must be provided on the system input stream i.e. `cat token.txt | brigade ...`")
      println("  S3_BUCKET     The S3 bucket to use for storage (only if using S3).")
      println("  S3_PATH       The S3 key prefix to use for storage (only if using S3).")
      System.exit(1)
    } else {
      println("boot")
      var result = 1
      try {
        lazy val system = ActorSystem(
          controller(if (args.isEmpty) None else Some(args(0) -> normalizeS3Path(args(1)))),
          "brigade"
        )
        sys.runtime.addShutdownHook(new Thread() {
          override def run(): Unit = Await.result(system.terminate(), 10.seconds)
        })
        system ! sys.env.getOrElse("DISCORD_TOKEN", new BufferedReader(new InputStreamReader(System.in)).readLine())
        result = 0
      } catch {
        case t: Throwable => t.printStackTrace()
      } finally if (result != 0) System.exit(result)
    }

  /**
   * Trims all leading and trailing slashes on S3 paths and replaces back slashes with forward slashes.
   *
   * @param path The path to trim.
   * @return The trimmed path.
   */
  @annotation.tailrec
  private def normalizeS3Path(path: CharSequence): String =
    if (path.length == 0) "" else path charAt 0 match {
      case '/' | '\\' => normalizeS3Path(path.subSequence(1, path.length()))
      case _ => path charAt path.length - 1 match {
        case '/' | '\\' => normalizeS3Path(path.subSequence(0, path.length - 1))
        case _ => path.toString.replace('\\', '/')
      }
    }

  private def controller(storage: Option[(String, String)]) = Behaviors.immutable[String] { (ctx, token) =>
    println("startup")
    val listeners = Vector[IListener[_]](
      /*OnKickedFrom(ctx.self),
      OnRemovedFrom(ctx.self),
      OnReceived(ctx.self),
      OnUpdated(ctx.self),
      OnDeleted(ctx.self),
      OnPinned(ctx.self),
      OnUnpinned(ctx.self)*/
    )
    var success = false
    val discordClient = new ClientBuilder().setDaemon(true).withToken(token).login()
    try {
      for {
        guild <- discordClient.getGuilds.asScala
        channel <- guild.getChannels.asScala
      } yield {
        val messages = channel.getPinnedMessages.asScala filter (_.getAuthor.getLongID == guild.getOwnerLongID)

        ???
      }
      val dispatcher = discordClient.getDispatcher
      listeners foreach dispatcher.registerListener
      success = true
    } finally if (!success) discordClient.logout()

    Behaviors.immutable[String] { (_, _) =>
      Behaviors.stopped {
        Behaviors.onSignal {
          case (_, PostStop) =>
            println("shutdown")
            // TODO unregister
            Behaviors.same
        }
      }
    }
  }

}
