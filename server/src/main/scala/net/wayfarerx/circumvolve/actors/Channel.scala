package net.wayfarerx.circumvolve
package actors

import akka.actor.typed.scaladsl._

final class Channel(val id: Channel.Id) {

  import Channel._

  def behavior: Behaviors.Immutable[Action] = Behaviors.immutable[Action] { (_, action) =>


    ???
  }

}

object Channel {

  final class Id(val value: Long) extends AnyVal

}
