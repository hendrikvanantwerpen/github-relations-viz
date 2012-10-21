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
  
  val commits = timed("read and reduce commits") {
    val cs = readCommitsFromURL(url)
    mapReduce(new BinCommitsByPeriod(PERIOD))(cs)
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
    
    val userProjects = timed("group projects per user") { mapReduce(GroupProjectsByUserStep)(commitsInRange) }
    println("found "+userProjects.size+" users with projects")
    
    val projectLinkMap = timed("get all project links") { mapReduce(ProjectsToLinkMap)(userProjects.values) }
    println("found "+projectLinkMap.size+" project links")

    val bigProjectLinks = timed("filter links by degree") { projectLinkMap.filter( _._2 >= minDegree ) }
    println(bigProjectLinks.size+" links with degree >= "+minDegree+" left")
    
    bigProjectLinks
  }
  
  def getJITData(from: Int, until: Int, minDegree: Int) = {
    val links = getProjectLinks(from, until, minDegree).keySet
    
    val (involvedProjects,projectAdjacencyMap) = timed("build adjacency map") { mapReduce(LinksToProjectsAndAdjacencyMap)(links) }
    println("total involved projects is "+involvedProjects.size)
    println("total projects for adjacancies is "+projectAdjacencyMap.size)
    
    val projectAndOptAdjecancyMap = timed("create full graph map") { mapReduce(new ZiptWithOption(projectAdjacencyMap))(involvedProjects) }
    println("zipped projects with adjacancies")
    
    val graphNodes = mapReduce(GraphMapToGraphNode)(projectAndOptAdjecancyMap)
    println("created "+graphNodes.size+" graph nodes")
    
    graphNodes
  }
  
  def getProtovisData(from: Int, until: Int, minDegree: Int) = {
    import net.liftweb.json.JsonDSL._
    val links = getProjectLinks(from, until, minDegree)
    
    val involvedProjects = timed("get all projects") { mapReduce(LinksToProjects)(links.keySet).toList }
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

  def makePeriodCommits(w: Int)(c: Commit): Map[Int,Map[Commit,Int]] = {
    val t = roundOnPeriod(w)(c.timestamp)
    Map(t -> Map(c.copy(timestamp = t) -> 1))
  }
    
  def roundOnPeriod(w: Int)(t: Int): Int = t - (t % w)

  object GroupProjectsByUserStep extends MapReduceStep[Commit,(User,Project),Map[User,Set[Project]]] {
    override def zero = Map.empty
    override def mapper(c: Commit) = (c.user,c.project)
    override def reducer(m: Map[User,Set[Project]], p:(User,Project)) =
      m + (p._1 -> m.get( p._1 ).map( _ + p._2 ).getOrElse( Set(p._2) ))
  }
  
  class BinCommitsByPeriod(period: Int) extends MapReduceStep[Commit,Commit,Set[Commit]] {
    override def zero = Set.empty
    override def mapper(c: Commit) = c.copy(timestamp = c.timestamp - (c.timestamp % period))
    override def reducer(cs: Set[Commit], c: Commit) = 
      cs + c
  }
  
  object ProjectsToLinkMap extends MapReduceStep[Set[Project],Iterator[Link],Map[Link,Int]] {
    override def zero = Map.empty
    override def mapper(ps: Set[Project]) =
      ps.subsets(2)
      .map( (ss) => {
        val l = ss.toList
        Link(l(0),l(1)).normalize
      } )
    override def reducer(lm: Map[Link,Int], ls: Iterator[Link]) =
      ls.foldLeft(lm)( (m,l) => m + (l -> m.get( l ).map( _ + 1 ).getOrElse( 1 )) )
  }
  
  object LinksToProjectsAndAdjacencyMap extends MapReduceStep[Link,Link,(Set[Project],Map[Project,Set[Project]])] {
    override def zero = (Set.empty,Map.empty)
    override def mapper(l: Link) = l
    override def reducer(t: (Set[Project],Map[Project,Set[Project]]), l: Link) = {
      val ps = t._1
      val am = t._2
      (ps + l.p1 + l.p2, am + (l.p1 -> am.get( l.p1 ).map( _ + l.p2 ).getOrElse( Set(l.p2) )))
    }
  }

  object LinksToProjects extends MapReduceStep[Link,Link,Set[Project]] {
    override def zero = Set.empty
    override def mapper(l: Link) = l
    override def reducer(ps: Set[Project], l: Link) = {
      ps + l.p1 + l.p2
    }
  }  
  
  class ZiptWithOption[A,B](lookup: Map[A,B]) extends MapReduceStep[A,A,Map[A,Option[B]]] {
    override def zero = Map.empty
    override def mapper(a: A) = a
    override def reducer(m: Map[A,Option[B]], a: A) =
      m + (a -> lookup.get(a))
  }
    
  object GraphMapToGraphNode extends MapReduceStep[(Project,Option[Set[Project]]),(Project,Option[Set[Project]]),Set[JITGraphNode]] {
    override def zero = Set.empty
    override def mapper(pl: (Project,Option[Set[Project]])) = pl
    override def reducer(gns: Set[JITGraphNode], e: (Project,Option[Set[Project]])) =
      gns + JITGraphNode(
    		   e._1.id.toString, 
    		   e._1.name, 
    		   e._2.map(_ map( _.id.toString ))
    		)
  }

}
