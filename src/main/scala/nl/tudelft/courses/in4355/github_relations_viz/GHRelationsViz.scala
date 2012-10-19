package nl.tudelft.courses.in4355.github_relations_viz

import scala.io.Source
import scala.util.matching.Regex
import scalaz._
import Scalaz._

case class User(id:Int,name:String)
case class Project(id:Int,name:String)
case class Commit(project:Project,user:User,timestamp:Int)
case class Link(p1:Project,p2:Project) {
  def normalize = {
    if ( p2.id < p1.id ) {
      Link(p2,p1)
    } else {
      this
    }
  }
}

case class JITGraphNode(id: String, name: String, adjacencies: Option[List[String]])

object GHRelationsVizApp {
  import GHRelationsViz._

  def main(args: Array[String]) = {
    println( getProjectRelations(Int.MinValue,Int.MaxValue) )
  }
  
}

object GHRelationsViz {
  val TReg = """([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+)""".r
  
  def getProjectRelations(from: Int, until: Int) = {
    val src = Source.fromURL(getClass.getResource("/commits.txt"))
    val commits = 
      readCommitsFromSource(src)
      .filter( isCommitInRange(_, from, until) )
      .take( 100000 )
    
    val projectLinks = 
      commits
      .foldLeft(Map.empty:Map[User,Set[Project]])(groupProjectsPerUser)
      .values.par
      .flatMap(getAllProjectLinks)
      .map(_.normalize);
      
    val involvedProjects =
      projectLinks
      .foldLeft(Set.empty:Set[Project])( (s,l) => s + l.p1 + l.p2 )
      
    val projectAdjacencyMap =
      projectLinks
      .foldLeft(Map.empty:Map[Project,List[Project]])(createAdjacencyMap);
      
    val graphNodes =
      zipAndMap(involvedProjects, projectAdjacencyMap)( createGraphNodeFromProjectAndLinks )
      
    graphNodes
  }
  
  def readCommitsFromSource(src: Source) =
    src
    .getLines
    .filter(isNotEmpty)
    .map(parseLine)
    .toList

  def isNotEmpty(str: String) = !(str isEmpty)
  
  def isCommitInRange(c: Commit, from: Int, until: Int) = 
    c.timestamp >= from && c.timestamp <= until

  /**
   * Parse a single line from the data into a tuple
   * @param line the input line as string
   * @return the tuple with data
   */
  def parseLine(line: String) = {
    val TReg(pId, pName, uId, uName, ts) = line
    Commit(Project(pId.toInt,pName),User(uId.toInt,uName),ts.toInt)
  }
  
/**
 * 
 * @param state
 * @param commit
 * @return
 */
def groupProjectsPerUser(state: Map[User,Set[Project]], commit: Commit): Map[User,Set[Project]] = {
    state |+| Map((commit.user,Set(commit.project)))
  }

/**
 * @param ps
 * @return
 */
def getAllProjectLinks(ps: Set[Project]) = {
    ps
    .subsets(2)
    .map( (ss) => {
        val l = ss.toList
        Link(l(0),l(1))
      } )
  }
  
/**
 * 
 * @param state
 * @param link
 * @return
 */
def createAdjacencyMap(state: Map[Project,List[Project]], link: Link): Map[Project,List[Project]] = {
    state |+| Map((link.p1,List(link.p2)))
  }

  
/**
 * 
 * @param as
 * @param bs
 * @param f
 * @return
 */
def zipAndMap[A,B,C](as: Set[A], bs: Map[A,B])(f: (A,Option[B]) => C): List[C] = {
    def acc(res: List[C], rest: List[A]): List[C] = rest match {
      case Nil => res
      case h::ts => acc( f(h, bs get h) :: res, ts )
    }
    acc(Nil, as.toList)
  }
  
/**
 * Convert a project and list of linked projects into a graph node object to be converted into JSON
 * @param p
 * @param as
 * @return
 */
def createGraphNodeFromProjectAndLinks(p: Project, as:Option[List[Project]]) = 
      JITGraphNode(p.id.toString, p.name, as map( _ map( _.id.toString )))

}
