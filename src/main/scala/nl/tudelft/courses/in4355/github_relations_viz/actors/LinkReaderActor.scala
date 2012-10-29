package nl.tudelft.courses.in4355.github_relations_viz.actors
import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import java.io.File

//The actor
class LinkReaderActor() extends Actor{
	val url = getClass.getResource("/commits.txt")
	val divisor = 1
	val remainder = 0
	val maxLines = 10000
	val computeEngine = new GHObtainLinks(url, divisor, remainder, maxLines)
	def receive = {
	  case obtainLinks(from, until) =>
	    println("Obtaining links from %d, to %d.".format(from, until))
	    sender ! computeEngine.obtainLinks(from, until)
	  case _ =>
	    println("linkReader received unknown command...")
	}
}
//End of the actor

//Class used to manage the actors
class linkReaderApplication() {
  val system = ActorSystem("ProjectLinkApplication", ConfigFactory.load.getConfig("linkcalculator"))
  val actor = system.actorOf(Props[LinkReaderActor], "linkReader1")
  //#setup

  def startup() {
  }

  def shutdown() {
    system.shutdown()
  }
}
//And the end of that same class

//The main method
object lrApp {
  def main(args: Array[String]) {
    println("Initializing the linkreader application")
    new linkReaderApplication()
    println("Started linkReaderApplication - waiting for messages")
  }
}