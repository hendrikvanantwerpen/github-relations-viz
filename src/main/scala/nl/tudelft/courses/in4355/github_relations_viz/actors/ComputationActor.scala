package nl.tudelft.courses.in4355.github_relations_viz.actors
import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import java.io.File
import akka.actor.UntypedActorFactory
import java.net.URL


//The computation actor. Upon receiving a request it computes the links and sends the result back to the sender
class ComputationActor() extends Actor{
	var computeEngine: GHObtainLinks = null
	def receive = {
	  case LinkComputerConfig(divisor, remainder) =>
	    println("Obtaining compute engine with modulo %d and remainder %d".format(divisor, remainder))
	    computeEngine = new GHObtainLinks(divisor, remainder)
	  case obtainLinks(from, until) =>
	    println("Actor "+this.toString()+" Obtaining links from %d, to %d in actor %s.".format(from, until, this))
	    val links = computeEngine.getProjectLinks(from, until)
	    println("Actor "+this.toString()+" Sending result back to"+sender.toString)
	    sender ! linkResult(links)
	  case _ =>
	    println("linkReader received unknown command...")
	}
}
//End of the actor