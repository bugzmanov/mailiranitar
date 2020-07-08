package com.bugzmanov.mailiranitar.web

import java.net.InetAddress
import java.time.Instant

import com.bugzmanov.mailiranitar.Context
import com.bugzmanov.mailiranitar.mail.{MailBoxStat, Message}
import com.bugzmanov.mailiranitar.web.{MailiranitarServer, PageIter}
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.twitter.finagle.Http
import com.twitter.finagle.http.{Response, Status}
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.finatra.jackson.ScalaObjectMapper
import com.twitter.inject.server.{EmbeddedTwitterServer, FeatureTest, PortUtils}

class ServerSpec extends FeatureTest {

  private val objectMapper: ScalaObjectMapper = {
    val mapper = ScalaObjectMapper.builder
      .withPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
      .objectMapper
    mapper.registerModule(new JavaTimeModule())
    mapper
  }

  private val mailranitarServer = new MailiranitarServer(Context.config, Context.mailSystem)

  override protected val server =
    new EmbeddedTwitterServer(
      twitterServer = mailranitarServer,
      flags = Map(
        "http.port" -> PortUtils.ephemeralLoopback,
        "dtab.add" -> "/$/inet=>/$/nil;/zk=>/$/nil"
      )
    )

  private lazy val httpClient =
    Http.client
      .withSessionQualifier.noFailFast
      .withSessionQualifier.noFailureAccrual
      .newService(
        s"${InetAddress.getLoopbackAddress.getHostAddress}:${mailranitarServer.httpExternalPort.get}"
      )

  override protected def beforeAll(): Unit = {
    server.start()
  }


  test("MailiranitarServer#starts") {
    server.isHealthy should be(true)
  }

  test("MailiranitarServer#create mail box") {
    val request = RequestBuilder.post("/mailboxes")

    val response = await(httpClient(request))
    response.status should equal(Status.Ok)
    val stat = parseAwait[MailBoxStat](response)
    stat shouldBe defined

    RestClient.assertBoxExists(stat.head.email)
  }

  test("MailiranitarServer#delete mail box") {
    val boxEmail = RestClient.createMailBox().head.email

    val deleteResponse = await(httpClient(RequestBuilder.delete(s"/mailboxes/$boxEmail")))
    deleteResponse.status should equal(Status.Ok)

    RestClient.assertBoxDoesntExist(boxEmail)
  }

  test("MailiranitarServer#post a mail") {
    val boxEmail = RestClient.createMailBox().head.email

    val postMailRequest = RequestBuilder.post(s"/mailboxes/$boxEmail/messages")
      .body(
        """
          |{
          |     "sender": "ololo@gmail.com",
          |     "subject": "we need to talk about ololo",
          |     "body": "Call me ASAP",
          |     "sent":"2020-07-07T13:20:14.714Z"
          | }
          |""".stripMargin
      )


    val postMailRsp = await(httpClient(postMailRequest))
    postMailRsp.status should equal(Status.Ok)
    val message = parseAwait[Message](postMailRsp)

    message shouldBe defined
    message.get.sender should be ("ololo@gmail.com")
    message.get.subject should be ("we need to talk about ololo")
    message.get.sent should be (Instant.parse("2020-07-07T13:20:14.714Z"))
    message.get.body should be ("Call me ASAP")

    RestClient.assertMessageExists(boxEmail, message.get.id.toString)
  }

  test("MailiranitarServer#delete a mail") {
    val boxEmail = RestClient.createMailBox().head.email
    val message = RestClient.postMessage(boxEmail, "test1").get

    val request = RequestBuilder.delete(s"/mailboxes/$boxEmail/messages/${message.id}")
    val response = await(httpClient(request))
    response.status should equal(Status.Ok)

    RestClient.assertMessageDoesntExist(boxEmail, message.id.toString)
  }

  test("MailiranitarServer#get mailbox messages") {
    val boxEmail = RestClient.createMailBox().head.email
    val message = RestClient.postMessage(boxEmail, "test1").get

    val response = await(httpClient(RequestBuilder.get(s"/mailboxes/$boxEmail/messages")))
    response.status should equal(Status.Ok)

    val messages = parseAwait[PageIter[Message]](response)
    messages shouldBe defined

    messages.get.data should have size(1)
  }

