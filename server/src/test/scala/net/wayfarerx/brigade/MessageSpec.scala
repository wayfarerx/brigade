/*
 * MessageSpec.scala
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

import java.time.Instant

import collection.JavaConverters._
import java.util.StringTokenizer

import org.scalatest._

import scala.collection.immutable.ListMap

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

  it should "handle lifecycle commands" in {
    useMessage(amy, s"!open !tank 1 !healer 2 !dps 9").commands shouldBe
      Vector(Command.Open(ListMap(tank -> 1, healer -> 2, dps -> 9)))
    useMessage(amy, s"!abort").commands shouldBe
      Vector(Command.Abort)
    useMessage(amy, s"!close").commands shouldBe
      Vector(Command.Close)
    useMessage(amy, s"This is a bunch of intro text to skip! !open !tank 1 !healer 1 !dps 2").commands shouldBe
      Vector(Command.Open(ListMap(tank -> 1, healer -> 1, dps -> 2)))
  }

  it should "handle administrator commands" in {
    useMessage(amy, s"!assign ${mention(amy)} !tank ${mention(ann)} !healer").commands shouldBe
      Vector(Command.Assign(amy, tank), Command.Assign(ann, healer))
    useMessage(amy, s"!release ${mention(amy)} ${mention(ann)}").commands shouldBe
      Vector(Command.Release(amy), Command.Release(ann))
    useMessage(amy, s"!offer ${mention(ben)} !healer !dps").commands shouldBe
      Vector(Command.Volunteer(ben, healer), Command.Volunteer(ben, dps))
    useMessage(amy, s"!kick ${mention(ben)} !healer !dps").commands shouldBe
      Vector(Command.Drop(ben, healer), Command.Drop(ben, dps))
    useMessage(amy, s"!kick ${mention(bob)}").commands shouldBe
      Vector(Command.DropAll(bob))
  }

  it should "handle user commands" in {
    useMessage(amy, s"!tank !healer").commands shouldBe
      Vector(tank, healer).map(Command.Volunteer(amy, _))
    useMessage(amy, s"!dps !healer !tank").commands shouldBe
      Vector(dps, healer, tank).map(Command.Volunteer(amy, _))
    useMessage(amy, s"!drop !dps !healer !tank").commands shouldBe
      Vector(dps, healer, tank).map(Command.Drop(amy, _))
    useMessage(amy, s"!drop").commands shouldBe
      Vector(Command.DropAll(amy))
    useMessage(amy, s"!help").commands shouldBe
      Vector(Command.Help)
  }

  it should "handle garbage input" in {
    useMessage(amy, s"! befuddle").commands shouldBe Vector()
    useMessage(amy, s"!open").commands shouldBe Vector()
    useMessage(amy, s"!open tank 2").commands shouldBe Vector()
    useMessage(amy, s"!assign").commands shouldBe Vector()
    useMessage(amy, s"!assign ${mention(bob)}").commands shouldBe Vector()
    useMessage(amy, s"!assign ${mention(bob)}").commands shouldBe Vector()
    useMessage(amy, s"!release").commands shouldBe Vector()
    useMessage(amy, s"!offer").commands shouldBe Vector()
    useMessage(amy, s"!offer ${mention(bob)}").commands shouldBe Vector()
    useMessage(amy, s"!kick").commands shouldBe Vector()
  }

  private val MentionFormat = """\<\@([\d]+)\>""".r

  private def mention(user: User): String = s"<@${user.id}>"

  private def useMessage(author: User, string: String): Message = Message(
    Message.Id(0),
    System.currentTimeMillis,
    author,
    new StringTokenizer(string).asScala.map(_.toString.trim).map {
      case MentionFormat(id) => Message.Mention(User(id.toLong))
      case word => Message.Word(word)
    }.toVector
  )

}
