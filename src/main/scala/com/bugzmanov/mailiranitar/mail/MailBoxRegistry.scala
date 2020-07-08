package com.bugzmanov.mailiranitar.mail

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

import com.bugzmanov.mailiranitar.MailSystemConfig
import com.github.benmanes.caffeine.cache.{Caffeine, Scheduler}
import org.apache.http.annotation.ThreadSafe


case class MailBoxStat(email: String,
                       expires: Instant)

/**
 * Central repository for all mailboxes in the system.
 * The repository is responsible for:
 * - generating new mailboxes
 * - providing access to existing mailboxes
 * - expiring existing mailboxes based on {@see MailSystemConfig#mailBoxTTL} and {@see MailSystemConfig#maxNumberOfMailBoxes}
 *
 * TTL of a mailbox is counted from the time of creation.
 *
 * When the amount of mailboxes are close to {@see MailSystemConfig#maxNumberOfMailBoxes} the registry drops entries
 * that are less likely to be used again
 *
 * @param domainName
 * @param mailConfig
 */
@ThreadSafe
class MailBoxRegistry(domainName: String, mailConfig: MailSystemConfig) {

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

  private def generateEmail(): String = s"${UUID.randomUUID().toString}@$domainName"
}
