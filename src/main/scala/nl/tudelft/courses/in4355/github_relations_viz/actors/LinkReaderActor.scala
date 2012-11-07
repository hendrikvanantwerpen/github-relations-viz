package nl.tudelft.courses.in4355.github_relations_viz.actors
import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import java.io.File
import akka.actor.UntypedActorFactory
import java.net.URL
//The actor
class LinkReaderActor(divisor: Int, remainder: Int, resource : String) extends Actor{
	val url = new URL("file://"+resource + "/commits.txt")
	println("Obtaining compute engine with url: "+"("+url+")")
	val computeEngine = new GHObtainLinks(url, divisor, remainder)
	def receive = {
	  case obtainLinks(from, until) =>
	    println("Actor "+this.toString()+" Obtaining links from %d, to %d.".format(from, until))
	    val links = computeEngine.obtainLinks(from, until)
	    println("Actor "+this.toString()+" Sending result back to"+sender.toString)
	    sender ! linkResult(links)
	  case _ =>
	    println("linkReader received unknown command...")
	}
}
//End of the actor

//Class used to manage the actors
class linkReaderApplication(resource : String) {
  val system = ActorSystem("ProjectLinkApplication", ConfigFactory.load.getConfig("linkcalculator"))
  val numActors = 8;
  
  val url = getClass.getResource("/actorConf.txt")
  val lines = scalax.io.Resource.fromURL(url).lines().map(_.toInt)
 // lines.foreach(r => println(r));
  
  
  lines.foreach(remainder => createActor(system, numActors, remainder)) 
  


  
  
  
  //#setup

  def startup() {
  }

  def shutdown() {
    system.shutdown()
  }
  
  //Creates an actor for the given properties
  def createActor(system: ActorSystem, divisor: Int, remainder: Int) = {
    println("Creating actor with name: "+"linkreader-%d-%d".format(divisor, remainder))
    system.actorOf(Props(new LinkReaderActor(divisor,remainder, resource)), "linkreader-%d-%d".format(divisor, remainder));
  }
}
//And the end of that same class

//The main method
object lrApp {
  def main(args: Array[String]) {
    if(args.size < 1) {
    	println("Please pass an url to the data directory")
    	return
    }
    println("Initializing lrApp on resource directory %s".format(args.head))

	if(! new File(args.head).exists()) {
		println("Resource directory not found: "+args.head)
		return
	}
 
    println("Initializing the linkreader application")
    new linkReaderApplication(args.head)
    println("Started linkReaderApplication - waiting for messages")
  }
}