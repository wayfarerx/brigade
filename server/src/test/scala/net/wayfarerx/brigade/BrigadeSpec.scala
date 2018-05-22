/*
 * BrigadeSpec.scala
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

import language.implicitConversions
import org.scalatest._

import scala.collection.immutable.ListMap

/**
 * Test case for the brigade model.
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

  it should "ignore submissions while disabled" in {
    val disabled = Brigade()
    disabled.submit(kim, Message.Id(1), Vector(Command.Help), 1L) shouldBe disabled -> Vector()
    disabled.submit(bob, Message.Id(2), Vector(Command.Open(ListMap(tank -> 1), Some(Message.Id(0)))), 2L) shouldBe
      disabled -> Vector()
    disabled.submit(bob, Message.Id(3), Vector(Command.Query(sue)), 3L) shouldBe disabled -> Vector()
    disabled.submit(bob, Message.Id(4), Vector(Command.Assign(bob, tank)), 4L) shouldBe disabled -> Vector()
    disabled.submit(bob, Message.Id(5), Vector(Command.Release(bob)), 5L) shouldBe disabled -> Vector()
    disabled.submit(sue, Message.Id(6), Vector(Command.Volunteer(sue, tank)), 6L) shouldBe disabled -> Vector()
    disabled.submit(bob, Message.Id(7), Vector(Command.Drop(bob, healer)), 7L) shouldBe disabled -> Vector()
    disabled.submit(bob, Message.Id(8), Vector(Command.DropAll(jim)), 8L) shouldBe disabled -> Vector()
    disabled.submit(bob, Message.Id(9), Vector(Command.Abort), 9L) shouldBe disabled -> Vector()
    disabled.submit(bob, Message.Id(10), Vector(Command.Close), 10L) shouldBe disabled -> Vector()
  }

  it should "respond to configure requests when disabled" in {
    Brigade().configure(Set(bob), Configuration.Default, 1L) shouldBe
      Brigade(Set(bob), Configuration.Default, Brigade.Inactive(1L)) -> Vector()
  }

  it should "ignore most commands while inactive" in {
    val inactive = Brigade().configure(Set(bob), Configuration.Default, 0L)._1
    inactive.submit(kim, Message.Id(1), Vector(Command.Help), 1L) shouldBe inactive -> Vector(Reply.Usage)
    inactive.submit(kim, Message.Id(2), Vector(Command.Open(ListMap(tank -> 1), Some(Message.Id(0)))), 2L) shouldBe
      inactive -> Vector()
    inactive.submit(bob, Message.Id(3), Vector(Command.Query(sue)), 3L) shouldBe inactive -> Vector()
    inactive.submit(bob, Message.Id(4), Vector(Command.Assign(bob, tank)), 4L) shouldBe inactive -> Vector()
    inactive.submit(bob, Message.Id(5), Vector(Command.Release(bob)), 5L) shouldBe inactive -> Vector()
    inactive.submit(sue, Message.Id(6), Vector(Command.Volunteer(sue, tank)), 6L) shouldBe inactive -> Vector()
    inactive.submit(bob, Message.Id(7), Vector(Command.Drop(bob, healer)), 7L) shouldBe inactive -> Vector()
    inactive.submit(bob, Message.Id(8), Vector(Command.DropAll(jim)), 8L) shouldBe inactive -> Vector()
    inactive.submit(bob, Message.Id(9), Vector(Command.Abort), 9L) shouldBe inactive -> Vector()
    inactive.submit(bob, Message.Id(10), Vector(Command.Close), 10L) shouldBe inactive -> Vector()
  }

  it should "build ledgers and teams from sequences of submissions" in {
    val brigade = (Brigade().configure(Set(bob), Configuration.Default, 0L)._1 /: Vector(
      (bob, Message.Id(1), Vector(Command.Open(ListMap(tank -> 1, healer -> 1, dps -> 2), Some(Message.Id(0)))), 1L),
      (bob, Message.Id(2), Vector(Command.Assign(bob, tank)), 2L),
      (bob, Message.Id(3), Vector(Command.Assign(sue, healer)), 3L),
      (bob, Message.Id(4), Vector(Command.Volunteer(bob, healer), Command.Volunteer(bob, dps)), 4L),
      (sue, Message.Id(5), Vector(Command.Volunteer(sue, tank)), 5L),
      (jim, Message.Id(6), Vector(Command.Volunteer(jim, dps)), 6L),
      (kim, Message.Id(7), Vector(Command.Volunteer(kim, healer), Command.Volunteer(kim, dps)), 7L),
      (bob, Message.Id(8), Vector(Command.Release(bob)), 8L),
      (bob, Message.Id(9), Vector(Command.Drop(bob, healer)), 9L),
      (jim, Message.Id(10), Vector(Command.DropAll(jim), Command.Volunteer(jim, tank)), 10L)
    )) { (previous, submission) =>
      val (next, replies) = (previous.submit _).tupled(submission)
      Reply.normalize(replies).collect { case reply@Reply.UpdateTeams(_, _) => reply }.length shouldBe 1
      next
    }
    brigade shouldBe Brigade(Set(bob), Configuration.Default, Brigade.Active(
      bob,
      Message.Id(1),
      Some(Message.Id(0)),
      ListMap(tank -> 1, healer -> 1, dps -> 2),
      Ledger(
        Ledger.Entry(Message.Id(2), bob, Command.Assign(bob, tank)),
        Ledger.Entry(Message.Id(3), bob, Command.Assign(sue, healer)),
        Ledger.Entry(Message.Id(4), bob, Command.Volunteer(bob, healer), Command.Volunteer(bob, dps)),
        Ledger.Entry(Message.Id(5), sue, Command.Volunteer(sue, tank)),
        Ledger.Entry(Message.Id(6), jim, Command.Volunteer(jim, dps)),
        Ledger.Entry(Message.Id(7), kim, Command.Volunteer(kim, healer), Command.Volunteer(kim, dps)),
        Ledger.Entry(Message.Id(8), bob, Command.Release(bob)),
        Ledger.Entry(Message.Id(9), bob, Command.Drop(bob, healer)),
        Ledger.Entry(Message.Id(10), jim, Command.DropAll(jim), Command.Volunteer(jim, tank))
      ),
      10L
    ))
    val (next, replies) = brigade.submit(bob, Message.Id(11), Vector(Command.Close), 11L)
    next shouldBe Brigade(Set(bob), Configuration.Default, Brigade.Inactive(11L))
    replies shouldBe Vector(Reply.FinalizeTeams(Message.Id(0), Vector(Team(ListMap(
      tank -> Vector(jim),
      healer -> Vector(sue),
      dps -> Vector(bob, kim)
    )))))
  }

  it should "build ledgers and teams from sequences of submissions with edits" in {
    val brigade = (Brigade().configure(Set(bob), Configuration.Default, 0L)._1 /: Vector(
      (bob, Message.Id(1), Vector(Command.Open(ListMap(tank -> 1, healer -> 1, dps -> 2), Some(Message.Id(0)))), 1L),
      (bob, Message.Id(2), Vector(Command.Assign(bob, tank)), 2L),
      (bob, Message.Id(3), Vector(Command.Assign(sue, healer)), 3L),
      (bob, Message.Id(4), Vector(Command.Volunteer(bob, healer), Command.Volunteer(bob, dps)), 4L),
      (sue, Message.Id(5), Vector(Command.Volunteer(sue, tank)), 5L),
      (jim, Message.Id(6), Vector(Command.Volunteer(jim, dps), Command.Volunteer(jim, tank)), 6L),
      (kim, Message.Id(7), Vector(Command.Volunteer(kim, healer), Command.Volunteer(kim, dps)), 7L),
      (bob, Message.Id(2), Vector(Command.Assign(bob, dps)), 8L),
      (bob, Message.Id(4), Vector(Command.Volunteer(bob, tank), Command.Volunteer(bob, dps)), 9L)
    )) { (previous, submission) =>
      val (next, replies) = (previous.submit _).tupled(submission)
      Reply.normalize(replies).collect { case reply@Reply.UpdateTeams(_, _) => reply }.length shouldBe 1
      next
    }
    brigade shouldBe Brigade(Set(bob), Configuration.Default, Brigade.Active(
      bob,
      Message.Id(1),
      Some(Message.Id(0)),
      ListMap(tank -> 1, healer -> 1, dps -> 2),
      Ledger(
        Ledger.Entry(Message.Id(2), bob, Command.Assign(bob, tank)),
        Ledger.Entry(Message.Id(3), bob, Command.Assign(sue, healer)),
        Ledger.Entry(Message.Id(4), bob, Command.Volunteer(bob, healer), Command.Volunteer(bob, dps)),
        Ledger.Entry(Message.Id(5), sue, Command.Volunteer(sue, tank)),
        Ledger.Entry(Message.Id(6), jim, Command.Volunteer(jim, dps), Command.Volunteer(jim, tank)),
        Ledger.Entry(Message.Id(7), kim, Command.Volunteer(kim, healer), Command.Volunteer(kim, dps)),
        Ledger.Entry(Message.Id(2), bob, Command.Assign(bob, dps)),
        Ledger.Entry(Message.Id(4), bob, Command.Volunteer(bob, tank), Command.Volunteer(bob, dps))
      ),
      9L
    ))
    val (next, replies) = brigade.submit(bob, Message.Id(8), Vector(Command.Close), 10L)
    next shouldBe Brigade(Set(bob), Configuration.Default, Brigade.Inactive(10L))
    replies shouldBe Vector(Reply.FinalizeTeams(Message.Id(0), Vector(Team(ListMap(
      tank -> Vector(jim),
      healer -> Vector(sue),
      dps -> Vector(bob, kim)
    )))))
  }

  it should "build teams after editing the open message" in {
    val (brigade, replies) = (Brigade().configure(Set(bob), Configuration.Default, 0L)._1 /: Vector(
      (bob, Message.Id(1), Vector(Command.Open(ListMap(tank -> 1, healer -> 1, dps -> 1), Some(Message.Id(0)))), 1L),
      (bob, Message.Id(2), Vector(Command.Assign(bob, tank)), 2L),
      (bob, Message.Id(3), Vector(Command.Assign(sue, healer)), 3L),
      (bob, Message.Id(4), Vector(Command.Volunteer(bob, healer), Command.Volunteer(bob, dps)), 4L),
      (sue, Message.Id(5), Vector(Command.Volunteer(sue, tank)), 5L),
      (jim, Message.Id(6), Vector(Command.Volunteer(jim, dps), Command.Volunteer(jim, tank)), 6L),
      (kim, Message.Id(7), Vector(Command.Volunteer(kim, healer), Command.Volunteer(kim, dps)), 7L),
      (bob, Message.Id(2), Vector(Command.Assign(bob, dps)), 8L),
      (bob, Message.Id(4), Vector(Command.Volunteer(bob, tank), Command.Volunteer(bob, dps)), 9L)
    )) { (previous, submission) =>
      val (next, replies) = (previous.submit _).tupled(submission)
      Reply.normalize(replies).collect { case reply@Reply.UpdateTeams(_, _) => reply }.length shouldBe 1
      next
    }.submit(
      bob,
      Message.Id(1),
      Vector(Command.Open(ListMap(tank -> 1, healer -> 1, dps -> 2), Some(Message.Id(0)))),
      10L
    )
    replies shouldBe Vector(Reply.UpdateTeams(
      Message.Id(0),
      Vector(Team(ListMap(
        tank -> Vector(jim),
        healer -> Vector(sue),
        dps -> Vector(bob, kim)
      )))
    ))
    brigade.submit(bob, Message.Id(8), Vector(
      Command.Open(ListMap(tank -> 1), Some(Message.Id(9))),
      Command.Help,
      Command.Abort
    ), 10L)._2 shouldBe Vector(Reply.Usage, Reply.AbandonTeams(Message.Id(0)))
  }

  it should "reply to queries" in {
    val brigade = Brigade().configure(Set(bob), Configuration.Default, 0L)._1
    val (next, replies) = brigade.submit(
      bob,
      Message.Id(1),
      Vector(
        Command.Open(ListMap(tank -> 1, healer -> 1, dps -> 2), Some(Message.Id(0))),
        Command.Assign(bob, tank),
        Command.Assign(sue, healer),
        Command.Volunteer(jim, dps), Command.Volunteer(jim, tank),
        Command.Volunteer(kim, healer), Command.Volunteer(kim, dps),
        Command.Query(sue),
        Command.Query(kim),
        Command.Close
      ),
      1L)
    next shouldBe brigade.copy(session = Brigade.Inactive(1L))
    Reply.normalize(replies) shouldBe Vector(
      Reply.Status(sue, Vector(healer), Vector()),
      Reply.Status(kim, Vector(), Vector(healer, dps)),
      Reply.FinalizeTeams(
        Message.Id(0),
        Vector(Team(ListMap(
          tank -> Vector(bob),
          healer -> Vector(sue),
          dps -> Vector(jim, kim)
        )))
      )
    )
  }

  it should "retain active rosters when reconfigured with the appropriate organizers and abandon otherwise" in {
    val brigade = (Brigade().configure(Set(bob), Configuration.Default, 0L)._1 /: Vector(
      (bob, Message.Id(1), Vector(Command.Open(ListMap(tank -> 1, healer -> 1, dps -> 2), Some(Message.Id(0)))), 1L),
      (bob, Message.Id(2), Vector(Command.Assign(bob, tank)), 2L)
    )) { (previous, submission) =>
      val (next, replies) = (previous.submit _).tupled(submission)
      Reply.normalize(replies).collect { case reply@Reply.UpdateTeams(_, _) => reply }.length shouldBe 1
      next
    }
    brigade shouldBe Brigade(Set(bob), Configuration.Default, Brigade.Active(
      bob,
      Message.Id(1),
      Some(Message.Id(0)),
      ListMap(tank -> 1, healer -> 1, dps -> 2),
      Ledger(
        Ledger.Entry(Message.Id(2), bob, Command.Assign(bob, tank))
      ),
      2L
    ))
    val (brigade2, replies2) = brigade.configure(Set(bob, sue), Configuration.Default, 3L)
    brigade2 shouldBe Brigade(Set(bob, sue), Configuration.Default, Brigade.Active(
      bob,
      Message.Id(1),
      Some(Message.Id(0)),
      ListMap(tank -> 1, healer -> 1, dps -> 2),
      Ledger(
        Ledger.Entry(Message.Id(2), bob, Command.Assign(bob, tank))
      ),
      3L
    ))
    replies2 shouldBe Vector(Reply.UpdateTeams(
      Message.Id(0),
      Vector(Team(ListMap(tank -> Vector(bob), healer -> Vector(), dps -> Vector())))
    ))
    val (brigade3, replies3) = brigade2.configure(Set(sue), Configuration.Default, 4L)
    brigade3 shouldBe Brigade(Set(sue), Configuration.Default, Brigade.Inactive(4L))
    replies3 shouldBe Vector(Reply.AbandonTeams(Message.Id(0)))
  }

  it should "support reconfiguring history usage" in {
    val brigade = Brigade().configure(Set(bob), Configuration.Default, 0L)._1
    val (next, firstReplies) = brigade.submit(
      bob,
      Message.Id(1),
      Vector(
        Command.Open(ListMap(tank -> 1, healer -> 1, dps -> 2), Some(Message.Id(0))),
        Command.Volunteer(bob, tank), Command.Volunteer(bob, dps),
        Command.Volunteer(sue, healer),
        Command.Volunteer(jim, tank), Command.Volunteer(jim, dps),
        Command.Volunteer(kim, healer), Command.Volunteer(kim, dps)
      ),
      1L)
    Reply.normalize(firstReplies) shouldBe Vector(
      Reply.UpdateTeams(
        Message.Id(0),
        Vector(Team(ListMap(
          tank -> Vector(bob),
          healer -> Vector(sue),
          dps -> Vector(jim, kim)
        )))
      )
    )
    val (_, lastReplies) = next.configure(
      Set(bob),
      Configuration.Cycle(History(Vector(Vector(Team(ListMap(
        tank -> Vector(bob),
        healer -> Vector(sue),
        dps -> Vector(jim, kim)
      )))))),
      2L)
    Reply.normalize(lastReplies) shouldBe Vector(
      Reply.UpdateTeams(
        Message.Id(0),
        Vector(Team(ListMap(
          tank -> Vector(jim),
          healer -> Vector(sue),
          dps -> Vector(bob, kim)
        )))
      )
    )
  }

}
