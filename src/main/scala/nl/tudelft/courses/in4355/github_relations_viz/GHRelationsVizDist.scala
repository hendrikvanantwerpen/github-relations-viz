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
                         usersurl: URL)(implicit ec: ExecutionContext, timeout: Timeout) extends GHRelationsViz {
  import GHRelationsViz._

  println( "Reading users" )
  val users = readUsers(usersurl)
  override def getUser(id: UserRef) = users.get(id).getOrElse(User.unknown(id))
  
  println( "Reading projects" )
  val projects = readProjects(projectsurl)
  override def getProject(id: ProjectRef) = projects.get(id).getOrElse(Project.unknown(id))
  
  val linkCombineActor = system.actorOf(Props[LinkCombineActor], "LinkCombineActor")

  def getProjectLinks(from: Int, to: Int, langFilter: Map[String,Boolean], langStrict: Boolean, includeForks: Boolean, minLinkWeight: Int, limit: Int) =
    (linkCombineActor ? obtainLinks(from,to)).map( lr => Right(lr.asInstanceOf[linkResult].map) )

  override def getUserProjectsLinksPerWeek(from: Int, to: Int, langFilter: Map[String,Boolean], langStrict: Boolean, includeForks: Boolean) = Promise.successful ( Nil )
  
  override def getLanguages(from: Int, to: Int, includeForks: Boolean) = Promise.successful ( Nil )
  
  override def getParentProject(id: ProjectRef): Option[ProjectRef] = None
  
  def system = ActorSystem("ghlink", ConfigFactory.load.getConfig("LinkCombine"))
}