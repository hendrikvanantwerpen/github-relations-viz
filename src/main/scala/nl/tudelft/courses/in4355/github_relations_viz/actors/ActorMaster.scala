package nl.tudelft.courses.in4355.github_relations_viz.actors

import scala.io.Source
import akka.pattern.ask
import akka.actor._
import akka.event.Logging
import java.net.URL

object AM extends App {
	  val system = ActorSystem("ObtainLinks")
	  
	  val actor = system.actorFor("akka://ObtainLinks@127.0.0.1:2552/testyser/actorName")
	  
	println("Creating actor...");
	  //val myActor = system.actorOf(Props(new GHObtainLinks(getClass.getResource("/commits.txt"), 100,1)), name = "GHObtainLinks")
	  println("Done :)");

	  
}

class ActorMaster {
  
  
}