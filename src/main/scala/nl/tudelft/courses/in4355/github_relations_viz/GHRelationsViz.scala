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

class GHRelationsViz(projectsurl: URL, usersurl: URL, forksurl: URL, commitsurl: URL, minFrom: Int, maxUntil: Int, period: Int) {
  import GHRelationsViz._

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
  
  println( "Calculating range" )
  val timeBins = commits.seq.keys
  val limits = Range(timeBins.min,timeBins.max,period)

  def getProjectLinks(from: Int, until: Int, minWeight: Int) = {
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

object GHRelationsViz {

  def getLines(url: URL) =
    scalax.io.Resource.fromURL(url)
                      .lines()
                      .filter( l => !l.isEmpty && !l.startsWith("#") )

  def notNULL(s: String) = {
    if ( s.toLowerCase == "null" ) "" else s
  }

  def readUsers(url: URL) =
    (Map.empty[UserRef,User] /: getLines(url)) { (m,l) => 
      parseStringToUser(l).map( m + _ ).getOrElse( m )
    }
  
  private val ProjectReg = """([^\t]+)\t([^\t]+)\t([^\t]+)\t([^\t]+)\t([^\t]*)""".r  
  def parseStringToProject(str: String) = {
    try {
      val ProjectReg(id, ownerId, name, language, description) = str
      Some( id.toInt -> Project(id.toInt, ownerId.toInt, notNULL(name), notNULL(language), notNULL(description)) )
    } catch {
      case _ => { println( "Cannot parse to Project: "+str ); None }
    }
  }  

  def readProjects(url: URL) =
    (Map.empty[ProjectRef,Project] /: getLines(url)) { (m,l) => 
      parseStringToProject(l).map( m + _ ).getOrElse( m )
    }
  
  private val UserReg = """([^\t]+)\t([^\t]*)\t([^\t]+)""".r
  def parseStringToUser(str: String) = {
    try {
      val UserReg(id, name, login) = str
      Some( id.toInt -> User(id.toInt, notNULL(login), notNULL(name)) )
    } catch {
      case _ => { println( "Cannot parse to User: "+str ); None }
    }
  }
  
  def readCommits(url: URL) = 
    getLines(url)
      .flatMap( parseStringToCommit )
  
  private val CommitReg = """([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+)""".r
  def parseStringToCommit(str: String) = {
    try {
      val CommitReg(pId, pName, uId, uName, ts) = str
      Some( Commit(pId.toInt,uId.toInt,ts.toInt) )
    } catch {
      case _ => { println( "Cannot parse to Commit: "+str ); None }
    }
  }

  def readForks(url: URL) =
    getLines(url)
      .mapReduce[Set[ProjectRef]] { l =>
        parseStringToFork(l).map( _.project ).toList
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
  
  def projectsToLinks(ps: Set[ProjectRef]) = {
    createProduct(ps).map( l => (Link(l._1,l._2).normalize,1) )
  }
    
  def createProduct[A](as: Set[A]): Set[(A,A)] =
    as.subsets(2)
      .map( s => (s.head,s.tail.head) )
      .toSet

}
