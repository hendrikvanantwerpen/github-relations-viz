package nl.tudelft.courses.in4355.github_relations_viz.actors
import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import java.io.File
import akka.actor.UntypedActorFactory
import java.net.URL


//The computation actor. Upon receiving a request it computes the links and sends the result back to the sender
class ComputationActor() extends Actor{
	val url = new URL("file://Users/nielsvankaam/Documents/Studie/FuncProgramming/repo/github-relations-viz/commits/commits.txt")
	var computeEngine: GHObtainLinks = null
	def receive = {
	  case LinkComputerConfig(divisor, remainder) =>
	    println("Obtaining compute engine with url: %s, modulor %d and remainder %d".format(url, divisor, remainder))
	    computeEngine = new GHObtainLinks(url, divisor, remainder)
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