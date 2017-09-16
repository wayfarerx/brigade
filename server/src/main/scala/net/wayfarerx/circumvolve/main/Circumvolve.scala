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
    if (args.length != 1 && args.length != 5) {
      println("Usage: circumvolve LOGIN_TOKEN (S3_BUCKET S3_PATH, S3_ACCESS_KEY S3_SECRET_KEY)")
      println("  LOGIN_TOKEN   The token to log in to Discord with.")
      println("  S3_BUCKET     The S3 bucket to use for storage (optional).")
      println("  S3_PATH       The S3 key prefix to use for storage (optional).")
      println("  S3_ACCESS_KEY The S3 access key to use for storage (optional).")
      println("  S3_SECRET_KEY The S3 secret key to use for storage (optional).")
      System.exit(1)
    } else {
      val token = args(0)
      val storage = if (args.length == 1) Storage.Empty else
        new Storage.S3Storage(args(1), trimS3Path(args(2)), args(3), args(4))
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

  /**
   * Trims all leading and trailing slashes on S3 paths.
   *
   * @param path The path to trim.
   * @return The trimmed path.
   */
  @annotation.tailrec
  private def trimS3Path(path: String): String =
    if (path startsWith "/") trimS3Path(path substring 1)
    else if (path endsWith "/") trimS3Path(path.substring(0, path.length - 1))
    else path

}
