/*
 * BrigadeSpec.scala
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

import language.implicitConversions

import org.scalatest._

/**
 * Test case for the brigade.
 */
class BrigadeSpec extends FlatSpec with Matchers {

  behavior of "Brigade"

  private val bob = User(1)
  private val sue = User(2)
  private val jim = User(3)
  private val kim = User(4)

  private val tank = Role("tank")
  private val healer = Role("healer")
  private val dps = Role("dps")

  implicit def mentions(user: User): Vector[Message.Token] = Vector(Message.Mention(user))

  implicit def words(value: String): Vector[Message.Token] = value.trim.split("""\s+""").map(Message.Word).toVector

  def msg(tokens: Vector[Message.Token]*): Vector[Message.Token] = (Vector[Message.Token]() /: tokens)(_ ++ _)


  it should "build ledgers from message streams" in {
    ((Brigade.Inactive: Brigade) /: Vector(
      Message(Message.Id(0), 0, bob, msg("!open !tank 1 !healer 1 !dps 2")),
      Message(Message.Id(1), 1, bob, msg("!assign", bob, "!tank")),
      Message(Message.Id(2), 2, sue, msg("!assign", sue, "!healer")),
      Message(Message.Id(3), 3, bob, msg("!healer !dps")),
      Message(Message.Id(4), 4, sue, msg("!tank")),
      Message(Message.Id(5), 5, jim, msg("!offer", jim, "!dps")),
      Message(Message.Id(6), 6, kim, msg("!offer", kim, "!healer !offer", kim, "!dps")),
      Message(Message.Id(7), 7, bob, msg("!release", bob)),
      Message(Message.Id(8), 8, bob, msg("!drop !healer")),
      Message(Message.Id(9), 9, jim, msg("!kick", jim))
    )) ((previous, message) => previous(message).brigade)
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
        (bob, dps, 1), // FIXME
        (sue, tank, 0),
        (kim, healer, 0),
        (kim, dps, 1)
      )
    )
  }

}
