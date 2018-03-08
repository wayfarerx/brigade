package net.wayfarerx.circumvolve
package discord

sealed trait Event {

}

object Event {

  sealed trait GuildEvent {

    def guildId: GuildId

  }

  sealed trait SystemEvent {


  }


  case object Hi extends SystemEvent

  case object Ho extends SystemEvent

}
