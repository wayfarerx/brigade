/*
 * MessageSpec.scala
 *
 * Copyright 2018 wayfarerx <x@wayfarerx.net> (@thewayfarerx)
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

import collection.JavaConverters._
import collection.immutable.ListMap

import java.util.StringTokenizer

import org.scalatest._

/**
 * Test case for message parsing.
 */
class MessageSpec extends FlatSpec with Matchers {

  behavior of "Message"

  val amy = User(1)
  val ann = User(2)
  val ben = User(3)
  val bob = User(4)

  val tank = Role("tank")
  val healer = Role("healer")
  val dps = Role("dps")

  it should "handle configuration" in {
    useMessage(amy, "Let's do a !brigade").extractConfiguration shouldBe
      Set(amy) -> None
    useMessage(amy, s"!brigade ${mention(ann)} ${mention(bob)}").extractConfiguration shouldBe
      Set(amy, ann, bob) -> None
    useMessage(amy, "!cycle").extractConfiguration shouldBe
      Set() -> Some(2)
    useMessage(amy, "!cycle 8").extractConfiguration shouldBe
      Set() -> Some(8)
  }

  it should "handle lifecycle commands" in {
    useMessage(amy, s"!open !tank 1 !healer 2 !dps 9").extractSubmission shouldBe
      Vector(Command.Open(ListMap(tank -> 1, healer -> 2, dps -> 9), None))
    useMessage(amy, s"!open !tank 1 !healer 2 !dps 4 !dps 5").extractSubmission shouldBe
      Vector(Command.Open(ListMap(tank -> 1, healer -> 2, dps -> 9), None))
    useMessage(amy, s"!open !tank ${mention(ann)}").extractSubmission shouldBe
      Vector(Command.Open(ListMap(tank -> 1), None))
    useMessage(amy, s"!open !tank count").extractSubmission shouldBe
      Vector(Command.Open(ListMap(tank -> 1), None))
    useMessage(amy, s"!open !tank !tank").extractSubmission shouldBe
      Vector(Command.Open(ListMap(tank -> 2), None))
    useMessage(amy, s"!abort").extractSubmission shouldBe
      Vector(Command.Abort)
    useMessage(amy, s"!close").extractSubmission shouldBe
      Vector(Command.Close)
    useMessage(amy, s"This is a bunch of intro text to skip! !open !tank 1 !healer 1 !dps 2").extractSubmission shouldBe
      Vector(Command.Open(ListMap(tank -> 1, healer -> 1, dps -> 2), None))
  }

  it should "handle administrator commands" in {
    useMessage(amy, s"!?").extractSubmission shouldBe
      Vector(Command.Query(amy))
    useMessage(amy, s"!? ${mention(bob)} ${mention(amy)}").extractSubmission shouldBe
      Vector(Command.Query(bob), Command.Query(amy))
    useMessage(amy, s"!assign ${mention(amy)} !tank ${mention(ann)} !healer").extractSubmission shouldBe
      Vector(Command.Assign(amy, tank), Command.Assign(ann, healer))
    useMessage(amy, s"!release ${mention(amy)} ${mention(ann)}").extractSubmission shouldBe
      Vector(Command.Release(amy), Command.Release(ann))
    useMessage(amy, s"!offer ${mention(ben)} !healer !dps").extractSubmission shouldBe
      Vector(Command.Volunteer(ben, healer), Command.Volunteer(ben, dps))
    useMessage(amy, s"!kick ${mention(ben)} !healer !dps").extractSubmission shouldBe
      Vector(Command.Drop(ben, healer), Command.Drop(ben, dps))
    useMessage(amy, s"!kick ${mention(bob)}").extractSubmission shouldBe
      Vector(Command.DropAll(bob))
  }

  it should "handle user commands" in {
    useMessage(amy, s"!tank !healer").extractSubmission shouldBe
      Vector(tank, healer).map(Command.Volunteer(amy, _))
    useMessage(amy, s"!dps !healer !tank").extractSubmission shouldBe
      Vector(dps, healer, tank).map(Command.Volunteer(amy, _))
    useMessage(amy, s"!drop !dps !healer !tank").extractSubmission shouldBe
      Vector(dps, healer, tank).map(Command.Drop(amy, _))
    useMessage(amy, s"!drop").extractSubmission shouldBe
      Vector(Command.DropAll(amy))
    useMessage(amy, s"!help").extractSubmission shouldBe
      Vector(Command.Help)
  }

  it should "handle garbage input" in {
    useMessage(amy, s"! befuddle").extractSubmission shouldBe Vector()
    useMessage(amy, s"!open").extractSubmission shouldBe Vector()
    useMessage(amy, s"!open tank 2").extractSubmission shouldBe Vector()
    useMessage(amy, s"!assign").extractSubmission shouldBe Vector()
    useMessage(amy, s"!assign ${mention(bob)}").extractSubmission shouldBe Vector()
    useMessage(amy, s"!assign ${mention(bob)}").extractSubmission shouldBe Vector()
    useMessage(amy, s"!release").extractSubmission shouldBe Vector()
    useMessage(amy, s"!offer").extractSubmission shouldBe Vector()
    useMessage(amy, s"!offer ${mention(bob)}").extractSubmission shouldBe Vector()
    useMessage(amy, s"!kick").extractSubmission shouldBe Vector()
  }

  private val MentionFormat = """\<\@([\d]+)\>""".r

  private def mention(user: User): String = s"<@${user.id}>"

  private def useMessage(author: User, string: String): Message = Message(
    Message.Id(0),
    author,
    new StringTokenizer(string).asScala.map(_.toString.trim).map {
      case MentionFormat(id) => Message.Mention(User(id.toLong))
      case word => Message.Word(word)
    }.toSeq: _*
  )

}
