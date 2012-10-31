package nl.tudelft.courses.in4355.github_relations_viz.actors
import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities.Link
import akka.dispatch.Future
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import akka.util.duration._ 
import akka.dispatch.Await

//#actor, wordt momenteel niet gebruikt
class LinkCombinerActor extends Actor {
  def receive = {
    case (actor: ActorRef, cmd: ActorCommand) => //The normal command, retreive some links
      println("Starting computation on actor: "+actor.toString+" with command "+cmd.toString())
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
  val minDegree = 5;
  implicit val timeout = Timeout(240 seconds) // needed for `?` below
  
  val remoteActorUrls = List(
      "akka://ProjectLinkApplication@127.0.0.1:2552/user/linkreader-4-0", 
      "akka://ProjectLinkApplication@127.0.0.1:2552/user/linkreader-4-1",
      "akka://ProjectLinkApplication@127.0.0.1:2552/user/linkreader-4-2",
      "akka://ProjectLinkApplication@127.0.0.1:2552/user/linkreader-4-3"
  )
  
  

  	def doSomething(from: Int, until: Int, minDegree: Int) = {
    val op = obtainLinks(from, until)
    val remoteActors = for (u <- remoteActorUrls) yield system.actorFor(u)
   
    //List with the futures containing all results
    val futureList = for (a <- remoteActors) yield (
    	ask (a, op)
    )
     
    //futureList.flatMap(Await.result(_, 60 seconds))
    println("Waiting for the results. Wait, waiting? No waiting! Yes i know, still gotta fix that bro!")
     
    val futureTest = futureList.head
    val f12 = Await.ready(futureTest, 240 seconds)
    println("Testresult found: "+f12.toString())

    
    var results = Map[Link, Int]() //Also needs to be rewritten. Dont use variables!
    for (f <- futureList) results ++ Await.result(f, timeout.duration).asInstanceOf[linkResult].map
    	
    
    println("Got the results!")
    
    //results.filter( _._2 >= minDegree )
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
    println("Started Lookup Application 0.25")
    val result = app.doSomething(0, 10000000, 5)
   // println("Got the result of size: %d".format(result.size))
  }
}