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

class GHRelationsVizDist(projectsurl: URL,
                         usersurl: URL,
                         system: ActorSystem) extends GHRelationsViz {
  import GHRelationsViz._

  println( "Reading users" )
  val users = readUsers(usersurl)
  def getUser(id: UserRef) = users.get(id).getOrElse(User.unknown(id))
  
  println( "Reading projects" )
  val projects = readProjects(projectsurl)
  def getProject(id: ProjectRef) = projects.get(id).getOrElse(Project.unknown(id))
  
  implicit val timeout: Timeout = 2400 seconds
  implicit val ec = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  
  val linkCombineActor = system.actorOf(Props[LinkCombineActor], "LinkCombineActor")

  def getLimits = Promise.successful ( Range(0,0,0) )

  def getProjectLinks(from: Int, until: Int, minWeight: Int) =
    (linkCombineActor ? obtainLinks(from,until)).map( _.asInstanceOf[linkResult].map )

  def getUserProjectsLinksPerWeek = Promise.successful ( Nil )
  
}