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
                          maxTo: Int,
                          period: Int)(implicit ec: ExecutionContext) extends GHRelationsViz {
  import GHRelationsViz._

  private val timer = new Timer
  
  println( "Reading users" )
  timer.tick
  private val users = readUsers(usersurl).log( println("Done in %s".format(timer.tick.nanoTimeToString)) )
  override def getUser(id: UserRef) = users.get(id).getOrElse(User.unknown(id))
  
  println( "Reading projects" )
  timer.tick
  private val projects = readProjects(projectsurl).log( println("Done in %s".format(timer.tick.nanoTimeToString)) )
  override def getProject(id: ProjectRef) = projects.get(id).getOrElse(Project.unknown(id))
  
  println( "Reading forks" )
  timer.tick
  private val forks = readForks(forksurl).log( println("Done in %s".format(timer.tick.nanoTimeToString)) )
  override def getParentProject(id: ProjectRef): Option[ProjectRef] = forks.get( id )
  
  println( "Reading commits" )
  timer.tick
  private val commits = readCommits(commitsurl)
    .mapReduce[ParMap[Int,Set[(UserRef,ProjectRef)]]] { c =>
      Some(c).filter( c => c.timestamp >= minFrom &&
                           c.timestamp <= maxTo )
             .map( c => (getBinnedTime(period)(c.timestamp),(c.user,c.project)) )
             .toList
     }.log( println("Done in %s".format(timer.tick.nanoTimeToString)) )

  private def getFilteredCommits(from: Int, to: Int, langFilter: Map[String,Boolean], langStrict: Boolean, includeForks: Boolean) = {
      println( "Get commits from %d until %d %s forks for %s (%s)".format(
                 from, to, 
                 if (includeForks) "with" else "without",
                 langFilter.toString,
                 if (langStrict) "strict" else "not strict") )
      
      val commitsInTime = timed( "Filter %d time bins".format(commits.size), println ) {
        commits.filter( e => e._1 >= from && e._1 <= to )
      }

      val commitsForForks = if (includeForks) {
        commitsInTime
      } else {
        timed( "Filter for forks".format(commitsInTime.size), println ) {
          commitsInTime.map( kv => kv._1 -> kv._2.filter( c => !forks.contains( c._2 ) ) )
        }
      }
      
      val commitsForLanguage = if (langFilter.isEmpty && !langStrict) {
        commitsForForks
      } else {
        timed( "Filter for languages".format(commitsForForks.size), println ) {
          commitsForForks.map( kv => kv._1 -> kv._2.filter( c =>
                                 projects get (c._2)
                                          map( p => langFilter get (p lang) 
                                                               getOrElse !langStrict )
                                          getOrElse false ) )
        }
      }
      
      commitsForLanguage
  }
     
  override def getUserProjectsLinksPerWeek(from: Int, to: Int, langFilter: Map[String,Boolean], langStrict: Boolean, includeForks: Boolean) =
    Promise.successful {
      println( "Calculating project-user/week histogram" )
	  val timer = new Timer
	  val emptyMap = SortedMap.empty[Int,Int] ++ commits.map( _._1 -> 0 )
	  val actualMap = getFilteredCommits(from, to, langFilter, langStrict, includeForks)
	    .mapReduce[SortedMap[Int,Int]]( e => e._1 -> e._2.size )
        .log( println("Done in %s".format(timer.tick.nanoTimeToString)) )
      emptyMap |<| actualMap
    }
     
  override def getLanguages(from: Int, to: Int, includeForks: Boolean) =
    Promise.successful {
      println( "Calculating languages" )
	  val timer = new Timer
      getFilteredCommits(from, to, Map.empty, false, includeForks)
	    .mapReduce[Map[ProjectRef,Int]]( _._2.map( up => up._2 -> 1 ) )
	    .mapReduce[Map[String,Int]]( pc => getProject(pc._1).lang -> pc._2 )
        .log( println("Done in %s".format(timer.tick.nanoTimeToString)) )
    }
  
  override def getProjectLinks(from: Int, to: Int, langFilter: Map[String,Boolean], langStrict: Boolean, includeForks: Boolean, minLinkWeight: Int, limit: Int) =
    Promise.successful {
      val timer = new Timer
      println( "Calculating project links with minimum weight %d (limit %d)".format(from,to,minLinkWeight,limit) )
      getFilteredCommits(from, to, langFilter, langStrict, includeForks)
        .map( kv => kv._2 ).flatten
        .log( cs => println("Done in %s.\nReduce %d commits to projects per user".format(timer.tick.nanoTimeToString,cs.size) ) )
        .reduceTo[ParMap[UserRef,Set[ProjectRef]]]
        .map( kv => kv._2 )
        .log( psets => println("Done in %s.\nReducing %d project sets to link map".format(timer.tick.nanoTimeToString, psets.size) ) )
        .par.foldLeft(Right(ThresholdCountingSet[Link](minLinkWeight)):Either[String,ThresholdCountingSet[Link]]) { (ecs,ps) =>
           ecs.right.flatMap { cs =>
             if ( cs.included.size > limit ) Left("Limit exceeded.")
             else Right( cs ++ projectsToLinksWithoutCount(ps) )
           }
         }
        .log( _.fold( _ => println( "Interrupted because limit of %d links was exceeded.".format(limit) ),
                      _ => println( "Done in %s.".format(timer.tick.nanoTimeToString) ) ) )
        .right.map ( _.included )
    }
  
  override def system = ActorSystem()
}