  test("MailiranitarServer#messages in mailbox ordered by recieved date desc") {
    val boxEmail = RestClient.createMailBox().head.email
    RestClient.postMessage(boxEmail, "test1").get
    RestClient.postMessage(boxEmail, "test2").get
    RestClient.postMessage(boxEmail, "test3").get

    val response = await(httpClient(RequestBuilder.get(s"/mailboxes/$boxEmail/messages")))
    response.status should equal(Status.Ok)

    val messages = parseAwait[PageIter[Message]](response)
    messages shouldBe defined

    messages.get.data should have size(3)
    messages.get.data.map(_.body) should be (Vector("test3","test2", "test1"))
  }

  test("MailiranitarServer#messages in mailbox pagination") {
    val boxEmail = RestClient.createMailBox().head.email
    RestClient.postMessage(boxEmail, "test1").get
    val message = RestClient.postMessage(boxEmail, "test2").get
    RestClient.postMessage(boxEmail, "test3").get

    val response = await(httpClient(RequestBuilder.get(s"/mailboxes/$boxEmail/messages?from=${message.id}")))
    response.status should equal(Status.Ok)

    val messages = parseAwait[PageIter[Message]](response)

    messages.get.data.map(_.body) should be (Vector("test2", "test1"))
  }


  test("MailiranitarServer#messages in mailbox next page") {
    val boxEmail = RestClient.createMailBox().head.email
    for(i <- 0 to 20) {
      RestClient.postMessage(boxEmail, s"test$i").get
    }

    val response = await(httpClient(RequestBuilder.get(s"/mailboxes/$boxEmail/messages")))
    response.status should equal(Status.Ok)

    val page1 = parseAwait[PageIter[Message]](response)

    page1.get.data.map(_.body) should be (
      (20 to 11 by -1).map(i => s"test$i").toVector
    )

    page1.get.links.next shouldBe defined

    val nextPage = await(httpClient(RequestBuilder.get(page1.get.links.next.get)))
    nextPage.status should equal(Status.Ok)

    val page2 = parseAwait[PageIter[Message]](nextPage)

    page2.get.data.map(_.body) should be (
      (10 to 1 by -1).map(i => s"test$i").toVector
    )
  }


  private def parseAwait[T:Manifest](resp: Response): Option[T] = {
    await(resp.reader.map(buf => objectMapper.parse[T](buf)).read())
  }

  object RestClient {

     def assertBoxDoesntExist(email: String): Unit = {
      val request = RequestBuilder.get(s"/mailboxes/$email/messages")
      val response = await(httpClient(request))
      response.status should equal(Status.NotFound)
    }

     def assertBoxExists(email: String): Unit = {
      val request = RequestBuilder.get(s"/mailboxes/$email/messages")
      val response = await(httpClient(request))
      response.status should equal(Status.Ok)
    }

    def assertMessageExists(email: String, messageId: String): Unit = {
      val request = RequestBuilder.get(s"/mailboxes/$email/messages/$messageId")
      val response = await(httpClient(request))
      response.status should equal(Status.Ok)
    }

    def assertMessageDoesntExist(email: String, messageId: String): Unit = {
      val request = RequestBuilder.get(s"/mailboxes/$email/messages/$messageId")
      val response = await(httpClient(request))
      response.status should equal(Status.NotFound)
    }


    def createMailBox(): Option[MailBoxStat] = {
      val request = RequestBuilder.post("/mailboxes")
      val response = await(httpClient(request))
      response.status should equal(Status.Ok)
      parseAwait[MailBoxStat](response)
    }

    def postMessage(toEmail: String, text: String): Option[Message] = {
      val postMailRequest = RequestBuilder.post(s"/mailboxes/$toEmail/messages")
        .body(
          s"""
            |{
            |     "sender": "ololo@gmail.com",
            |     "subject": "${text}",
            |     "body": "${text}",
            |     "sent":"2020-07-07T13:20:14.714Z"
            | }
            |""".stripMargin
        )
      val postMailRsp = await(httpClient(postMailRequest))
      postMailRsp.status should equal(Status.Ok)
      parseAwait[Message](postMailRsp)
    }
  }
}
