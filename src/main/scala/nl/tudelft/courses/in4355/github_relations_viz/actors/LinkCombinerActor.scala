package nl.tudelft.courses.in4355.github_relations_viz.actors
import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities.Link

//#actor
class LinkCombinerActor extends Actor {
  def receive = {
    case (actor: ActorRef, cmd: ActorCommand) => //The normal command, retreive some links
      println("Starting computation")
      actor ! cmd
    case linkResult(map) => 
      println("got result!!")
   }
}
//#actor


class LinkcombineApplication {
  //#setup
  val system = ActorSystem("LinkcombineApplication", ConfigFactory.load.getConfig("remotelookup"))
  val actor = system.actorOf(Props[LinkCombinerActor], "lookupActor")
  val remoteActor = system.actorFor("akka://ProjectLinkApplication@127.0.0.1:2552/user/linkReader1")

  def doSomething(op: ActorCommand) = {
    actor ! (remoteActor, op)
  }
  //#setup

  def startup() {
  }

  def shutdown() {
    system.shutdown()
  }
}


object combineLinks {
  
  def main(args: Array[String]) {
    val app = new LinkcombineApplication
    println("Started Lookup Application")
    app.doSomething(obtainLinks(0, 10000000))
  }
}