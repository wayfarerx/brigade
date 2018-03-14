package net.wayfarerx.brigade
package actors

sealed trait Action {

}

object Action {


  case object Hi extends Action

  case object Ho extends Action

}

