package nl.tudelft.courses.in4355.github_relations_viz

import java.net.URL
import java.util.Date
import scala.io.Source
import scala.util.matching.Regex
import GHEntities._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import util.Logger._
import scala.collection.immutable.SortedMap
import scala.collection.parallel.ParMap
import scala.collection.{GenMap,GenIterable}
import akka.dispatch.Future
import akka.actor.ActorSystem
import nl.tudelft.courses.in4355.github_relations_viz.util.Logger

trait GHRelationsViz {
  def getProjectLinks(from: Int, to: Int, langFilter: Map[String,Boolean], langStrict: Boolean, includeForks: Boolean, minLinkWeight: Int, limit: Int): Future[Either[String,GenMap[Link,Int]]]
  def getUser(id: UserRef): User
  def getProject(id: ProjectRef): Project
  def getParentProject(id: ProjectRef): Option[ProjectRef]
  def getUserProjectsLinksPerWeek(from: Int, to: Int, langFilter: Map[String,Boolean], langStrict: Boolean, includeForks: Boolean): Future[GenIterable[(Int,Int)]]
  def getLanguages(from: Int, to: Int, includeForks: Boolean): Future[GenIterable[(String,Int)]]
  def system: ActorSystem
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
      .mapReduce[Map[ProjectRef,ProjectRef]] { l =>
        parseStringToFork(l).map( f => (f.project -> f.parent) ).toList
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

  def projectsToLinksWithoutCount(ps: Set[ProjectRef]) = {
    createProduct(ps).map( l => Link(l._1,l._2).normalize )
  }  
  
  def createProduct[A](as: Set[A]): Set[(A,A)] =
    as.subsets(2)
      .map( s => (s.head,s.tail.head) )
      .toSet

}
