package nl.tudelft.courses.in4355.github_relations_viz.actors
import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities.Link
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.CollectionAggregators._
import net.van_antwerpen.scala.collection.mapreduce.ValueAggregators._
import akka.dispatch.Future
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import akka.util.duration._
import akka.dispatch.Await
import akka.dispatch.ExecutionContext
import java.util.concurrent.Executors

//#actor, wordt momenteel niet gebruikt
class LinkCombinerActor extends Actor {
  def receive = {
    case (actor: ActorRef, cmd: ActorCommand) => //The normal command, retreive some links
      println("Starting computation on actor: "+actor.toString+" with command "+cmd.toString())
      actor ! cmd
    case linkResult(mapr) => 
      println("got result!! size is: "+mapr.size)
   }
}
//#actor


class LinkcombineApplication {
  //#setup
  val system = ActorSystem("LinkcombineApplication", ConfigFactory.load.getConfig("remotelookup"))
  val actor = system.actorOf(Props[LinkCombinerActor], "lookupActor")

  val minDegree = 5;
  implicit val timeout = Timeout(240 seconds) // needed for `?` below
  implicit val ec = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  
  val remoteActorUrls = List(
      "akka://ProjectLinkApplication@127.0.0.1:2552/user/linkreader-4-0", 
      "akka://ProjectLinkApplication@127.0.0.1:2552/user/linkreader-4-1",
      "akka://ProjectLinkApplication@127.0.0.1:2552/user/linkreader-4-2",
      "akka://ProjectLinkApplication@127.0.0.1:2552/user/linkreader-4-3",
      "akka://ProjectLinkApplication@127.0.0.1:2552/user/linkreader-4-4",
      "akka://ProjectLinkApplication@127.0.0.1:2552/user/linkreader-4-5",
      "akka://ProjectLinkApplication@127.0.0.1:2552/user/linkreader-4-6",
      "akka://ProjectLinkApplication@127.0.0.1:2552/user/linkreader-4-7"
  )
  
  
  	
  	def doSomething(from: Int, until: Int, minDegree: Int) = {
    val op = obtainLinks(from, until)
    val remoteActors = for (u <- remoteActorUrls) yield system.actorFor(u)
   
    //Debug zooi
    val remote = system.actorFor("akka://ProjectLinkApplication@127.0.0.1:2552/user/linkreader-4-0");
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


    val resultMap = results.foldLeft(Map[Link,Int]())   ((i,s) => i |<<| s)
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
    val app = new LinkcombineApplication
    println("Started Lookup Application 0.25")
   val result = app.doSomething(0, 10000000, 5)
    
   // println("Got the result of size: %d".format(result.size))
  }
}