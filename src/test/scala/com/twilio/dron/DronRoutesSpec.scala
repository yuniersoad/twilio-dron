package com.twilio.dron

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ Matchers, WordSpec }

import scala.xml.Utility.trim

class DronRoutesSpec extends WordSpec with Matchers with ScalaFutures with ScalatestRouteTest
    with DronRoutes {

  override val dronActor: ActorRef = system.actorOf(DronActor.props, "dronActor")

  lazy val routes: Route = dronRoutes

  "DronRoutes" should {
    "Gather orders when no parameters are given (POST /dron)" in {
      val request = HttpRequest(uri = "/dron", method = HttpMethods.POST)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)

        // we expect the response to be xml or Twilio won't process it correctly:
        contentType should ===(ContentTypes.`text/xml(UTF-8)`)

        val xmlResp = scala.xml.XML.loadString(entityAs[String])
        val expected = <Response>
                         <Say language="en-US">What are my orders?</Say>
                         <Gather timeout="10" numDigits="1" maxSpeechTime="2" language="en-US" input="speech dtmf" hints="take off, land" finishOnKey=""/>
                       </Response>

        trim(xmlResp) should ===(trim(expected))
      }
    }

  }
}
