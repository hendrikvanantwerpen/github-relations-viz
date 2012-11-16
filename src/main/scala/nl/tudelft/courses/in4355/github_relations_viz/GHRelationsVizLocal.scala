package nl.tudelft.courses.in4355.github_relations_viz

import java.net.URL
import java.util.Date
import scala.io.Source
import scala.util.matching.Regex
import GHEntities._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import util.Logger._
import util.Timer
import util.Timer._
import util.ThresholdCountingSet
import scala.collection.immutable.SortedMap
import scala.collection.parallel.ParMap
import scala.collection.GenMap
import akka.dispatch.{Future,Promise,ExecutionContext}
import java.util.concurrent.Executors
import akka.actor.ActorSystem
import nl.tudelft.courses.in4355.github_relations_viz.util.Logger

class GHRelationsVizLocal(projectsurl: URL,
                          usersurl: URL,
                          forksurl: URL,
                          commitsurl: URL,
                          minFrom: Int,
                          maxUntil: Int,
                          period: Int)(implicit ec: ExecutionContext) extends GHRelationsViz {
  import GHRelationsViz._

  val timer = new Timer
  
  println( "Reading users" )
  timer.tick
  val users = readUsers(usersurl).log( println("Done in %s".format(timer.tick.nanoTimeToString)) )
  override def getUser(id: UserRef) = users.get(id).getOrElse(User.unknown(id))
  
  println( "Reading projects" )
  timer.tick
  val projects = readProjects(projectsurl).log( println("Done in %s".format(timer.tick.nanoTimeToString)) )
  override def getProject(id: ProjectRef) = projects.get(id).getOrElse(Project.unknown(id))
  
  println( "Reading forks" )
  timer.tick
  val forks = readForks(forksurl).log( println("Done in %s".format(timer.tick.nanoTimeToString)) )
  
  println( "Reading commits" )
  timer.tick
  val commits = readCommits(commitsurl)
    .mapReduce[ParMap[Int,Set[(UserRef,ProjectRef)]]] { c =>
      Some(c).filter( c => c.timestamp >= minFrom &&
                           c.timestamp <= maxUntil &&
                           !forks.contains(c.project) )
             .map( c => (getBinnedTime(period)(c.timestamp),(c.user,c.project)) )
             .toList
     }.log( println("Done in %s".format(timer.tick.nanoTimeToString)) )

  println( "Calculating project-user/week histogram" )
  timer.tick
  val userProjectLinksPerWeek =
    commits.mapReduce[SortedMap[Int,Int]]( e => e._1 -> e._2.size )
           .toList
           .log( println("Done in %s".format(timer.tick.nanoTimeToString)) )
  override def getUserProjectsLinksPerWeek = Promise.successful { userProjectLinksPerWeek }
  
  override def getProjectLinks(from: Int, until: Int, minWeight: Int, limit: Int) = Promise.successful {
    val timer = new Timer
    println( "Calculating project links from %d until %d with minimum weight %d".format(from,until,minWeight) )
    commits
      .log( cs => println( "Filter %d time bins".format(cs.size) ) )
      .par.filter( e => e._1 >= from && e._1 <= until ).map( kv => kv._2 ).flatten
      .log( cs => println("Done in %s.\nReduce %d commits to projects per user".format(timer.tick.nanoTimeToString,cs.size) ) )
      .reduceTo[ParMap[UserRef,Set[ProjectRef]]]
      .map( kv => kv._2 )
      .log( psets => println("Done in %s.\nReducing %d project sets to link map".format(timer.tick.nanoTimeToString, psets.size) ) )
      .par.foldLeft(Right(ThresholdCountingSet[Link](minWeight)):Either[String,ThresholdCountingSet[Link]]) { (ecs,ps) =>
         ecs.right.flatMap { cs =>
           if ( cs.included.size > limit ) Left("Limit exceeded.")
           else Right( cs ++ projectsToLinksWithoutCount(ps) )
         }
       }
      .log( _.fold( _ => println( "Interrupted because limit was exceeded." ),
                    _ => println( "Done in %s.".format(timer.tick.nanoTimeToString) ) ) )
      .right.map ( _.included )
      
  }
  
  override def system = ActorSystem()
}