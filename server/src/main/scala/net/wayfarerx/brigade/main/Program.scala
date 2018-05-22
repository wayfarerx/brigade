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

import java.nio.charset.Charset

import collection.JavaConverters._
import concurrent.Await
import concurrent.duration._

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl._

import org.apache.commons.io.IOUtils


/**
 * Main entry point for the bot.
 */
object Program {

  private val S3Pattern = """s3\:\/\/([a-zA-Z0-9_\-]+)\/([a-zA-Z0-9_\-\/]+)""".r

  /**
   * Main entry point for the bot.
   *
   * @param args The command-line arguments.
   */
  def main(args: Array[String]): Unit =
    if (args.length != 1 || args.exists(a => a.equalsIgnoreCase("-h") || a.equalsIgnoreCase("--help"))) {
      println("Usage: java -jar brigade.jar [<DIRECTORY> | s3://<S3_BUCKET>/<S3_PREFIX>]")
      println("  DIRECTORY The local directory to store data in, defaults to the current directory.")
      println("  S3_BUCKET The S3 bucket to use for storage.")
      println("  S3_PREFIX The S3 key prefix to use for storage.")
      System.exit(-1)
    } else {
      var result = 1
      try {
        val system = ActorSystem(behavior, "brigade")
        sys.runtime.addShutdownHook(new Thread() {
          override def run(): Unit = Await.result(system.terminate(), 2.minutes)
        })
        val token = sys.env.getOrElse("DISCORD_TOKEN",
          IOUtils.readLines(System.in, Charset.defaultCharset).asScala.mkString("\n")).trim
        val (storage, reporting) = (???, ???) /*args(0) match {
          case S3Pattern(bucket, prefix) =>
            Storage.Driver.Cloud(AmazonS3ClientBuilder.defaultClient, bucket, prefix) ->
              Reporting.Driver.Cloud(AmazonCloudWatchClientBuilder.defaultClient)
          case local =>
            Storage.Driver.Default(Paths.get(local)) -> Reporting.Driver.Default
        }*/
        system ! Initialize(token, storage, reporting)
        result = 0
      } catch {
        case t: Throwable => t.printStackTrace()
      } finally if (result != 0) System.exit(result)
    }

  private def behavior: Behavior[Action] = Behaviors.receive[Action] { (_, act) =>

    ???
  }

  /**
   * Base class for Discord messages.
   */
  sealed trait Action

  case class Initialize(
    discordToken: String,
    storageDriver: Storage.Driver,
    reportingDriver: Reporting.Driver
  ) extends Action

}
