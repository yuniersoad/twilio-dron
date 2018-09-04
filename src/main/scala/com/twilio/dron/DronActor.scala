package com.twilio.dron

import akka.actor.{ Actor, ActorLogging, Props }

object DronActor {
  sealed trait Command
  case class DTMFCommand(digits: String) extends Command
  case class SpeechCommand(speech: String) extends Command

  sealed trait Response
  case object Done extends Response
  case object UnknownCommand extends Response

  def props: Props = Props[DronActor]
}

class DronActor extends Actor with ActorLogging {
  import DronActor._

  override def receive: Receive = {
    case DTMFCommand(digits) =>
      log.info("Dron will Digits: {}", digits)
      Thread.sleep(1000L)
      sender() ! (digits match {
        case "1" => UnknownCommand
        case _ => Done
      })

    case SpeechCommand(speech) =>
      log.info("Dron will Speech {}", speech)
      sender() ! Done
  }
}
