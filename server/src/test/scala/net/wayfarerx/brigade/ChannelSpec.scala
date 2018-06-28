/*
 * ChannelSpec.scala
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

import collection.immutable.ListMap
import language.implicitConversions

import org.scalatest._

import akka.testkit.typed.scaladsl._

/**
 * Test case for the channel actor.
 */
class ChannelSpec extends FlatSpec with Matchers {

  behavior of "Channel"

  private val bob = User(1)
  private val sue = User(2)
  private val jim = User(3)
  private val kim = User(4)
  private val sam = User(5)

  private val tank = Role("tank")
  private val healer = Role("healer")
  private val dps = Role("dps")

  private implicit def stringToToken(str: String): Message.Token = Message.Word(str)

  private implicit def userToToken(user: User): Message.Token = Message.Mention(user)

  private implicit def intToMessageId(id: Int): Message.Id = Message.Id(id)

  it should "initialize in a disabled state and subsequently become enabled" in {
    val id = Channel.Id(1)
    val cfgMsgId = Message.Id(1)
    val openMsgId = Message.Id(2)
    val teamsMsgId = Message.Id(0)
    val outgoing = TestInbox[Event.Outgoing]()
    val tester = BehaviorTestKit(Channel(id, bob, outgoing.ref))
    val slots = ListMap(tank -> 1, healer -> 1, dps -> 2)
    val initialTeam = Team(ListMap(tank -> Vector(bob), healer -> Vector(sue), dps -> Vector(jim, kim)))
    // Ensure unexpected events are dropped while initializing.
    tester.run(Event.HistoryLoaded(History(Vector()), 1L))
    outgoing.hasMessages shouldBe false
    // Initialize the channel in a new state.
    tester.run(Event.Initialize(None, 1L))
    outgoing.expectMessage(Event.LoadMessages(id, 0, tester.selfInbox.ref, 1L))
    outgoing.hasMessages shouldBe false
    // Start it as disabled.
    tester.run(Event.MessagesLoaded(Event.Configure(Vector(), 2L), Vector(), 2L))
    outgoing.hasMessages shouldBe false
    // Enable the channel.
    tester.run(Event.Configure(Vector(Message(cfgMsgId, bob, "!brigade")), 3L))
    outgoing.expectMessage(Event.SaveSession(id, Brigade.Inactive(3L), 3L))
    outgoing.hasMessages shouldBe false
    // Open a roster and add to it before it fully opens.
    tester.run(Event.Submit(Message(openMsgId, bob, "!open", "!tank", "!healer", "!dps", "2", "!assign", bob, "!tank"), 4L))
    outgoing.expectMessage(Event.PrepareTeams(id, tester.selfInbox.ref, 4L))
    tester.run(Event.Submit(Message(4, sue, "!healer"), 6L))
    tester.run(Event.Submit(Message(5, jim, "!dps"), 7L))
    tester.run(Event.Submit(Message(6, kim, "!dps", "!tank"), 8L))
    outgoing.hasMessages shouldBe false
    // Ensure unexpected events are dropped while initializing.
    tester.run(Event.HistoryLoaded(History(Vector()), 1L))
    outgoing.hasMessages shouldBe false
    // Finish opening the roster.
    tester.run(Event.TeamsPrepared(Some(teamsMsgId), 9L))
    outgoing.expectMessage(Event.SaveSession(id, Brigade.Active(bob, openMsgId, Some(teamsMsgId), slots, Ledger(
      Ledger.Entry(openMsgId, bob, Command.Assign(bob, tank)),
      Ledger.Entry(4, sue, Command.Volunteer(sue, healer)),
      Ledger.Entry(5, jim, Command.Volunteer(jim, dps)),
      Ledger.Entry(6, kim, Command.Volunteer(kim, dps), Command.Volunteer(kim, tank))
    ), 9L), 9L))
    outgoing.expectMessage(Event.PostReplies(id, Vector(Reply.UpdateTeams(slots, teamsMsgId, Vector(initialTeam))), 9L))
    outgoing.hasMessages shouldBe false
    // Reconfigure the brigade to require a history.
    tester.run(Event.Configure(Vector(Message(cfgMsgId, bob, "!brigade", "!cycle")), 10L))
    outgoing.expectMessage(Event.LoadHistory(id, 2, tester.selfInbox().ref, 10L))
    outgoing.hasMessages shouldBe false
    // While reconfiguring we get a new healer.
    tester.run(Event.Submit(Message(7, sam, "!healer"), 11L))
    outgoing.hasMessages shouldBe false
    // Finish reconfiguring.
    tester.run(Event.HistoryLoaded(History(Vector(Vector(initialTeam), Vector(initialTeam))), 12L))
    outgoing.expectMessage(Event.SaveSession(id, Brigade.Active(bob, openMsgId, Some(teamsMsgId), slots, Ledger(
      Ledger.Entry(openMsgId, bob, Command.Assign(bob, tank)),
      Ledger.Entry(4, sue, Command.Volunteer(sue, healer)),
      Ledger.Entry(5, jim, Command.Volunteer(jim, dps)),
      Ledger.Entry(6, kim, Command.Volunteer(kim, dps), Command.Volunteer(kim, tank)),
      Ledger.Entry(7, sam, Command.Volunteer(sam, healer))
    ), 12L), 12L))
    outgoing.expectMessage(Event.PostReplies(id, Vector(Reply.UpdateTeams(slots, teamsMsgId, Vector(
      Team(ListMap(tank -> Vector(bob), healer -> Vector(sam), dps -> Vector(jim, kim))),
      Team(ListMap(tank -> Vector(), healer -> Vector(sue), dps -> Vector()))
    ))), 12L))
    outgoing.hasMessages shouldBe false
    // Ensure unexpected events are dropped while waiting.
    tester.run(Event.HistoryLoaded(History(Vector()), 13L))
    outgoing.hasMessages shouldBe false
  }

  it should "buffer unique signals sorted by timestamp" in {
    val config = Event.Configure(Vector(Message(1, bob, "!brigade")), 1L)
    val submit1 = Event.Submit(Message(3, bob, "!assign", bob, "!tank"), 3L)
    val submit2 = Event.Submit(Message(5, bob, "!assign", sue, "!healer"), 5L)
    val submit3 = Event.Submit(Message(2, bob, "!assign", jim, "!dps"), 2L)
    val submit4 = Event.Submit(Message(4, bob, "!assign", kim, "!dps"), 4L)
    val buffer1 = Channel.Buffer() :+ config
    buffer1 shouldBe Channel.Buffer(Some(config), Vector())
    buffer1 :+ Event.Configure(Vector(Message(0, bob)), 0L) shouldBe
      Channel.Buffer(Some(config), Vector())
    val buffer2 = buffer1 :+ submit1
    buffer2 shouldBe Channel.Buffer(Some(config), Vector(submit1))
    val buffer3 = buffer2 :+ submit2
    buffer3 shouldBe Channel.Buffer(Some(config), Vector(submit1, submit2))
    val buffer4 = buffer3 :+ submit3
    buffer4 shouldBe Channel.Buffer(Some(config), Vector(submit3, submit1, submit2))
    val buffer5 = buffer4 :+ submit4
    buffer5 shouldBe Channel.Buffer(Some(config), Vector(submit3, submit1, submit4, submit2))
    buffer5 :+ submit4 shouldBe buffer5
  }

}
