package nl.tudelft.courses.in4355.github_relations_viz

import java.net.URL
import java.util.Date
import scala.io.Source
import scala.util.matching.Regex
import GHEntities._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import util.Logger._
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
import nl.tudelft.courses.in4355.github_relations_viz.util.Logger

class GHRelationsVizDist(projectsurl: URL,
                         usersurl: URL)(implicit ec: ExecutionContext, to: Timeout) extends GHRelationsViz {
  import GHRelationsViz._

  println( "Reading users" )
  val users = readUsers(usersurl)
  def getUser(id: UserRef) = users.get(id).getOrElse(User.unknown(id))
  
  println( "Reading projects" )
  val projects = readProjects(projectsurl)
  def getProject(id: ProjectRef) = projects.get(id).getOrElse(Project.unknown(id))
  
  val linkCombineActor = system.actorOf(Props[LinkCombineActor], "LinkCombineActor")

  def getProjectLinks(from: Int, until: Int, minWeight: Int, limit: Int) =
    (linkCombineActor ? obtainLinks(from,until)).map( lr => Right(lr.asInstanceOf[linkResult].map) )

  def getUserProjectsLinksPerWeek = Promise.successful ( Nil )
  
  def system = ActorSystem("ghlink", ConfigFactory.load.getConfig("LinkCombine"))
}