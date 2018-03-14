/*
 * Incoming.scala
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

import akka.actor.typed.scaladsl._

/**
 * The actor responsible for handling events by sending actions to the appropriate guild.
 */
final class Incoming {

  import Incoming._

  /** The behavior of this actor. */
  val behavior: Behaviors.Immutable[Event] = {
    Behaviors.immutable[Event] {
      case (context, event) =>
        println(context.toString + event)
        Behaviors.same
    }
  }

}

object Incoming {

  sealed trait Event {

  }


  case object Hi extends Event

  case object Ho extends Event

}