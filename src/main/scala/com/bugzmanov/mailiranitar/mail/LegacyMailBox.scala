package com.bugzmanov.mailiranitar.mail

import java.time.Instant
import java.util.UUID
import java.util.concurrent.{ConcurrentLinkedDeque, ConcurrentLinkedQueue}

import org.apache.http.annotation.ThreadSafe

import scala.collection.JavaConverters._


/**
 * Repository for messages stored for a mailbox. The mail box can store limited amout of mails.
 * After reaching this threshold, mailbox silently drops the oldest messages
 *
 * @param email mailbox email
 * @param created date-time when mailbox was created
 * @param maxSize the max amount of messages the mailbox can store,
 *
 * TODO: this class does 2 things: serves as data-structure and as mailbox. Potentially needs to be split
 * WARNING: class has a race condition
 * WARNING: this class has suboptimal performance characteristic for all operations (time complexity = O(n))
 */
@ThreadSafe
@Deprecated
class LegacyMailBox(val email: String,
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

    // TODO queue.size() is not constant
    while (queue.size() > maxSize) { //TODO: check-then-act race condition
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
}


