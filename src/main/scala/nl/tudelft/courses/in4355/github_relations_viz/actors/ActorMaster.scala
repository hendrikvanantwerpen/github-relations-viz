package nl.tudelft.courses.in4355.github_relations_viz.actors

import scala.io.Source
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import akka.pattern.ask
import akka.actor._
import akka.event.Logging

object ActorMaster extends App {
	  val system = ActorSystem("MySystem")
	  
	  
	  val myActor = system.actorOf(Props[GHObtainLinks], name = "GHObtainLinks")
	  
	  
	  
	  
}
