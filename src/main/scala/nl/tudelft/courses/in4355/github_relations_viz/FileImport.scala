package nl.tudelft.courses.in4355.github_relations_viz

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

case class GraphNode(id: String, name: String, adjacencies: Option[List[String]])

object FileImport {
	val TReg = """(\w+),(\w+),(\w+),(\w+),(\w+)""".r
	
	type ProjectsPerUser =  Map[User, Set[Project]]
	type ProjectLinks =  Map[Project, List[Project]]

	def main(args: Array[String]) {
	  println( getProjectRelations(Int.MinValue,Int.MaxValue) )
	}
	
	def getProjectRelations(t0: Int, t1: Int) = {
      val commits = 
        readFile("gittext.gittext").toList.par
	    .filter(isNotEmpty)
	    .map(parseLine)
	    .filter( c => c.timestamp >= t0 && c.timestamp <= t1 );
      
      val projectLinks = 
        commits.foldLeft(Map.empty:ProjectsPerUser)(groupProjectsPerUser)
	    .values.par
	    .flatMap(getAllProjectLinks)
	    .map(_.normalize);
	    
      val involvedProjects =
        projectLinks.foldLeft(Set.empty:Set[Project])( (s,l) => s + l.p1 + l.p2 )
      
	  val projectAdjacencyMap =
	    projectLinks.foldLeft(Map.empty:ProjectLinks)(createAdjacencyMap);
      
      val graphNodes =
        myZip(involvedProjects, projectAdjacencyMap)( (p,as) => GraphNode(p.id.toString, p.name, as map( _ map( _.id.toString ))) )
      
      graphNodes
	}
	
	def myZip[A,B,C](as: Set[A], bs: Map[A,B])(f: (A,Option[B]) => C): List[C] = {
	  def acc(res: List[C], rest: List[A]): List[C] = rest match {
	    case Nil => res
	    case h::ts => acc(f(h, bs get h) :: res, ts)
	  }
	  acc(Nil, as.toList)
	}
	
	def readFile(filename: String) = {
	  scala.io.Source.fromFile(filename).getLines
	}
	
	def isNotEmpty(str: String) = !(str isEmpty)
	
	/**
	 * Parse a single line from the data into a tuple
	 * @param line the input line as string
	 * @return the tuple with data
	 */
	def parseLine(line: String) = {
	  val TReg(pId, pName, uId, uName, ts) = line
	  Commit(Project(pId.toInt,pName),User(uId.toInt,uName),ts.toInt)
	}
	
	def groupProjectsPerUser(state: ProjectsPerUser, commit: Commit): ProjectsPerUser = {
	  state |+| Map((commit.user,Set(commit.project)))
	}

	def getAllProjectLinks(ps: Set[Project]) = {
	  ps
	  .subsets(2)
	  .map( (ss) => {
	      val l = ss.toList
	      Link(l(0),l(1))
	    } )
	}
	
	def createAdjacencyMap(state: ProjectLinks, link: Link): ProjectLinks = {
	  state |+| Map((link.p1,List(link.p2)))
	}

}