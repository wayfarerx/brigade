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

/**
 * Test case for the ledger.
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

  it should "build rosters from simple ledgers" in {
    Ledger(
      Ledger.Entry(Message.Id(1), Command.Assign(bob, tank)),
      Ledger.Entry(Message.Id(2), Command.Assign(sue, healer)),
      Ledger.Entry(Message.Id(3), Command.Volunteer(bob, healer), Command.Volunteer(bob, dps)),
      Ledger.Entry(Message.Id(4), Command.Volunteer(sue, tank)),
      Ledger.Entry(Message.Id(5), Command.Volunteer(jim, dps)),
      Ledger.Entry(Message.Id(6), Command.Volunteer(kim, healer), Command.Volunteer(kim, dps)),
      Ledger.Entry(Message.Id(7), Command.Release(bob)),
      Ledger.Entry(Message.Id(8), Command.Drop(bob, healer)),
      Ledger.Entry(Message.Id(9), Command.DropAll(jim))
    ).buildRoster() shouldBe Roster(
      Vector(
        sue -> healer
      ),
      Vector(
        (bob, dps, 0),
        (sue, tank, 0),
        (kim, healer, 0),
        (kim, dps, 1)
      )
    )
  }

  it should "build rosters from complex ledgers with edits" in {
    Ledger(
      Ledger.Entry(Message.Id(1), Command.Assign(bob, tank)),
      Ledger.Entry(Message.Id(2), Command.Assign(sue, healer)),
      Ledger.Entry(Message.Id(3), Command.Volunteer(bob, healer), Command.Volunteer(bob, dps)),
      Ledger.Entry(Message.Id(4), Command.Volunteer(sue, tank)),
      Ledger.Entry(Message.Id(5), Command.Volunteer(jim, dps)),
      Ledger.Entry(Message.Id(6), Command.Volunteer(kim, healer), Command.Volunteer(kim, dps)),
      Ledger.Entry(Message.Id(1), Command.Assign(bob, dps)),
      Ledger.Entry(Message.Id(3), Command.Volunteer(bob, tank), Command.Volunteer(bob, dps))
    ).buildRoster() shouldBe Roster(
      Vector(
        sue -> healer,
        bob -> dps
      ),
      Vector(
        (bob, dps, 1),
        (sue, tank, 0),
        (jim, dps, 0),
        (kim, healer, 0),
        (kim, dps, 1),
        (bob, tank, 0)
      )
    )
  }

  it should "preserve user preference after edits" in {
    Ledger(
      Ledger.Entry(Message.Id(1), Command.Volunteer(bob, tank), Command.Volunteer(bob, healer)),
      Ledger.Entry(Message.Id(1), Command.Volunteer(bob, healer), Command.Volunteer(bob, tank))
    ).buildRoster() shouldBe Roster(
      Vector(),
      Vector(
        (bob, tank, 1),
        (bob, healer, 0)
      )
    )

  }

}
