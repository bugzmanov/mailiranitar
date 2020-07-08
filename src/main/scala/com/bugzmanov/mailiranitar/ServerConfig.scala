package com.bugzmanov.mailiranitar

import scala.concurrent.duration.Duration

/**
 *
 * @param domainName domain name of the mail server
 * @param mailConfig configuration of the mail system
 */
case class ServerConfig(domainName: String,
                        mailConfig: MailSystemConfig)

/**
 *
 * @param mailBoxTTL           how long will a mailbox be kept on the server after creation
 * @param maxMailBoxSize       maximum amount of messages each mailbox can keep
 * @param messagesPageSize     page size of messages being requested from a mailbox
 * @param maxNumberOfMailBoxes how many live mailboxes can be kept of the server at the same time
 */
case class MailSystemConfig(mailBoxTTL: Duration,
                            maxMailBoxSize: Int,
                            messagesPageSize: Int,
                            maxNumberOfMailBoxes: Int)
