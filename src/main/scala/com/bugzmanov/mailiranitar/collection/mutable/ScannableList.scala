package com.bugzmanov.mailiranitar.collection.mutable

import java.util.UUID

import javax.annotation.concurrent.{NotThreadSafe, ThreadSafe}

import scala.collection.mutable

trait HasUUID[A] {
  def uuid(a: A): UUID
}

object GetUUID {
  def uuid[A](a: A)(implicit sh: HasUUID[A]): UUID = sh.uuid(a)
}


@NotThreadSafe
private[mutable] class Node[T](val value: T) {
  var next: Node[T] = _;
  var prev: Node[T] = _;
}

@NotThreadSafe
private[mutable] class LinkedList[T] {
  private var head: Node[T] = _
  private var tail: Node[T] = _

  def tailOpt: Option[Node[T]] = Option(tail)

  def headOpt: Option[Node[T]] = Option(head)

  // for testing
  def toVector: Vector[T] = {
    var items = Vector.newBuilder[T]
    for (node <- headOpt) {
      var iter = node
      while (iter != null) {
        items += iter.value
        iter = iter.next
      }
    }
    items.result()
  }

  // for testing
  def toVectorReverse: Vector[T] = {
    var items = Vector.newBuilder[T]
    for (node <- tailOpt) {
      var iter = node
      while (iter != null) {
        items += iter.value
        iter = iter.prev
      }
    }
    items.result()
  }

  def pollHead(): Option[Node[T]] = {
    if (head == null) {
      return None
    }

    if (tail == head) {
      val result = Some(head)
      head = null
      tail = null
      return result
    }

    val result = Some(head);
    this.head = head.next
    this.head.prev = null
    result
  }

  def addLast(t: T): Node[T] = {
    if (head == null) {
      val node = new Node(t)
      this.head = node;
      this.tail = node;
      return node;
    }

    val node = new Node(t)
    node.prev = this.tail
    this.tail.next = node
    this.tail = node
    node
  }

  def remove(node: Node[T]): Unit = {
    if (node == head && node == tail) {
      this.head = null
      this.tail = null
      return
    }

    if (node == head) {
      this.pollHead()
      return
    }

    if (node == tail) {
      this.tail = tail.prev
      this.tail.next = null
      return
    }

    node.prev.next = node.next
    node.next.prev = node.prev
  }
}


trait ScannableList[ID, T] {
  def addLast(t: T): Option[T]
  def get(id: ID): Option[T]
  def delete(id: ID): Option[T]
  def getItems(from: Option[ID], count: Int): Vector[T]
}

@NotThreadSafe
class ScannableLinkedList[T: HasUUID](maxSize: Int) extends ScannableList[UUID, T] {

  import GetUUID._

  assert(maxSize > 0)

  private val list = new LinkedList[T]();
  private val map = new mutable.HashMap[UUID, Node[T]]()

  def addLast(t: T): Option[T] = {
    val itemId = uuid(t)
    if (map.contains(itemId)) {
      return Some(map(itemId).value)
    }
    val node = list.addLast(t)
    map.put(itemId, node)

    while (map.size > maxSize) {
      val node = list.pollHead()
      assert(node.isDefined)

      map.remove(uuid(node.get.value))
    }

    Some(t)
  }

  def get(id: UUID): Option[T] = {
    map.get(id).map(_.value)
  }

  def delete(id: UUID): Option[T] = {
    for (node <- map.get(id)) yield {
      list.remove(node)
      map.remove(id)
      node.value
    }
  }

  def getItems(from: Option[UUID], count: Int): Vector[T] = {
    val nodeStart = from match {
      case Some(uuid) => map.get(uuid)
      case None => list.tailOpt
    }

    var items = Vector.newBuilder[T]
    for (node <- nodeStart) {
      var iter = node
      var currentCount = 0
      while (iter != null && currentCount < count) {
        items += iter.value
        currentCount += 1
        iter = iter.prev
      }
    }
    items.result()
  }
}

//TODO test this
@ThreadSafe
class GuardedScannableList[T: HasUUID](delegate: ScannableList[UUID, T]) extends ScannableList[UUID, T] {

  import java.util.concurrent.locks.StampedLock

  private val lock = new StampedLock

  @inline
  private def underReadLock[K](f: => K): K = {
    var stamp: Long = 0
    while (stamp == 0) {
      stamp = lock.tryReadLock()
    }
    try {
      f
    } finally {
      lock.unlock(stamp)
    }
  }

  @inline
  private def underWriteLock[K](f: => K): K = {
    var stamp: Long = 0
    while (stamp == 0) {
      stamp = lock.tryWriteLock()
    }
    try {
      f
    } finally {
      lock.unlock(stamp)
    }
  }

  override def addLast(t: T): Option[T] = {
    underWriteLock {
      delegate.addLast(t)
    }
  }

  override def get(id: UUID): Option[T] = {
    underReadLock {
      delegate.get(id)
    }
  }

  override def delete(id: UUID): Option[T] = {
    underWriteLock {
      delegate.delete(id)
    }
  }

  override def getItems(from: Option[UUID], count: Int): Vector[T] = {
    underReadLock {
      delegate.getItems(from, count)
    }
  }
}
