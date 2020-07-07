package com.bugzmanov.mailiranitar

import com.bugzmanov.mailiranitar.mail.MailSystem
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
    mailConfig = MailSystemConfig (
      mailBoxTTL = 15 minutes,
      maxMailBoxSize = 30,
      messagesPageSize =  10,
      maxNumberOfMailBoxes = 200
    )
  )

  val mailSystem = new MailSystem(config.domainName, config.mailConfig)
}
