package com.bugzmanov.mailiranitar.mail

import java.time.Instant
import java.util.UUID

import com.bugzmanov.mailiranitar.mail.{LegacyMailBox, MailBox}
import org.scalatest.{FlatSpec, Matchers}

class MailBoxSpec extends FlatSpec with Matchers{


  it should "be possible to retrieve saved message" in {

    val mailBox = newMailBox()
    val mail = mailBox.add("ololo@ololoev.net", "subject", "body", Instant.now())

    val loaded = mailBox.get(mail.get.id)
    loaded shouldBe defined
    mail.get.body should be ("body")
    mail.get.subject should be ("subject")
    mail.get.sender should be ("ololo@ololoev.net")
  }

  it should "return None if message not found" in {
    val mailBox = newMailBox()

    mailBox.get(UUID.randomUUID()) should be (None)
  }

  it should "be possible to remove message" in {
    val mailBox = newMailBox()

    val mail = mailBox.add("ololo@ololoev.net", "subject", "body", Instant.now())
    mailBox.delete(mail.get.id)
    mailBox.get(mail.get.id) should be (None)
  }

  it should "return messages ordered by received date desc" in {
    val mailBox = newMailBox()

    mailBox.add("ololo1@ololoev.net", "subject", "body", Instant.now())
    mailBox.add("ololo2@ololoev.net", "subject", "body", Instant.now())
    mailBox.add("ololo3@ololoev.net", "subject", "body", Instant.now())

    val messages = mailBox.messages(None)
    messages.map(_.sender) should be (Vector("ololo3@ololoev.net", "ololo2@ololoev.net", "ololo1@ololoev.net"))
  }

  it should "be possible to specify from and count for message list" in {
    val mailBox = newMailBox()

    mailBox.add("ololo1@ololoev.net", "subject", "body", Instant.now())
    mailBox.add("ololo2@ololoev.net", "subject", "body", Instant.now())
    val message = mailBox.add("ololo3@ololoev.net", "subject", "body", Instant.now())
    mailBox.add("ololo4@ololoev.net", "subject", "body", Instant.now())

    val messages = mailBox.messages(Some(message.get.id), 2)
    messages.map(_.sender) should be (Vector("ololo3@ololoev.net", "ololo2@ololoev.net"))
  }

  it should "old drop old messages after maxMessages limit is reached" in {
    val mailBox = newMailBox()

    for (i <- 0 to 15) {
      mailBox.add(s"ololo_$i@ololoev.net", "subject", "body", Instant.now())
    }

    val result = mailBox.messages(None, 15)
    result should have size(10)
    result.last.sender should be ("ololo_6@ololoev.net")
  }

  private def newMailBox(): MailBox = new MailBox("test@test.com", Instant.now(), 10)
}
