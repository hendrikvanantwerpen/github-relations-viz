package nl.tudelft.courses.in4355.github_relations_viz

import java.net.URL
import java.util.Date
import scala.io.Source
import scala.util.matching.Regex
import GHEntities._
import JITEntities._
import ProtovisEntities._
import MapReduce._
import Timer._

class GHRelationsViz(url: URL) {
  import GHRelationsViz._

  val PERIOD = 7 * 24 * 3600
  val LIMIT = 5e6.toInt
  
  val commits = timed("read and reduce commits") {
    val cs = readCommitsFromURL(url).take( LIMIT )
    mapReduce[Commit,Commit,Set[Commit]](binCommitByPeriod(PERIOD))(cs)
  }
  println("processing "+commits.size+" commits")

  /*val limits = timed("calculate limits") { Range(
      (Int.MaxValue /: commits)( (a,c) => math.min(a,c.timestamp) ),
      (Int.MinValue /: commits)( (a,c) => math.max(a,c.timestamp) )
  ) }*/
  
  val epoch2000_01_01 = 946684800
  def getLimits = Range((new Date(epoch2000_01_01).getTime()/1000).toInt, (new Date().getTime()/1000).toInt)
  
  def getProjectLinks(from: Int, until: Int, minDegree: Int) = {
    val commitsInRange = timed("filter commits") { commits.filter( c => isCommitInRange(c, from, until) ) }
    println("found "+commitsInRange.size+" commits in range")
    
    val userProjects = timed("group projects per user") { mapReduce[Commit,(User,Project),Map[User,Set[Project]]](groupProjectByUser)(commitsInRange) }
    println("found "+userProjects.size+" users with projects")
    
    val projectLinkMap = timed("get all project links") { mapReduceI[Set[Project],(Link,Int),Map[Link,Int]](projectsToLinks)(userProjects.values) }
    println("found "+projectLinkMap.size+" project links")

    val bigProjectLinks = timed("filter links by degree") { projectLinkMap.filter( _._2 >= minDegree ) }
    println(bigProjectLinks.size+" links with degree >= "+minDegree+" left")
    
    bigProjectLinks
  }
  
  def getJITData(from: Int, until: Int, minDegree: Int) = {
    val links = getProjectLinks(from, until, minDegree).keySet
    
    val (involvedProjects,projectAdjacencyMap) = timed("build adjacency map") { mapReduce[Link,(Set[Project],(Project,Project)),(Set[Project],Map[Project,Set[Project]])](linksToProjectsAndAdjacencyMap)(links) }
    println("total involved projects is "+involvedProjects.size)
    println("total projects for adjacancies is "+projectAdjacencyMap.size)
    
    val projectAndOptAdjecancyMap = timed("create full graph map") { mapReduce[Project,(Project,Option[Set[Project]]),Map[Project,Option[Set[Project]]]](zipWithOption(projectAdjacencyMap))(involvedProjects) }
    println("zipped projects with adjacancies")
    
    val graphNodes = mapReduce[(Project,Option[Set[Project]]),JITGraphNode,Set[JITGraphNode]](graphMapToGraphNode)(projectAndOptAdjecancyMap)
    println("created "+graphNodes.size+" graph nodes")
    
    graphNodes
  }
  
  def getProtovisData(from: Int, until: Int, minDegree: Int) = {
    import net.liftweb.json.JsonDSL._
    val links = getProjectLinks(from, until, minDegree)
    
    val involvedProjects = timed("get all projects") { mapReduceI[Link,Project,Set[Project]](linksToProjects)(links.keySet).toList }
    println("total involved projects is "+involvedProjects.size)
    
    val pvnodes = involvedProjects.map( p => {
      ("name" -> p.name)
    } )
    val pvlinks = links.map( t => {
      val l = t._1
      val v = t._2
       ("source" -> involvedProjects.indexOf(l.p1)) ~ ("target" -> involvedProjects.indexOf(l.p2)) ~ ("value" -> v)
    } )
    
    ("nodes" -> pvnodes) ~ ("links" -> pvlinks)
  }
  
}

object GHRelationsViz {
  
  private val TReg = """([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+)""".r  
  
  def readCommitsFromURL(url: URL) =
    scalax.io.Resource.fromURL(url)
    .lines()
    .filter(isNotEmpty)
    .map( parseStringToCommit )

  def isNotEmpty(str: String) = !(str isEmpty)
  
  def parseStringToCommit(str: String) = {
    try {
      val TReg(pId, pName, uId, uName, ts) = str
      Commit(Project(pId.toInt,pName),User(uId.toInt,uName),ts.toInt)      
    } catch {
      case ne: NumberFormatException => throw new Exception("Cannot parse: "+str)
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
    
  def graphMapToGraphNode(pl: (Project,Option[Set[Project]])) = 
    JITGraphNode(
         pl._1.id.toString,
         pl._1.name,
         pl._2.map(_ map( _.id.toString ))
    )

  def linksToProjects(l: Link) =
    Set(l.p1,l.p2)
  
}
