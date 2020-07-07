package com.bugzmanov.mailiranitar.mail

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import com.bugzmanov.mailiranitar.MailSystemConfig
import com.github.benmanes.caffeine.cache.{Caffeine, Scheduler}


case class MailBoxStat(email: String,
                       expires: Instant)

class MailSystem(domainName: String, mailConfig: MailSystemConfig) {

  private val cache = Caffeine.newBuilder()
    .scheduler(Scheduler.systemScheduler())
    .expireAfterWrite(mailConfig.mailBoxTTL.toMinutes, TimeUnit.MINUTES)
    .maximumSize(mailConfig.maxNumberOfMailBoxes)
    .build[String, MailBox]()

  def generateMailBox(): Option[MailBoxStat] = {
    val email = generateEmail()

    val box = new MailBox(email, Instant.now(), mailConfig.maxMailBoxSize)
    cache.put(email, box)
    Some(MailBoxStat(email, box.created))
  }

  def getMailBox(email: String): Option[MailBox] = {
    Option(cache.getIfPresent(email))
  }

  def deleteBox(email: String): Option[MailBoxStat] = {
    for {box <- getMailBox(email)} yield {
      cache.invalidate(email)
      MailBoxStat(box.email, box.created)
    }
  }

  //todo: check if uuid valid for emails
  def generateEmail(): String = s"${UUID.randomUUID().toString}@$domainName"
}
