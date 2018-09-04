package com.twilio.dron

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer

object QuickstartServer extends App with DronRoutes {

  // set up ActorSystem and other dependencies here
  implicit val system: ActorSystem = ActorSystem("dronAkkaHttpServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val dronActor: ActorRef = system.actorOf(DronActor.props, "dronActor")

  // from the DronRoutes trait
  lazy val routes: Route = dronRoutes

  Http().bindAndHandle(routes, "localhost", 8080)

  println(s"Server online at http://localhost:8080/")

  Await.result(system.whenTerminated, Duration.Inf)
}
