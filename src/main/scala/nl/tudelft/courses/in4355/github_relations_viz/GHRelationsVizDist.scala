package nl.tudelft.courses.in4355.github_relations_viz

import java.net.URL
import java.util.Date
import scala.io.Source
import scala.util.matching.Regex
import GHEntities._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import Logger._
import scala.collection.immutable.SortedMap
import scala.collection.parallel.ParMap
import scala.collection.GenMap
import akka.actor.ActorSystem
import akka.actor.Props
import nl.tudelft.courses.in4355.github_relations_viz.actors.LinkCombineActor
import nl.tudelft.courses.in4355.github_relations_viz.actors.obtainLinks
import akka.pattern.{ ask, pipe }
import nl.tudelft.courses.in4355.github_relations_viz.actors.linkResult
import akka.util.Timeout
import akka.util.duration._
import akka.dispatch.{Future,Promise,ExecutionContext}
import java.util.concurrent.Executors
import com.typesafe.config.ConfigFactory
import nl.tudelft.courses.in4355.github_relations_viz.actors.ActorCombinerSet
import nl.tudelft.courses.in4355.github_relations_viz.actors.ActorCombinerConfig
import akka.actor.AddressFromURIString
import nl.tudelft.courses.in4355.github_relations_viz.actors.ActorComputationConfig
import nl.tudelft.courses.in4355.github_relations_viz.actors.LinkComputerConfig
import nl.tudelft.courses.in4355.github_relations_viz.actors.obtainLinksFilterPass
import nl.tudelft.courses.in4355.github_relations_viz.actors.userProjectsPerWeekSkip


class GHRelationsVizDist(projectsurl: URL,
                         usersurl: URL,
                         commitsurl: URL,
                         forksurl: URL,
                         system: ActorSystem,
                          minFrom: Int,
                          maxUntil: Int,
                          period: Int) extends GHRelationsViz {
  import GHRelationsViz._

  println( "Reading users" )
  val users = readUsers(usersurl)
  def getUser(id: UserRef) = users.get(id).getOrElse(User.unknown(id))
  
  println( "Reading projects" )
  val projects = readProjects(projectsurl)
  def getProject(id: ProjectRef) = projects.get(id).getOrElse(Project.unknown(id))
  
  implicit val timeout: Timeout = 59 seconds
  implicit val ec = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  
  val linkCombineActor = system.actorOf(Props[LinkCombineActor], "LinkCombineActor")
  
  val config = ActorCombinerSet(List(
	    ActorCombinerConfig(
	     AddressFromURIString("akka://ghlink@37.59.53.125:2552"),        
	     ActorCombinerSet(List(
	       ActorCombinerConfig(
	         AddressFromURIString("akka://ghlink@37.59.53.125:2552"), 
	         ActorComputationConfig(List(
        	   LinkComputerConfig(2, 0),
			   LinkComputerConfig(2, 1)
	         ))
	       )
	      /**
	       ,    
		       ActorCombinerConfig(
		         AddressFromURIString("akka://ghlink@188.165.237.154:2552"), 
		         ActorComputationConfig(List(
			       LinkComputerConfig(3, 2)
			       //LinkComputerConfig(10, 9)
			       //LinkComputerConfig(12, 10),
			       //LinkComputerConfig(12, 11)
		         ))      
		       )  
		       **/
		      
	      ))
	   )
	   ))
  
  linkCombineActor ! config

  def getProjectLinks(from: Int, until: Int, minWeight: Int) =
    (linkCombineActor.ask(obtainLinksFilterPass(from,until,minWeight))).map( _.asInstanceOf[linkResult].map )
  
  def getUserProjectsLinksPerWeek = (linkCombineActor.ask(userProjectsPerWeekSkip())).map(_.asInstanceOf[Seq[(Int, Int)]])
  
}