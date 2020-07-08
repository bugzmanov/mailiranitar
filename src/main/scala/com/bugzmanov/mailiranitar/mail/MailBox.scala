package com.bugzmanov.mailiranitar.mail

import java.time.Instant
import java.util.UUID

import com.bugzmanov.mailiranitar.collection.mutable.{GuardedScannableList, HasUUID, ScannableLinkedList}
import org.apache.http.annotation.ThreadSafe

/**
 * Repository for messages stored for a mailbox. The mail box can store limited amount of mails.
 * After reaching this threshold, mailbox silently drops the oldest messages
 *
 * @param email   mailbox email
 * @param created date-time when mailbox was created
 * @param maxSize the max amount of messages the mailbox can store,
 */
@ThreadSafe
class MailBox(val email: String,
              val created: Instant,
              maxSize: Int) {

  private implicit val message2Uuid = new HasUUID[Message] {
    override def uuid(a: Message): UUID = a.id
  }

  val scannableList = new GuardedScannableList[Message](new ScannableLinkedList[Message](maxSize))

  def messages(from: Option[UUID] = None, count: Int = 10): Vector[Message] = {
    scannableList.getItems(from, count)
  }

  def delete(uuid: UUID): Option[Message] = {
    scannableList.delete(uuid)
  }

  def add(sender: String, subject: String, body: String, sent: Instant): Option[Message] = {
    val message = Message(UUID.randomUUID(),
      sender = sender,
      subject = subject,
      body = body,
      received = Instant.now(),
      sent = sent)

    scannableList.addLast(message)

    Some(message)
  }

  def get(uuid: UUID): Option[Message] = {
    scannableList.get(uuid)
  }
}
