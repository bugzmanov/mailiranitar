package com.bugzmanov.mailiranitar.mail

import java.time.Instant
import java.util.UUID
import java.util.concurrent.{ConcurrentLinkedDeque, ConcurrentLinkedQueue}

import scala.collection.JavaConverters._

case class Message private(id: UUID,
                           sender: String,
                           subject: String,
                           body: String,
                           received: Instant,
                           sent: Instant,
                          ) {
  override def hashCode(): Int = id.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case other: Message if other.id == this.id => true
    case _ => false
  }
}

class MailBox(val email: String,
              val created: Instant,
              maxSize: Int) {

  val queue = new ConcurrentLinkedDeque[Message]()

  def messages(from: Option[UUID] = None, count: Int = 10): Vector[Message] = {
    val iter = queue.descendingIterator().asScala

    from match {
      case None =>
        iter.take(count).toVector
      case Some(id) =>
        iter.dropWhile(m => m.id != id).take(count).toVector
    }
  }

  def delete(uuid: UUID): Option[Message] = {
    val iter = queue.iterator()
    while (iter.hasNext) {
      val next = iter.next()
      if (next.id == uuid) {
        val response = Some(next)
        iter.remove()
        return response
      }
    }
    None
  }

  def add(sender: String, subject: String, body: String, sent: Instant): Option[Message] = {
    val message = Message(UUID.randomUUID(),
      sender = sender,
      subject = subject,
      body = body,
      received = Instant.now(),
      sent = sent)

    queue.offer(message)

    while (queue.size() > maxSize) {
      queue.poll()
    }

    Some(message)
  }

  def get(uuid: UUID): Option[Message] = {
    val iter = queue.iterator().asScala.dropWhile(m => m.id != uuid)
    if (iter.hasNext) {
      Some(iter.next())
    } else {
      None
    }
  }

  def size: Int = queue.size()
}


