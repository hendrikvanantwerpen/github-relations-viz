package nl.tudelft.courses.in4355.github_relations_viz.actors
import nl.tudelft.courses.in4355.github_relations_viz.Timer
import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities.Link
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
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import scala.collection.immutable.SortedMap
import scala.collection.parallel.ParMap


//Link combiner actor. Able to initialize computer actors, and requesting them to obtain project links
class LinkCombineActor extends Actor {
  implicit val timeout: Timeout = 2400 seconds
  implicit val ec = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  
  println("Initialized linkcombiner actor")
  
  def receive = {
    //Obtain links request. Ask the children, map a foldleft over them, and pipe the result back to the sender of the request. Crap, just two lines :O
    case o: obtainLinks =>
      println("Received link obtain command. Sending to %d children.".format(context.children.size))
      Future.sequence(for (child <- context.children) yield child.ask(o).mapTo[linkResult].map(_.map))
      .map(_.foldLeft(Map[Link,Int]())   ((i,s) => i |<| s)).map(linkResult(_)).pipeTo(sender)
    //Another dirty fix for presewntation
    case o:obtainLinksFilterPass =>
    	println("Received link obtain command. Sending to %d children.".format(context.children.size))
      Future.sequence(for (child <- context.children) yield child.ask(obtainLinksFilter(o.From, o.until, o.degree)).mapTo[linkResult].map(_.map))
      .map(_.foldLeft(Map[Link,Int]())   ((i,s) => i |<| s)).map(linkResult(_)).pipeTo(sender)
    case o: obtainLinksFilter =>
      println("Received link obtain command with filter. Sending to %d children.".format(context.children.size))
      Future.sequence(for (child <- context.children) yield child.ask(obtainLinks(o.From, o.until)).mapTo[linkResult].map(_.map))
      .map(_.foldLeft(Map[Link,Int]())   ((i,s) => i |<| s)).map(_.filter(_._2 >= o.degree)).map(linkResult(_)).pipeTo(sender)
    //Initializing a series of computers
    case ActorComputationConfig(computers) =>
      println("Initializing computation actor")
      for (config <- computers) {
        context.actorOf(Props[ComputationActor], name = "LinkComputer%d-%d".format(config.modulo,config.remainder)) ! config
      }
    //Initializing a series of other linkCombiners (possibly on a remote system)
    case ActorCombinerSet(configs) => {
      for (configVars <- configs) {
        println("Initializing combiner actor, deploying from "+context.system+" on "+configVars.system)
        val ref = context.actorOf(Props[LinkCombineActor].withDeploy(Deploy(scope = RemoteScope(configVars.system))))
        println("Reference is: "+ref)
        ref ! (configVars.initCommand)
      }
    }
    //Ask a child for the projects per week. Quick dirty fix for the presentation on friday
    case userProjectsPerWeekSkip() => {
      context.children.head.ask(userProjectsPerWeek()).pipeTo(sender)
    }
    //Compute the histogram of projects per user per week
    case userProjectsPerWeek() => {
      sender ! GHResources.commits.mapReduce[SortedMap[Int,Int]]( e => e._1 -> e._2.size ).toList
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
