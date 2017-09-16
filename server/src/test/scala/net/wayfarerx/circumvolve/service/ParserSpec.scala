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

  val amy = Member("1")
  val ann = Member("2")
  val ben = Member("3")
  val bob = Member("4")

  "The parser" should "handle lifecycle actions" in {
    Parser(s"!open !tank 1 !heal 2 !dps 9") shouldBe
      Vector(Action.Open(Vector(tank -> 1, heal -> 2, dps -> 9)))
    Parser(s"!abort") shouldBe
      Vector(Action.Abort)
    Parser(s"!close") shouldBe
      Vector(Action.Close)
    Parser(s"This is a bunch of intro text to skip! !open !tank 1 !heal 1 !dps 2") shouldBe
      Vector(Action.Open(Vector(tank -> 1, heal -> 1, dps -> 2)))
  }

  "The parser" should "handle administrator actions" in {
    Parser(s"!assign ${mention(amy)} !tank ${mention(ann)} !heal") shouldBe
      Vector(Action.Assign(Vector(amy -> tank, ann -> heal)))
    Parser(s"!release ${mention(amy)} ${mention(ann)}") shouldBe
      Vector(Action.Release(Set(amy, ann)))
    Parser(s"!offer ${mention(ben)} !heal !dps") shouldBe
      Vector(Action.Offer(ben, Vector(heal, dps)))
    Parser(s"!kick ${mention(ben)} !heal !dps") shouldBe
      Vector(Action.Kick(ben, Vector(heal, dps)))
    Parser(s"!kick ${mention(bob)}") shouldBe
      Vector(Action.Kick(bob, Vector()))
  }

  "The parser" should "handle user actions" in {
    Parser(s"!tank !heal") shouldBe
      Vector(Action.Volunteer(Vector(tank, heal)))
    Parser(s"!dps !heal !tank") shouldBe
      Vector(Action.Volunteer(Vector(dps, heal, tank)))
    Parser(s"!drop !dps !heal !tank") shouldBe
      Vector(Action.Drop(Vector(dps, heal, tank)))
    Parser(s"!drop") shouldBe
      Vector(Action.Drop(Vector()))
    Parser(s"!help") shouldBe
      Vector(Action.Help)
  }

  "The parser" should "handle garbage input" in {
    Parser(s"! befuddle") shouldBe Vector()
    Parser(s"!open") shouldBe Vector()
    Parser(s"!open !tank") shouldBe Vector()
    Parser(s"!open tank 2") shouldBe Vector()
    Parser(s"!open !tank ${mention(ann)}") shouldBe Vector()
    Parser(s"!open !tank befuddle") shouldBe Vector()
    Parser(s"!assign") shouldBe Vector()
    Parser(s"!assign ${mention(bob)}") shouldBe Vector()
    Parser(s"!assign ${mention(bob)}") shouldBe Vector()
    Parser(s"!release") shouldBe Vector()
    Parser(s"!offer") shouldBe Vector()
    Parser(s"!offer ${mention(bob)}") shouldBe Vector()
    Parser(s"!kick") shouldBe Vector()
  }

  def mention(member: Member): String =
    s"<@${member.id}>"

}
