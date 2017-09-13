/*
 * ParserSpec.scala
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

package net.wayfarerx.circumvolve.service

import org.scalatest._

import net.wayfarerx.circumvolve.model.{Member, Role}

/**
 * Test case for the message parser.
 */
class ParserSpec extends FlatSpec with Matchers {

  val tank = Role("tank")
  val heal = Role("heal")
  val dps = Role("dps")

  val amy = Member("Amy")
  val ann = Member("Ann")
  val ben = Member("Ben")
  val bob = Member("Bob")

  "The parser" should "handle event lifecycle actions" in {
    Parser(Map(), "!event open !tank 1 !heal 2 !dps 9") shouldBe
      Vector(Action.Open(Vector(tank -> 1, heal -> 2, dps -> 9)))
    Parser(Map(), "!event abort") shouldBe
      Vector(Action.Abort)
    Parser(Map(), "!event close") shouldBe
      Vector(Action.Close)
    Parser(Map(), "This is a bunch of intro text to skip! !event open !tank 1 !heal 1 !dps 2") shouldBe
      Vector(Action.Open(Vector(tank -> 1, heal -> 1, dps -> 2)))
  }

  "The parser" should "handle administrator actions" in {
    val mentions = Vector(amy, ann, ben, bob).map(m => s"@${m.id.toLowerCase}" -> m).toMap
    Parser(mentions, "!event assign @amy !tank @ann !heal") shouldBe
      Vector(Action.Assign(Vector(amy -> tank, ann -> heal)))
    Parser(mentions, "!event release @amy @ann") shouldBe
      Vector(Action.Release(Set(amy, ann)))
    Parser(mentions, "!event offer @ben !heal !dps") shouldBe
      Vector(Action.Offer(ben, Vector(heal, dps)))
    Parser(mentions, "!event kick @ben !heal !dps") shouldBe
      Vector(Action.Kick(ben, Vector(heal, dps)))
    Parser(mentions, "!event kick @bob") shouldBe
      Vector(Action.Kick(bob, Vector()))
  }

  "The parser" should "handle user actions" in {
    Parser(Map(), "!tank !heal") shouldBe
      Vector(Action.Volunteer(Vector(tank, heal)))
    Parser(Map(), "!dps !heal !tank") shouldBe
      Vector(Action.Volunteer(Vector(dps, heal, tank)))
    Parser(Map(), "!drop !dps !heal !tank") shouldBe
      Vector(Action.Drop(Vector(dps, heal, tank)))
    Parser(Map(), "!drop") shouldBe
      Vector(Action.Drop(Vector()))
    Parser(Map(), "!help") shouldBe
      Vector(Action.Help)
  }

  "The parser" should "handle garbage input" in {
    val mentions = Vector(amy, ann, ben, bob).map(m => s"@${m.id.toLowerCase}" -> m).toMap
    Parser(Map(), "!event befuddle") shouldBe Vector()
    Parser(Map(), "!event open") shouldBe Vector()
    Parser(Map(), "!event open !tank") shouldBe Vector()
    Parser(Map(), "!event open tank 2") shouldBe Vector()
    Parser(Map(), "!event open !tank befuddle") shouldBe Vector()
    Parser(Map(), "!event assign") shouldBe Vector()
    Parser(Map(), "!event assign @bob") shouldBe Vector()
    Parser(mentions, "!event assign @bob") shouldBe Vector()
    Parser(Map(), "!event release") shouldBe Vector()
    Parser(Map(), "!event release @bob") shouldBe Vector()
    Parser(Map(), "!event offer") shouldBe Vector()
    Parser(Map(), "!event offer @bob") shouldBe Vector()
    Parser(mentions, "!event offer @bob") shouldBe Vector()
    Parser(Map(), "!event kick") shouldBe Vector()
    Parser(Map(), "!event kick @bob") shouldBe Vector()
  }

}
