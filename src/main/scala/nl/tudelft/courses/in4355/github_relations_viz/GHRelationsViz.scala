package nl.tudelft.courses.in4355.github_relations_viz

import java.net.URL
import java.util.Date
import scala.io.Source
import scala.util.matching.Regex
import D3Entities._
import GHEntities._
import JITEntities._
import net.van_antwerpen.scala.collection.mapreduce.Monoid._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import Timer._

class GHRelationsViz(url: URL) {
  import GHRelationsViz._

  val PERIOD = 7 * 24 * 3600
  
  val commits =
    readCommitsFromURL(url)
      .mapReduce[Set[Commit]](binCommitByPeriod(PERIOD))

  /*val limits = timed("calculate limits") { Range(
      (Int.MaxValue /: commits)( (a,c) => math.min(a,c.timestamp) ),
      (Int.MinValue /: commits)( (a,c) => math.max(a,c.timestamp) )
  ) }*/
  
  val epoch2000_01_01 = 946684800
  def getLimits = 
    Range(
      (new Date(epoch2000_01_01).getTime()/1000).toInt,
      (new Date().getTime()/1000).toInt
    )
  
  def getProjectLinks(from: Int, until: Int, minDegree: Int) =
    commits
      .filter( c => isCommitInRange(c, from, until) )
      .mapReduce[Map[User,Set[Project]]](groupProjectByUser)
      .values
      .flatMapReduce[Map[Link,Int]](projectsToLinks)
      .filter( _._2 >= minDegree )
  
  def getJITData(from: Int, until: Int, minDegree: Int) = {
    val links =
      getProjectLinks(from, until, minDegree)
    val (involvedProjects,projectAdjacencyMap) = ( 
      links.keySet
        .mapReduce[(Set[Project],Map[Project,Set[Project]])]
                  (linksToProjectsAndAdjacencyMap)
    )
    val projectAndOptAdjecancyMap = (
      involvedProjects
        .mapReduce[Map[Project,Option[Set[Project]]]]
                  (zipWithOption(projectAdjacencyMap))
    )
    val graphNodes = 
      projectAndOptAdjecancyMap
        .mapReduce[Set[JITNode]](projectWithAdjacanciesToJITNode)
    graphNodes
  }
  
  def getD3Data(from: Int, until: Int, minDegree: Int) = {
    val links = 
      getProjectLinks(from, until, minDegree)
    val involvedProjects =
      links.keySet
        .flatMapReduce[Set[Project]](linksToProjects)
        .toList
    val d3nodes =
      involvedProjects
        .map( projectToD3Node )
    val d3links =
      links
        .map( t => linkToD3Link(involvedProjects)(t._1, t._2) )
    D3Graph(d3nodes,d3links)
  }
  
}

object GHRelationsViz {
  
  private val TReg = """([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+)""".r  
  
  def readCommitsFromURL(url: URL) =
    scalax.io.Resource.fromURL(url)
      .lines()
      .flatMap( parseStringToCommit )

  def parseStringToCommit(str: String) = {
    try {
      val TReg(pId, pName, uId, uName, ts) = str
      Some(Commit(Project(pId.toInt,pName),User(uId.toInt,uName),ts.toInt))      
    } catch {
      case _ => None
    }
  }  

  def isCommitInRange(c: Commit, from: Int, until: Int) = 
    c.timestamp >= from && c.timestamp <= until

  def groupProjectByUser(c: Commit) =
    (c.user,c.project)

  def binCommitByPeriod(period: Int)(c: Commit) =
    c.copy(timestamp = c.timestamp - (c.timestamp % period))

  def projectsToLinks(ps: Set[Project]) = 
    ps.subsets(2)
      .map( (ss) => {
        val l = ss.toList
        (Link(l(0),l(1)).normalize,1)
      } )
  
  def linksToProjectsAndAdjacencyMap(l: Link) =
    (Set(l.p1,l.p2),(l.p1,l.p2))
  
  def zipWithOption[A,B](lookup: Map[A,B])(a: A) =
    (a -> lookup.get(a))

  def projectWithAdjacanciesToJITNode(pl: (Project,Option[Set[Project]])) = 
    JITNode(
      pl._1.id.toString,
      pl._1.name,
      pl._2.map(_ map( _.id.toString ))
    )

  def linksToProjects(l: Link) =
    Set(l.p1,l.p2)

  def projectToD3Node(p: Project) =
    new D3Node(p.name)
  
  def linkToD3Link(lookup: Seq[Project])(l: Link, v: Int) =
    new D3Link( lookup.indexOf(l.p1), lookup.indexOf(l.p2), v )
    
}
