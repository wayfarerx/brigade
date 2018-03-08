package net.wayfarerx.circumvolve
package guilds

import akka.actor.typed.scaladsl.Behaviors

trait Guild {

  def behavior: Behaviors.Immutable[Action]

}
