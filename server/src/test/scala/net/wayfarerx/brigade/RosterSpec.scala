/*
 * RosterSpec.scala
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

package net.wayfarerx.brigade

import org.scalatest._

import scala.collection.immutable.ListMap

/**
 * Test case for the team roster.
 */
class RosterSpec extends FlatSpec with Matchers {

  behavior of "Roster"

  val tank = Role("tank")
  val heal = Role("heal")
  val dps = Role("dps")

  val amy = User(1)
  val ann = User(2)
  val ben = User(3)
  val bob = User(4)
  val gus = User(5)
  val ida = User(6)
  val jim = User(7)
  val leo = User(8)
  val liz = User(9)
  val may = User(10)
  val rod = User(11)
  val sam = User(12)
  val sue = User(13)
  val zoe = User(14)

  /*

  it should "build partial teams" in {
    val roster = Roster(
      Vector(amy -> tank),
      Vector(
        (bob, heal, 0),
        (jim, dps, 0),
        (sue, heal, 0)
      )
    )
    val teams = roster.buildTeams(ListMap(tank -> 1, heal -> 1, dps -> 2), Roster.Fill)
    teams shouldBe Vector(Team(ListMap(
      tank -> Vector(amy),
      heal -> Vector(bob),
      dps -> Vector(jim)
    )))
    //unassigned shouldBe Vector(sue)
  }

  it should "build 4-person groups" in {
    val roster = Roster(
      Vector(amy -> tank),
      Vector(
        (bob, heal, 0),
        (jim, dps, 0),
        (sam, dps, 0),
        (sue, heal, 0),
        (sue, dps, 1)
      )
    )
    val teams = roster.buildTeams(ListMap(tank -> 1, heal -> 1, dps -> 2), Roster.Fill)
    teams shouldBe Vector(Team(ListMap(
      tank -> Vector(amy),
      heal -> Vector(bob),
      dps -> Vector(jim, sam)
    )), Team(ListMap(
      tank -> Vector(),
      heal -> Vector(sue),
      dps -> Vector()
    )))
  }

  it should "build 12-person groups" in {
    val roster = Roster(
      Vector(amy -> tank, ann -> heal, ben -> dps, bob -> dps),
      Vector(
        (gus, tank, 0),
        (ida, heal, 0),
        (jim, dps, 0),
        (leo, dps, 0),
        (liz, dps, 0),
        (may, tank, 0),
        (may, dps, 1),
        (rod, heal, 0),
        (rod, dps, 1),
        (sam, dps, 0),
        (sue, tank, 0),
        (sue, dps, 1),
        (zoe, heal, 0),
        (zoe, dps, 1)
      )
    )
    val teams = roster.buildTeams(ListMap(tank -> 2, heal -> 2, dps -> 8), Roster.Fill)
    teams shouldBe Vector(Team(ListMap(
      tank -> Vector(amy, gus),
      heal -> Vector(ann, ida),
      dps -> Vector(ben, bob, jim, leo, liz, sam, may, rod)
    )), Team(ListMap(
      tank -> Vector(sue),
      heal -> Vector(zoe),
      dps -> Vector()
    )))
  }

  it should "score candidates based on history" in {
    val oldTeam = Team(ListMap(tank -> Vector(amy), heal -> Vector(ann), dps -> Vector(ben, bob)))
    val roster = Roster(
      Vector(amy -> tank),
      Vector(
        (ann, heal, 0),
        (ben, dps, 0),
        (bob, dps, 0),
        (sue, heal, 0),
        (sue, dps, 1)
      )
    )
    val teams = roster.buildTeams(
      ListMap(tank -> 1, heal -> 1, dps -> 2),
      Roster.Rotate(History(Vector(Set(oldTeam))))
    )
    teams shouldBe Vector(Team(ListMap(
      tank -> Vector(amy),
      heal -> Vector(sue),
      dps -> Vector(ben, bob)
    )), Team(ListMap(
      tank -> Vector(),
      heal -> Vector(ann),
      dps -> Vector()
    )))
  }

  it should "favor roles higher on the user preference list" in {
    val roster = Roster(
      Vector(amy -> tank),
      Vector(
        (bob, heal, 0),
        (bob, dps, 1),
        (sue, heal, 0),
        (jim, dps, 0),
        (sam, dps, 0)
      )
    )
    val teams = roster.buildTeams(ListMap(tank -> 1, heal -> 1, dps -> 2), Roster.Fill)
    teams shouldBe Vector(Team(ListMap(
      tank -> Vector(amy),
      heal -> Vector(bob),
      dps -> Vector(jim, sam)
    )), Team(ListMap(
      tank -> Vector(),
      heal -> Vector(sue),
      dps -> Vector()
    )))
  }

  it should "fill as many roles as possible" in {
    val roster = Roster(
      Vector(amy -> tank),
      Vector(
        (bob, tank, 0),
        (bob, dps, 1),
        (sue, tank, 0),
        (sue, dps, 1),
        (jim, dps, 0),
        (jim, heal, 1)
      )
    )
    val teams = roster.buildTeams(ListMap(tank -> 1, heal -> 1, dps -> 2), Roster.Fill)
    teams shouldBe Vector(Team(ListMap(
      tank -> Vector(amy),
      heal -> Vector(jim),
      dps -> Vector(bob, sue)
    )))
  }

  */

}
