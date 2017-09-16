/*
 * Circumvolve.scala
 *
 * Copyright 2017 wayfarerx <x@wayfarerx.net> (@thewayfarerx)
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

package net.wayfarerx.circumvolve.main

import java.io.{BufferedReader, File, IOException, InputStreamReader}

import concurrent.Await
import concurrent.duration._
import akka.actor.ActorSystem
import net.wayfarerx.circumvolve.service.{Connection, Storage}

/**
 * Main entry point for the bot.
 */
object Circumvolve {

  /**
   * Main entry point for the bot.
   *
   * @param args The command-line arguments.
   */
  def main(args: Array[String]): Unit =
    if (args.length != 1 || args.length != 3) {
      println("Usage: circumvolve LOGIN_TOKEN (S3_BUCKET, S3_PATH)")
      println("  LOGIN_TOKEN The token to log in to Discord with.")
      println("  S3_BUCKET   The S3 bucket to use for storage (optional).")
      println("  S3_PATH     The S3 key prefix to use for storage (optional).")
      System.exit(1)
    } else {
      val token = args(0)
      val storage = if (args.length == 1) Storage.Empty else new Storage.S3Storage(args(1), args(2))
      // S3 Stuffs
      var result = 1
      try {
        val system = ActorSystem("circumvolve")
        try {
          system.actorOf(Connection(token, storage), "connection")
          result = waitForExit(new BufferedReader(new InputStreamReader(System.in)))
        } catch {
          case e: IOException => System.err.println(e.getMessage)
        } finally Await.result(system.terminate(), 10.seconds)
      } finally System.exit(result)
    }

  /**
   * Waits until told to exit or the system input stream ends.
   *
   * @param reader The reader to read lines from.
   * @return The normal exit code.
   */
  @annotation.tailrec
  private def waitForExit(reader: BufferedReader): Int =
  Option(reader.readLine()) match {
    case Some(line) if line.trim equalsIgnoreCase "exit" => 0
    case Some(_) => waitForExit(reader)
    case None => 0
  }

}
