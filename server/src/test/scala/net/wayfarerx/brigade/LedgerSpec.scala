/*
 * LedgerSpec.scala
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
 * Test case for the message parser.
 */
class LedgerSpec extends FlatSpec with Matchers {

  behavior of "Ledger"

  private val bob = User(1)
  private val sue = User(2)
  private val jim = User(3)
  private val kim = User(4)

  private val tank = Role("tank")
  private val healer = Role("healer")
  private val dps = Role("dps")

  val slots = ListMap(tank -> 1, healer -> 1, dps -> 2)

  it should "build rosters from simple ledgers" in {
    Ledger(
      Ledger.Entry(1, Command.Assign(bob, tank)),
      Ledger.Entry(2, Command.Assign(sue, healer)),
      Ledger.Entry(3, Command.Volunteer(bob, healer), Command.Volunteer(bob, dps)),
      Ledger.Entry(4, Command.Volunteer(sue, tank)),
      Ledger.Entry(5, Command.Volunteer(jim, dps)),
      Ledger.Entry(6, Command.Volunteer(kim, healer), Command.Volunteer(kim, dps)),
      Ledger.Entry(7, Command.Release(bob)),
      Ledger.Entry(8, Command.Drop(bob, healer)),
      Ledger.Entry(9, Command.DropAll(jim))
    ).buildRoster(slots) shouldBe Roster(
      slots,
      Vector(
        sue -> healer
      ),
      Vector(
        bob -> dps,
        sue -> tank,
        kim -> healer,
        kim -> dps
      )
    )
  }

  it should "build rosters from complex ledgers with edits" in {
    Ledger(
      Ledger.Entry(1, Command.Assign(bob, tank)),
      Ledger.Entry(2, Command.Assign(sue, healer)),
      Ledger.Entry(3, Command.Volunteer(bob, healer), Command.Volunteer(bob, dps)),
      Ledger.Entry(4, Command.Volunteer(sue, tank)),
      Ledger.Entry(5, Command.Volunteer(jim, dps)),
      Ledger.Entry(6, Command.Volunteer(kim, healer), Command.Volunteer(kim, dps)),
      Ledger.Entry(1, Command.Assign(bob, dps)),
      Ledger.Entry(3, Command.Volunteer(bob, tank), Command.Volunteer(bob, dps))
    ).buildRoster(slots) shouldBe Roster(
      slots,
      Vector(
        sue -> healer,
        bob -> dps
      ),
      Vector(
        bob -> dps,
        sue -> tank,
        jim -> dps,
        kim -> healer,
        kim -> dps,
        bob -> tank
      )
    )
  }

}
