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
import scala.collection.parallel.ParMap

class GHRelationsViz(projectsurl: URL, usersurl: URL, forksurl: URL, commitsurl: URL, period: Int) {
  import GHRelationsViz._

  println( "Reading projects" )
  val projects =
    (Map.empty[ProjectRef,Project] /: getLines(projectsurl)) { (m,l) => 
      parseStringToProject(l).map( m + _ ).getOrElse( m ) 
    }
  
  println( "Reading users" )
  val users =
    (Map.empty[UserRef,User] /: getLines(usersurl)) { (m,l) => 
      parseStringToUser(l).map( m + _ ).getOrElse( m )
    }

  println( "Reading forks" )
  val forks =
    getLines(forksurl)
      .mapReduce[Set[ProjectRef]] { l =>
        parseStringToFork(l).map( _.projectId ).toList
      }
  
  println( "Reading commits" )
  val commits =
    getLines(commitsurl)
      .mapReduce[Map[Int,Set[(UserRef,ProjectRef)]]] { l => 
        val c = parseStringToCommit(l)
        c.filter( c => c.timestamp != 0 )
         .filter( c => !forks.contains(c.projectId) )
         .map( c => (getBinnedTime(period)(c.timestamp),(c.userId,c.projectId)) )
         .toList
      }

  println( "Calculating limits" )
  val limits = Range(epoch1990,epoch2015)
      //(Int.MaxValue /: commits.keySet)( (a,ts) => math.min(a,ts) ),
      //(Int.MinValue /: commits.keySet)( (a,ts) => math.max(a,ts) )
  //)
  
  def getLimits = limits
  
  def getProjectLinks(from: Int, until: Int, minWeight: Int) = {
    println( "Calculating project links from %d until %d with minimum weight %d".format(from,until,minWeight) )
    commits
      .log( cs => println( "Filter %d time bins".format(cs.size) ) )
      .par.filter( e => e._1 >= from && e._1 <= until )
      .seq.values.flatten
      .log( cs => println("Reduce %d commits to projects per user".format(cs.size) ) )
      .par.reduceTo[Map[Int,Set[Int]]]
      .values
      .log( psets => println( "Reducing %d project sets to link map".format(psets.size) ) )
      .par.mapReduce[ParMap[Link,Int]]( projectsToLinks )
      .log( lm => println( "Filter %d links by weight".format(lm.size) ) )
      .filter( _._2 >= minWeight )
  }

  def getD3Data(from: Int, until: Int, minWeight: Int) = {
    val links = 
      getProjectLinks(from, until, minWeight)
    println( "Convert project links to D3 data" )
    val projectMap =
      links
        .par.mapReduce[Map[Int,Int]]( t => linksToProjects(t._1).map( p => (p,1) ) )
    val involvedProjects =
      projectMap.keySet.toList
    val d3nodes =
      involvedProjects
        .par.map( p => D3Node(p,
                              projects.get(p).map( p => p.name ).getOrElse("Unknown project id %d".format(p)),
                              projectMap.get(p).getOrElse(1)) )
    val d3links =
      links
        .par.map( t => D3Link( involvedProjects.indexOf(t._1.pId1), involvedProjects.indexOf(t._1.pId2), t._2 ) )
    println( "Return D3 graph" )
    D3Graph(d3nodes.seq,d3links.seq)
  }
  
}

object GHRelationsViz {

  val epoch1990 = 631148400
  val epoch2015= 1420066800  
  
  def getLines(url: URL) =
    scalax.io.Resource.fromURL(url)
                      .lines()
                      .filter( l => !l.isEmpty && !l.startsWith("#") )

  private val ProjectReg = """([^\t]+)\t([^\t]+)\t([^\t]+)\t([^\t]+)\t([^\t]*)""".r  
  def parseStringToProject(str: String) = {
    try {
      val ProjectReg(id, ownerId, name, language, description) = str
      Some( id.toInt -> Project(id.toInt, name, language, description) )
    } catch {
      case _ => { println( "Cannot parse to Project: "+str ); None }
    }
  }  

  private val UserReg = """([^\t]+)\t([^\t]*)\t([^\t]+)""".r
  def parseStringToUser(str: String) = {
    try {
      val UserReg(id, name, login) = str
      Some( id.toInt -> User(id.toInt, login, name) )
    } catch {
      case _ => { println( "Cannot parse to User: "+str ); None }
    }
  }
  
  private val CommitReg = """([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+)""".r
  def parseStringToCommit(str: String) = {
    try {
      val CommitReg(pId, pName, uId, uName, ts) = str
      Some( Commit(pId.toInt,uId.toInt,ts.toInt) )
    } catch {
      case _ => { println( "Cannot parse to Commit: "+str ); None }
    }
  }

  private val ForkReg = """([^\t]*)\t([^\t]*)""".r
  def parseStringToFork(str: String) = {
    try {
      val ForkReg(projectId, parentId) = str
      Some( Fork(projectId.toInt, parentId.toInt) )
    } catch {
      case _ => { println( "Cannot parse to Fork: "+str ); None }
    }
  }  
  
  def getBinnedTime(period: Int)(time: Int) =
    time - (time % period)
  
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
