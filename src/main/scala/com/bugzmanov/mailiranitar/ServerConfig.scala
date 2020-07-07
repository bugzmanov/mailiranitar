package com.bugzmanov.mailiranitar

import scala.concurrent.duration.Duration

case class ServerConfig(domainName: String,
                        mailConfig: MailSystemConfig)

case class MailSystemConfig(mailBoxTTL: Duration,
                            maxMailBoxSize: Int,
                            messagesPageSize: Int,
                            maxNumberOfMailBoxes: Int)
