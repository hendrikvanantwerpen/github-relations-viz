package nl.tudelft.courses.in4355.github_relations_viz.actors
import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities.Link
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import akka.dispatch.Future
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import akka.util.duration._
import akka.dispatch.Await
import akka.dispatch.ExecutionContext
import java.util.concurrent.Executors
import com.typesafe.config.Config
import akka.actor.Extension
import akka.actor.ActorSystem.Settings
import akka.actor.ExtensionIdProvider
import akka.actor.ExtensionId
import akka.actor.ExtendedActorSystem
import akka.actor.Deploy
import akka.remote.RemoteScope
import akka.actor.AddressFromURIString

//Link combiner actor. Able to initialize computer actors, and requesting them to obtain project links
class LinkCombineActor extends Actor {
  implicit val timeout: Timeout = 60 seconds
  implicit val ec = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  
  println("Initialized linkcombiner actor")
  
  def receive = {
    //Obtain links request. Ask the children, map a foldleft over them, and pipe the result back to the sender of the request. Crap, just two lines :O
    case o: obtainLinks => 
      Future.sequence(for (child <- context.children) yield child.ask(o).mapTo[linkResult].map(_.map))
      .map(_.foldLeft(Map[Link,Int]())   ((i,s) => i |<| s)).pipeTo(sender)
    //Initializing a series of computers
    case ActorComputationConfig(computers) =>
      println("Initializing computation actor")
      for (config <- computers) {
        context.actorOf(Props[ComputationActor], name = "LinkComputer%d-%d".format(config.modulo,config.remainder)) ! config
      }
    //Initializing a series of other linkCombiners (possibly on a remote system)
    case ActorCombinerSet(configs) => {
      for (configVars <- configs) {
        println("Initializing combiner actor")
        val ref = context.system.actorOf(Props[LinkCombineActor].withDeploy(Deploy(scope = RemoteScope(configVars.system))))
        println("Reference is: "+ref)
        ref ! (configVars.initCommand)
      }
    }
   }
}
//#actor


//Custom configuration allows to pass the children of the linkCombenerActor
class SettingsImpl(config: Config) extends Extension {
  val Children: String = config.getString("children")
}

object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {
  override def lookup = Settings
  override def createExtension(system: ExtendedActorSystem) = new SettingsImpl(system.settings.config)
}


class LinkcombineApplication {
  //#setup
  val system = ActorSystem("LinkcombineApplication", ConfigFactory.load.getConfig("remotelookup"))
  val actor = system.actorOf(Props[LinkCombineActor], "lookupActor")

  val minDegree = 5;
  implicit val timeout = Timeout(240 seconds) // needed for `?` below
  implicit val ec = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  
  val remoteActorUrls = List(
      "akka://ProjectLinkApplication@188.165.237.154:2552/user/linkreader-8-0", 
      "akka://ProjectLinkApplication@188.165.237.154:2552/user/linkreader-8-1",
      "akka://ProjectLinkApplication@188.165.237.154:2552/user/linkreader-8-2",
      "akka://ProjectLinkApplication@188.165.237.154:2552/user/linkreader-8-3",
      "akka://ProjectLinkApplication@188.165.237.154:2552/user/linkreader-8-4",
      "akka://ProjectLinkApplication@188.165.237.154:2552/user/linkreader-8-5",
      "akka://ProjectLinkApplication@188.165.237.154:2552/user/linkreader-8-6",
      "akka://ProjectLinkApplication@188.165.237.154:2552/user/linkreader-8-7"
  )
  
  
  	
  	def doSomething(from: Int, until: Int, minDegree: Int) = {
    val op = obtainLinks(from, until)
    val remoteActors = for (u <- remoteActorUrls) yield system.actorFor(u)
   
    //Debug zooi
   // val remote = system.actorFor("akka://ProjectLinkApplication@127.0.0.1:2552/user/linkreader-4-0");
  	//actor ! (remote, op);
    /**
  	println("Done sending request");
  	val f: Future[linkResult] = remote.ask(op).mapTo[linkResult];
  	println("Waiting for answer")
  	val f12 = Await.result(f, 240 seconds).asInstanceOf[linkResult]
    println("Testresult found: "+f12.map.size)
    **/
    //List with the futures containing all results
    
    val listOfFutures = for (a <- remoteActors) yield (
    	ask (a, op).mapTo[linkResult].map(_.map)
    )
     
    //futureList.flatMap(Await.result(_, 60 seconds))
    println("Waiting for the results. Wait, waiting? No waiting! Yes i know, still gotta fix that bro!")
     
    //val futureTest = futureList.head
    //val f12 = Await.result(futureTest, 240 seconds).asInstanceOf[linkResult]
    //println("Testresult found: "+f12.map.size)

     
    val futureList = Future.sequence(listOfFutures)
    val results = Await.result(futureList, timeout.duration);
    
    //for (f <- futureList)  println("size is: "+Await.result(f, timeout.duration).asInstanceOf[linkResult].map.size)
    println("Got the results!, size: "+results.size)


    val resultMap = results.foldLeft(Map[Link,Int]())   ((i,s) => i |<| s)
    println("Folded size: "+resultMap.size)
    //.flatMapReduce[Map[Link,Int]](projectsToLinks)
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
      val system = ActorSystem("ghlink", ConfigFactory.load.getConfig("LinkCombine"))
      val actor = system.actorOf(Props[LinkCombineActor], "LinkCombineActor")
      
      println("intializing config.")
     val testActor = system.actorOf(Props(new Actor {
	       
	      val config = ActorCombinerSet(List(
	          ActorCombinerConfig(AddressFromURIString("akka://ghlink@127.0.0.1:2553"), ActorComputationConfig(List(
	        				  LinkComputerConfig(4, 0),
	        				  LinkComputerConfig(4, 1),
	        				  LinkComputerConfig(4, 2),
	        				  LinkComputerConfig(4, 3)
	        		      ))
	             
	        		  )
	       ))
	       
	       actor ! config
	       println("Config sent")
	      def receive = {
	        case linkResult(res) =>
	          println("GOT THE RESULT!!!. Size is: %d".format(res.size))
	      }
     }))
  }
}