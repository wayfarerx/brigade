/*
 * SolverSpec.scala
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

package net.wayfarerx.circumvolve.model

import org.scalatest._

/**
 * Test case for the team solver.
 */
class SolverSpec extends FlatSpec with Matchers {

  val tank = Role("tank")
  val healer = Role("healer")
  val dps = Role("dps")

  val amy = Member("Amy")
  val ann = Member("Ann")
  val ben = Member("Ben")
  val bob = Member("Bob")
  val gus = Member("Gus")
  val ida = Member("Ida")
  val jim = Member("Jim")
  val leo = Member("Leo")
  val liz = Member("Liz")
  val may = Member("May")
  val rod = Member("Rod")
  val sam = Member("Sam")
  val sue = Member("Sue")
  val zoe = Member("Zoe")

  "The solver" should "build 4-man groups" in {
    val (_, Some(team)) = Event("dungeon")
      .open(Map(tank -> 1, healer -> 1, dps -> 2))
      .assign(Vector(amy -> tank))
      .volunteer(bob, Vector(healer))
      .volunteer(jim, Vector(dps))
      .volunteer(sam, Vector(dps))
      .volunteer(sue, Vector(healer, dps))
      .close()
    team shouldBe Team(
      Map(tank -> Vector(amy), healer -> Vector(bob), dps -> Vector(jim, sam)),
      Map(sue -> Vector(healer, dps))
    )
  }

  "The solver" should "build 12-man groups" in {
    val (_, Some(team)) = Event("trial")
      .open(Map(tank -> 2, healer -> 2, dps -> 8))
      .assign(Vector(amy -> tank, ann -> healer, ben -> dps, bob -> dps))
      .volunteer(gus, Vector(tank))
      .volunteer(ida, Vector(healer))
      .volunteer(jim, Vector(dps))
      .volunteer(leo, Vector(dps))
      .volunteer(liz, Vector(dps))
      .volunteer(may, Vector(tank, dps))
      .volunteer(rod, Vector(healer, dps))
      .volunteer(sam, Vector(dps))
      .volunteer(sue, Vector(tank, dps))
      .volunteer(zoe, Vector(healer, dps))
      .close()
    team shouldBe Team(
      Map(tank -> Vector(amy, gus), healer -> Vector(ann, ida), dps -> Vector(ben, bob, jim, leo, liz, sam, may, rod)),
      Map(sue -> Vector(tank, dps), zoe -> Vector(healer, dps))
    )
  }

  "The solver" should "score candidates based on history" in {
    val oldTeam = Team(Map(tank -> Vector(amy), healer -> Vector(ann), dps -> Vector(ben, bob)), Map())
    val (_, Some(team)) = Event("dungeon", history = History(Vector(oldTeam)))
      .open(Map(tank -> 1, healer -> 1, dps -> 2))
      .assign(Vector(amy -> tank))
      .volunteer(ann, Vector(healer))
      .volunteer(ben, Vector(dps))
      .volunteer(bob, Vector(dps))
      .volunteer(sue, Vector(healer, dps))
      .close()
    team shouldBe Team(
      Map(tank -> Vector(amy), healer -> Vector(sue), dps -> Vector(ben, bob)),
      Map(ann -> Vector(healer))
    )
  }

}
