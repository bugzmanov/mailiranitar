package com.bugzmanov.mailiranitar.collection.mutable

import java.util.UUID

import org.scalatest.{FlatSpec, Matchers}

class ScannableListSpec extends FlatSpec with Matchers {

  case class SimpleId(name: String, id: UUID = UUID.randomUUID())

  implicit val simpleId2Uuid = new HasUUID[SimpleId] {
    override def uuid(a: SimpleId): UUID = a.id
  }

  it should "empty list" in {
    val list = new ScannableLinkedList[SimpleId](10)
    list.get(UUID.randomUUID()) should be (None)
  }

  it should "support adding elements and retrieving them" in {
    val list = new ScannableLinkedList[SimpleId](10)
    val item = new SimpleId("testing")
    list.addLast(item)

    list.get(item.id) should be (Some(item))
  }

  it should "support removing items" in {
    val list = new ScannableLinkedList[SimpleId](10)
    val item = new SimpleId("testing")
    list.addLast(item)

    list.get(item.id) shouldBe defined
    list.delete(item.id)

    list.get(item.id) should be(None)
  }

  it should "support getting items from id" in {
    val list = new ScannableLinkedList[SimpleId](10)
    list.addLast(SimpleId("1"))
    val item2 = list.addLast(SimpleId("2"))
    list.addLast(SimpleId("3"))

    list.getItems(Some(item2.get.id), 10).map(_.name) should be (Vector("2", "1"))
  }

  it should "support getting most recent items" in {
    val list = new ScannableLinkedList[SimpleId](10)
    for(i <- 1 to 5) {
      list.addLast(SimpleId(i.toString))
    }

    list.getItems(None, 3).map(_.name) should be (Vector("5", "4", "3"))
  }

  it should "drop oldest items after limit being reached" in {
    val list = new ScannableLinkedList[SimpleId](3)
    for(i <- 1 to 5) {
      list.addLast(SimpleId(i.toString))
    }

    list.getItems(None, 100).map(_.name) should be (Vector("5", "4", "3"))
  }

}
