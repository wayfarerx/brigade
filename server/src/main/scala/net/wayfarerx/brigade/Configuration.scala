/*
 * Configuration.scala
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

/**
 * Base type for brigade configurations.
 */
sealed trait Configuration

/**
 * Definitions of the supported configurations.
 */
object Configuration {

  /**
   * The default configuration that ignores history.
   */
  case object Default extends Configuration

  /**
   * The configuration that attempts to cycle users through various roles.
   *
   * @param history The history to use when deciding what users to place in what roles.
   */
  case class Cycle(history: History) extends Configuration

}
