package com.twilio.dron

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import com.twilio.dron.DronActor._
import com.twilio.dron.DronActor.{ Command, Done, UnknownCommand }
import com.twilio.twiml.VoiceResponse
import com.twilio.twiml.voice.Gather.Input.{ DTMF, SPEECH }
import com.twilio.twiml.voice.Say.Language.EN_US
import com.twilio.twiml.voice.{ Gather, Say }

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait DronRoutes {

  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[DronRoutes])

  def dronActor: ActorRef

  // Required by the `ask` (?) method below
  implicit lazy val timeout: Timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration

  lazy val dronRoutes: Route =
    pathPrefix("dron") {
      concat(
        pathEnd {
          post(formFields("Digits".?, "SpeechResult".?) { (d, s) =>
            log.info("Digits: {}, Speech: {}", d, s)

            val command: Option[Command] = d.map(s => DTMFCommand(s)).orElse(s.map(s => SpeechCommand(s)))

            val feedback: Future[String] = command.map(c => (dronActor ? c).map {
              case Done => "Done"
              case UnknownCommand => "Unknown Command"
            }).getOrElse(Future.successful("What are my orders?"))

            val resF: Future[HttpResponse] = feedback.map { feedback =>
              val say = new Say.Builder().language(EN_US).addText(feedback).build()

              val gather = new Gather.Builder()
                .timeout(10)
                .inputs(List(SPEECH, DTMF).asJava)
                .numDigits(1)
                .maxSpeechTime(2)
                .finishOnKey("")
                .hints("take off, land")
                .language(Gather.Language.EN_US)
                .build
              val response = new VoiceResponse.Builder().say(say).gather(gather).build

              val entity = HttpEntity(`text/xml(UTF-8)`, response.toXml)
              HttpResponse(StatusCodes.OK, entity = entity)

            }

            complete(resF)
          })
        }
      )
    }
}
