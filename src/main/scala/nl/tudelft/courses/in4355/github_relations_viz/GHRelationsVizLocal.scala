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
import akka.dispatch.{Future,Promise,ExecutionContext}
import java.util.concurrent.Executors

class GHRelationsVizLocal(projectsurl: URL,
                          usersurl: URL,
                          forksurl: URL,
                          commitsurl: URL,
                          minFrom: Int,
                          maxUntil: Int,
                          period: Int) extends GHRelationsViz {
  import GHRelationsViz._

  implicit val ec = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())  
  
  println( "Reading users" )
  val users = readUsers(usersurl)
  def getUser(id: UserRef) = users.get(id).getOrElse(User.unknown(id))
  
  println( "Reading projects" )
  val projects = readProjects(projectsurl)
  def getProject(id: ProjectRef) = projects.get(id).getOrElse(Project.unknown(id))
  
  println( "Reading forks" )
  val forks = readForks(forksurl)
  
  println( "Reading commits" )
  val commits = readCommits(commitsurl)
    .mapReduce[ParMap[Int,Set[(UserRef,ProjectRef)]]] { c =>
      Some(c).filter( c => c.timestamp >= minFrom &&
                           c.timestamp <= maxUntil &&
                           !forks.contains(c.project) )
             .map( c => (getBinnedTime(period)(c.timestamp),(c.user,c.project)) )
             .toList
     }

  println( "Calculating project-user/week histogram" )
  val userProjectLinksPerWeek =
    commits.mapReduce[SortedMap[Int,Int]]( e => e._1 -> e._2.size )
           .toList
  def getUserProjectsLinksPerWeek = Promise.successful { userProjectLinksPerWeek }
  
  def getProjectLinks(from: Int, until: Int, minWeight: Int) = Promise.successful {
    Timer.tick
    println( "Calculating project links from %d until %d with minimum weight %d".format(from,until,minWeight) )
    commits
      .log( cs => println( "Filter %d time bins".format(cs.size) ) )
      .par.filter( e => e._1 >= from && e._1 <= until ).map( kv => kv._2 ).flatten
      .log( cs => println("Reduce %d commits to projects per user".format(cs.size) ) )
      .reduceTo[ParMap[UserRef,Set[ProjectRef]]]
      .map( kv => kv._2 )
      .log( psets => println("Done reducing 1 in %d, Reducing %d project sets to link map".format(Timer.tick, psets.size) ) )
      .par.mapReduce[ParMap[Link,Int]]( projectsToLinks )
      .log( lm => println( "Done reducing 2 in %d, Filter %d links by weight".format(Timer.tick, lm.size) ) )
      .filter( _._2 >= minWeight )
      .log(cs => println("Done filtering in %d".format(Timer.tick)))
  }
  
}