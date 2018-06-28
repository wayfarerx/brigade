/*
 * ReplySpec.scala
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

import org.scalatest._

import scala.collection.immutable.ListMap

/**
 * Test case for the reply normalizer.
 */
class ReplySpec extends FlatSpec with Matchers {

  behavior of "Reply"

  private val bob = User(1)
  private val sue = User(2)
  private val jim = User(3)
  private val kim = User(4)

  private val tank = Role("tank")
  private val healer = Role("healer")
  private val dps = Role("dps")

  it should "normalize away all but the first usage reply" in {
    Reply.normalize(Vector(
      Reply.Usage,
      Reply.Status(bob, Vector(tank), Vector(dps)),
      Reply.Usage,
      Reply.Status(sue, Vector(healer), Vector(dps)),
      Reply.Usage
    )) shouldBe Vector(
      Reply.Usage,
      Reply.Status(bob, Vector(tank), Vector(dps)),
      Reply.Status(sue, Vector(healer), Vector(dps))
    )
  }

  it should "normalize away all but the last status reply for each user" in {
    Reply.normalize(Vector(
      Reply.Status(bob, Vector(tank), Vector(dps)),
      Reply.Status(sue, Vector(healer), Vector(dps)),
      Reply.Usage,
      Reply.Status(sue, Vector(tank), Vector(dps)),
      Reply.Status(bob, Vector(healer), Vector(dps))
    )) shouldBe Vector(
      Reply.Usage,
      Reply.Status(sue, Vector(tank), Vector(dps)),
      Reply.Status(bob, Vector(healer), Vector(dps))
    )
  }

  it should "normalize away all but the last team reply" in {
    val slots = ListMap(tank -> 1, healer -> 1, dps -> 2)
    Reply.normalize(Vector(
      Reply.Status(bob, Vector(tank), Vector(dps)),
      Reply.UpdateTeams(slots, Message.Id(0), Vector(Team())),
      Reply.Status(sue, Vector(healer), Vector(dps)),
      Reply.FinalizeTeams(slots, Message.Id(0), Vector(Team())),
      Reply.Usage,
      Reply.AbandonTeams(Message.Id(0))
    )) shouldBe Vector(
      Reply.Status(bob, Vector(tank), Vector(dps)),
      Reply.Status(sue, Vector(healer), Vector(dps)),
      Reply.Usage,
      Reply.AbandonTeams(Message.Id(0))
    )
  }

}
