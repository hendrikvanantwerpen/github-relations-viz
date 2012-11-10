package nl.tudelft.courses.in4355.github_relations_viz

import java.net.URL
import java.util.Date
import scala.io.Source
import scala.util.matching.Regex
import D3Entities._
import GHEntities._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import Logger._

class GHRelationsViz(projectsurl: URL, usersurl: URL, commitsurl: URL, period: Int) {
  import GHRelationsViz._

  println( "Reading projects" )
  val projects =
    getLines(projectsurl)
      .flatMapReduce[Map[Int,String]]( parseStringToIntString )
  
  println( "Reading users" )
  val users =
    getLines(usersurl)
      .flatMapReduce[Map[Int,String]]( parseStringToIntString ).toList
    
  println( "Reading commits" )
  val commits =
    getLines(commitsurl).flatMapReduce[Map[Int,Set[Commit]]] { l => 
      val c = parseStringToBinnedCommit(period)(l)
      c.filter( _.timestamp != 0 ).map( c => (c.timestamp,c) )
    }

  println( "Calculating limits" )
  val limits = Range(
      (Int.MaxValue /: commits.keySet)( (a,ts) => math.min(a,ts) ),
      (Int.MinValue /: commits.keySet)( (a,ts) => math.max(a,ts) )
  )
  
  val epoch2000_01_01 = 946684800
  def getLimits = limits
  
  def getProjectLinks(from: Int, until: Int, minDegree: Int) = {
    println( "Calculating project links from %d until %d with minimum degree %d".format(from,until,minDegree) )
    commits
      .log( cs => println( "Filter %d time bins".format(cs.size) ) )
      .filterKeys( k => k >= from && k <= until ).values.flatten
      .log( cs => println("Reduce %d commits to projects per user".format(cs.size) ) )
      .mapReduce[Map[Int,Set[Int]]](groupProjectByUser)
      .values
      .log( psets => println( "Reducing %d project sets to link map".format(psets.size) ) )
      .flatMapReduce[Map[Link,Int]]( projectsToLinks )
      .log( lm => println( "Filter %d links by degree".format(lm.size) ) )
      .filter( _._2 >= minDegree )
  }

  def getD3Data(from: Int, until: Int, minDegree: Int) = {
    val links = 
      getProjectLinks(from, until, minDegree)
    println( "Convert project links to D3 data" )
    val projectMap =
      links
        .mapReduce[Map[Int,Int]]( t => linksToProjects(t._1).map( p => (p,1) ) )
    val involvedProjects =
      projectMap.keySet.toList
    val d3nodes =
      involvedProjects
        .map( p => D3Node(p,projects.get(p).getOrElse("Unknown project with id %d".format(p)),projectMap.get(p).getOrElse(1)) )
    val d3links =
      links
        .map( t => D3Link( involvedProjects.indexOf(t._1.pId1), involvedProjects.indexOf(t._1.pId2), t._2 ) )
    println( "Return D3 graph" )
    D3Graph(d3nodes,d3links)
  }
  
}

object GHRelationsViz {
  
  private val TReg = """([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+)""".r  
  
  def getLines(url: URL) =
    scalax.io.Resource.fromURL(url)
                      .lines()

  private val IntStringReg = """([^ ]+) ([^ ]+)""".r  
  def parseStringToIntString(str: String) = {
    try {
      val IntStringReg(id, name) = str
      Some( id.toInt -> name  )
    } catch {
      case _ => None
    }
  }  

  private val CommitReg = """([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+)""".r  
  def parseStringToBinnedCommit(period: Int)(str: String) = {
    try {
      val CommitReg(pId, pName, uId, uName, ts) = str
      val t = getBinnedTime(period)(ts.toInt)
      Some( Commit(pId.toInt,uId.toInt,t) )
    } catch {
      case _ => None
    }
  }  
    
  def getBinnedTime(period: Int)(time: Int) =
    time - (time % period)
  
  def groupProjectByUser(c: Commit) =
    (c.userId,c.projectId)

  def projectsToLinks(ps: Set[Int]) = {
    createProduct(ps).map( l => (Link(l._1,l._2).normalize,1) ) 
  }
    
  def createProduct[A](as: Set[A]): Set[(A,A)] =
    as.subsets(2)
      .map( s => (s.head,s.tail.head) )
      .toSet

  def linksToProjects(l: Link) =
    Set(l.pId1,l.pId2)
  
  def isActorTask(c: Commit, divisor: Int, remainder: Int) =
      c.userId%divisor==remainder

}
