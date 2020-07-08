package com.bugzmanov.mailiranitar.collection.mutable

import org.scalatest.{FlatSpec, Matchers}

class PrivateLinkedListSpec extends FlatSpec with Matchers {

  it should  "empty list has no head and no tail" in {
    val list = new LinkedList[Int]
    list.headOpt should be (None)
    list.tailOpt should be (None)
  }

  it should "singleton list should have tail == head" in {
    val list = new LinkedList[Int]
    list.addLast(1)

    list.headOpt should be(list.tailOpt)
  }

  it should "traverse from head should give list in order of addition" in {
    val list = new LinkedList[Int]
    list.addLast(1)
    list.addLast(2)
    list.addLast(3)

    list.toVector should be(Vector(1,2,3))
  }

  it should "traverse from tail should give list in order of addition in reverse" in {
    val list = new LinkedList[Int]
    list.addLast(1)
    list.addLast(2)
    list.addLast(3)

    list.toVectorReverse should be(Vector(3,2,1))
  }


  it should "support removing from the middle" in {
    val list = new LinkedList[Int]
    list.addLast(1)
    val node = list.addLast(2)
    list.addLast(3)

    list.remove(node)
    list.toVector should be(Vector(1,3))
    list.toVectorReverse should be(Vector(3,1))
  }

  it should "support removing from the end" in {
    val list = new LinkedList[Int]
    list.addLast(1)
    list.addLast(2)
    val node = list.addLast(3)

    list.remove(node)
    list.toVector should be(Vector(1,2))
    list.toVectorReverse should be(Vector(2,1))
  }

  it should "support removing from the head" in {
    val list = new LinkedList[Int]
    val node = list.addLast(1)
    list.addLast(2)
    list.addLast(3)

    list.remove(node)
    list.toVector should be(Vector(2,3))
    list.toVectorReverse should be(Vector(3,2))
  }

}
