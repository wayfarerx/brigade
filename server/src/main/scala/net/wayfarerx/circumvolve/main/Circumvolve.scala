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

import java.io.{BufferedReader, InputStreamReader}

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
    if (args.length != 0 && args.length != 2) {
      println("Usage: circumvolve (S3_BUCKET S3_PATH)")
      println("  The Discord login token must be provided on the system input stream: cat token.txt | circumvolve ...")
      println("  S3_BUCKET     The S3 bucket to use for storage (only if using S3).")
      println("  S3_PATH       The S3 key prefix to use for storage (only if using S3).")
      System.exit(1)
    } else {
      val storage = if (args.length == 1) Storage.Empty else
        new Storage.S3Storage(args(0), normalizeS3Path(args(1)))
      var result = 1
      try {
        val system = ActorSystem("circumvolve")
        sys.runtime.addShutdownHook(new Thread() {
          override def run(): Unit = Await.result(system.terminate(), 10.seconds)
        })
        try {
          val token = new BufferedReader(new InputStreamReader(System.in)).readLine()
          system.actorOf(Connection(token, storage), "connection")
          result = 0
        } catch {
          case e: Exception => e.printStackTrace()
        } finally if (result != 0) Await.result(system.terminate(), 10.seconds)
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

  /*else if (path.charAt(0) == '/') trimS3Path(path.subSequence(1, path.length()))
  else if (path.charAt(path.length - 1) == '/') trimS3Path(path.subSequence(0, path.length - 1))
  else path*/

}
