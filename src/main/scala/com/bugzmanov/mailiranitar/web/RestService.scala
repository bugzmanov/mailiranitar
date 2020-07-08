package com.bugzmanov.mailiranitar.web

import java.time.Instant
import java.util.UUID

import com.bugzmanov.mailiranitar.ServerConfig
import com.bugzmanov.mailiranitar.mail.MailBoxRegistry
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.google.inject.Module
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.annotations.RouteParam
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.http.{Controller, HttpServer}
import com.twitter.finatra.jackson.modules.ScalaObjectMapperModule

private object CustomJacksonModule extends ScalaObjectMapperModule {
  override def additionalMapperConfiguration(mapper: ObjectMapper): Unit = {
    mapper.registerModule(new JavaTimeModule())
  }
}

class MailiranitarServer(config: ServerConfig,
                         mailSystem: MailBoxRegistry
                        ) extends HttpServer {
  override def jacksonModule: Module = CustomJacksonModule

  override protected def configureHttp(router: HttpRouter) {
    router.add(new MailRestController(mailSystem))
  }
}

case class PostMessage(@RouteParam("email") recipient: String,
                       @JsonProperty sender: String,
                       @JsonProperty subject: String,
                       @JsonProperty sent: Instant,
                       @JsonProperty body: String)

case class PageLink(next: Option[String])

case class PageIter[T](data: Vector[T], links: PageLink)

case class DeleteMessageResponse(id: UUID, success: Boolean)


class MailRestController(mailSystem: MailBoxRegistry) extends Controller {

  post(route = "/mailboxes") { request: Request =>
    mailSystem.generateMailBox()
  }

  delete(route = "/mailboxes/:email") { request: Request =>
    val email = request.params("email")
    mailSystem.deleteBox(email)
  }

  post(route = "/mailboxes/:email/messages") { message: PostMessage =>
    for {box <- mailSystem.getMailBox(message.recipient)} yield {
      box.add(message.sender, message.subject, message.body, message.sent)
    }
  }

  get(route = "/mailboxes/:email/messages/:message_id") { request: Request =>
    val email = request.params("email")
    val messageId = request.params("message_id")

    for {box <- mailSystem.getMailBox(email)
         uuid <- uuid(messageId)
         message <- box.get(uuid)} yield {
      message
    }
  }


  delete(route = "/mailboxes/:email/messages/:message_id") { request: Request =>
    val email = request.params("email")
    val messageId = request.params("message_id")
    for {box <- mailSystem.getMailBox(email)
         uuid <- uuid(messageId)
         message <- box.delete(uuid)} yield {
      DeleteMessageResponse(message.id, success = true)
    }
  }

  get(route = "/mailboxes/:email/messages") { request: Request =>
    val email = request.params("email")
    val from = request.params.get("from").flatMap(uuid)
    for {box <- mailSystem.getMailBox(email)} yield {
      val messages = box.messages(from, 11)
      val nextPage = messages
        .drop(10)
        .headOption
        .map(m => generateLoadMessagesUrl(email, m.id))

      PageIter(data = messages.take(10), links = PageLink(nextPage))
    }
  }

  private def uuid(str: String): Option[UUID] = {
    try {
      Some(UUID.fromString(str))
    } catch {
      case e: Exception => None
    }
  }

  def generateLoadMessagesUrl(email: String, fromId: UUID): String = {
    s"/mailboxes/${email}/messages?from=${fromId}"
  }
}
