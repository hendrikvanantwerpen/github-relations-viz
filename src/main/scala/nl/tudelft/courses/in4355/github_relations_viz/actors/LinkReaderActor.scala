package nl.tudelft.courses.in4355.github_relations_viz.actors
import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import java.io.File
import akka.actor.UntypedActorFactory

//The actor
class LinkReaderActor(divisor: Int, remainder: Int) extends Actor{
	val url = getClass.getResource("/commits.txt")
	val computeEngine = new GHObtainLinks(url, divisor, remainder)
	def receive = {
	  case obtainLinks(from, until) =>
	    println("Actor "+this.toString()+" Obtaining links from %d, to %d.".format(from, until))
	    val links = computeEngine.obtainLinks(from, until)
	    println("Actor "+this.toString()+" Sending result back"+links.toString)
	    sender ! linkResult(links)
	  case _ =>
	    println("linkReader received unknown command...")
	}
}
//End of the actor

//Class used to manage the actors
class linkReaderApplication() {
  val system = ActorSystem("ProjectLinkApplication", ConfigFactory.load.getConfig("linkcalculator"))
  val numActors = 4;
  
  for(remainder <- 0 to numActors-1) {
    createActor(system, numActors, remainder)
  }
  
  
  
  //#setup

  def startup() {
  }

  def shutdown() {
    system.shutdown()
  }
  
  //Creates an actor for the given properties
  def createActor(system: ActorSystem, divisor: Int, remainder: Int) = {
    println("Creating actor with name: "+"linkreader-%d-%d".format(divisor, remainder))
    system.actorOf(Props(new LinkReaderActor(divisor,remainder)), "linkreader-%d-%d".format(divisor, remainder));
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