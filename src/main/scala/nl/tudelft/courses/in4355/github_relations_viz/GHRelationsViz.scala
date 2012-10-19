package nl.tudelft.courses.in4355.github_relations_viz

import scala.io.Source
import scala.util.matching.Regex
import scalaz._
import Scalaz._

import GHEntities._
import JITEntities._
import MapReduce._

class Timer {
  var t = 0l
  reset
  private def reset = {
    t = System.currentTimeMillis
    t
  }
  def tick(s: String) = {
    val t0 = t
    val t1 = reset
    val dt = t1 - t0
    println(s+": "+dt+"ms")
  }
}

object GHRelationsViz {
  
  val TReg = """([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+)""".r
  
  def getProjectRelations(from: Int, until: Int) = {
    val t = new Timer
    val src = Source.fromURL(getClass.getResource("/commits.txt"))
    t.tick("created source")
    val commits = readCommitsFromSource(src)
    t.tick("read commits")
    val commitsInRange = commits.filter( isCommitInRange(_, from, until) )
    t.tick("filtered commits")
    val limitedCommits = commitsInRange.take( 1000000 )
    t.tick("took small portion of commits commits")
    val userProjects = mapReduce(commitToUserProject)(limitedCommits toList)
    t.tick("mapped projects to users")
    val projectLinks = mapReduce(getAllNormalizedProjectLinks)(userProjects.values)
    t.tick("created all project links")
    val involvedProjects = mapReduce(getProjectsFromLink)(projectLinks)
    t.tick("iterated all involved projects")
    val projectAdjacencyMap = mapReduce(createAdjacencyMap)(projectLinks)
    t.tick("created project adjecancy map")
    val projectAndOptAdjecancyMap = mapReduce(zipWithOption(projectAdjacencyMap))(involvedProjects)
    t.tick("zipped projects with optional adjecancy")
    val graphNodes = mapReduce(createGraphNodeFromProjectAndLinks)(projectAndOptAdjecancyMap)
    t.tick("created graph nodes")
    graphNodes
  }
  
  def readCommitsFromSource(src: Source) =
    src
    .getLines
    .filter(isNotEmpty)
    .map(parseStringToCommit)

  def isNotEmpty(str: String) = !(str isEmpty)
  
  def parseStringToCommit(line: String) = {
    val TReg(pId, pName, uId, uName, ts) = line
    Commit(Project(pId.toInt,pName),User(uId.toInt,uName),ts.toInt)
  }  
  
  def isCommitInRange(c: Commit, from: Int, until: Int) = 
    c.timestamp >= from && c.timestamp <= until

  def commitToUserProject(c: Commit): Map[User,Set[Project]] =
    Map(c.user -> Set(c.project))

  def getProjectsFromLink(l: Link): Set[Project] = {
    Set(l.p1,l.p2)
  }
    
  def getAllNormalizedProjectLinks(ps: Set[Project]): List[Link] =
    ps
    .subsets(2)
    .map( (ss) => {
        val l = ss.toList
        Link(l(0),l(1)).normalize
    } ) toList
  
  def createAdjacencyMap(link: Link): Map[Project,List[Project]] = {
    Map((link.p1,List(link.p2)))
  }

  def zipWithOption[A,B](os: Map[A,B])(x: A): Map[A,Option[B]] =
    Map(x -> os.get(x))
  
  def createGraphNodeFromProjectAndLinks(pl: (Project,Option[List[Project]])): Set[JITGraphNode] =
      Set(JITGraphNode(pl._1.id.toString, pl._1.name, pl._2.map( _ map( _.id.toString ))))

}
