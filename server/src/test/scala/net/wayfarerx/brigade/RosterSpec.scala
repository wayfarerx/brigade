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

  it should "build 4-person groups" in {
    val roster = Roster(
      ListMap(tank -> 1, heal -> 1, dps -> 2),
      Vector(amy -> tank),
      Vector(
        bob -> heal,
        jim -> dps,
        sam -> dps,
        sue -> heal,
        sue -> dps
      )
    )
    val (teams, unassigned) = roster.buildTeams(Roster.Fill)
    teams shouldBe Vector(Team(ListMap(
      tank -> Vector(amy),
      heal -> Vector(bob),
      dps -> Vector(jim, sam)
    )))
    unassigned shouldBe Vector(sue)
  }

  it should "build 12-person groups" in {
    val roster = Roster(
      ListMap(tank -> 2, heal -> 2, dps -> 8),
      Vector(amy -> tank, ann -> heal, ben -> dps, bob -> dps),
      Vector(
        gus -> tank,
        ida -> heal,
        jim -> dps,
        leo -> dps,
        liz -> dps,
        may -> tank,
        may -> dps,
        rod -> heal,
        rod -> dps,
        sam -> dps,
        sue -> tank,
        sue -> dps,
        zoe -> heal,
        zoe -> dps
      )
    )
    val (teams, unassigned) = roster.buildTeams(Roster.Fill)
    teams shouldBe Vector(Team(ListMap(
      tank -> Vector(amy, gus),
      heal -> Vector(ann, ida),
      dps -> Vector(ben, bob, jim, leo, liz, sam, may, rod)
    )))
    unassigned shouldBe Vector(sue, zoe)
  }

  it should "score candidates based on history" in {
    val oldTeam = Team(ListMap(tank -> Vector(amy), heal -> Vector(ann), dps -> Vector(ben, bob)))
    val roster = Roster(
      ListMap(tank -> 1, heal -> 1, dps -> 2),
      Vector(amy -> tank),
      Vector(
        ann -> heal,
        ben -> dps,
        bob -> dps,
        sue -> heal,
        sue -> dps
      )
    )
    val (teams, unassigned) = roster.buildTeams(Roster.Rotate(History(Vector(Set(oldTeam)))))
    teams shouldBe Vector(Team(ListMap(
      tank -> Vector(amy),
      heal -> Vector(sue),
      dps -> Vector(ben, bob)
    )))
    unassigned shouldBe Vector(ann)
  }

  it should "favor roles higher on the user preference list" in {
    val roster = Roster(
      ListMap(tank -> 1, heal -> 1, dps -> 2),
      Vector(amy -> tank),
      Vector(
        bob -> heal,
        bob -> dps,
        sue -> heal,
        jim -> dps,
        sam -> dps
      )
    )
    val (teams, unassigned) = roster.buildTeams(Roster.Fill)
    teams shouldBe Vector(Team(ListMap(
      tank -> Vector(amy),
      heal -> Vector(bob),
      dps -> Vector(jim, sam)
    )))
    unassigned shouldBe Vector(sue)
  }

}
