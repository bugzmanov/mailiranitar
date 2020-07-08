package com.bugzmanov.mailiranitar

import com.bugzmanov.mailiranitar.mail.MailBoxRegistry
import com.bugzmanov.mailiranitar.web.MailiranitarServer

//https://bulbapedia.bulbagarden.net/wiki/Tyranitar_(Pok%C3%A9mon)
object StandaloneApp extends MailiranitarServer(
  config = Context.config,
  mailSystem = Context.mailSystem
) {

}

object Context {

  import scala.concurrent.duration._

  val config = ServerConfig(
    domainName = "ololo.com",
    mailConfig = MailSystemConfig(
      mailBoxTTL = 15 minutes,
      maxMailBoxSize = 50,
      messagesPageSize = 10,
      maxNumberOfMailBoxes = 5000
    )
  )

  val mailSystem = new MailBoxRegistry(config.domainName, config.mailConfig)
}
