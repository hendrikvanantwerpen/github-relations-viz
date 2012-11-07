package nl.tudelft.courses.in4355.github_relations_viz.stats

import java.net.URL
import java.util.Date

import scala.collection.immutable.SortedMap

import scalaz._
import Scalaz._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.CollectionAggregators._
import net.van_antwerpen.scala.collection.mapreduce.ValueAggregators._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._

case class User(id:Int,name:String)
case class Project(id:Int,name:String)
case class Commit(project:Int,user:Int,timestamp:Int)
case class Link(p1:Int,p2:Int) {
    def normalize = {
      if ( p2 < p1 ) {
        Link(p2,p1)
      } else {
        this
      }
    }
  }

object GHRelationsStats extends App {

  val epoch_20100101 = 1262300400
  val epoch_20110101 = 1293836400

  val PERIOD    = 7 * 24 * 3600
  val FROM      = epoch_20100101
  val TO        = epoch_20110101
  val MINDEGREE = 2

  // GENERAL FUNCTIONS

  val commitsurl = getClass.getResource("/commits.txt")
  val usersurl = getClass.getResource("/users.txt")
  val projectsurl = getClass.getResource("/projects.txt")
  
  def getLines(url: URL) =
    scalax.io.Resource.fromURL(url)
                      .lines()

  def getBinnedCommit(period: Int)(c: Commit) =
    c.copy(timestamp = getBinnedTime(period)(c.timestamp))

  def getBinnedTime(period: Int)(time: Int) =
    time - (time % period)

  def createProduct[A](as: Set[A]): Set[(A,A)] =
      if ( as.size > 1 ) {
        val h = as.head
        val t = as.tail
        t.map( (h,_) ) ++ createProduct( t )
      } else {
        Set.empty
      }

  val logres = scalax.io.Resource.fromFile("testresults.txt").writer
  def printlogln(a: Any) = {
    println(a)
    logln(a)
  }
  def log(a: Any) = logres.write(a.toString)
  def logln(a: Any) = logres.write(a.toString+"\n")

  def timed[A](msg: String)(f: => A): A = {
    printlogln(msg+" ...")
    val start = System.nanoTime()
    val result: A = f
    val end = System.nanoTime()
    printlogln("done in "+((end-start)/1e6)+"ms")
    result
  }


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
  def parseStringToBinnedCommit(str: String) = {
    try {
      val CommitReg(pId, pName, uId, uName, ts) = str
      val t = getBinnedTime(PERIOD)(ts.toInt)
      Some( Commit(pId.toInt,uId.toInt,t) )
    } catch {
      case _ => None
    }
  }  

  def groupProjectByUser(c: Commit) =
    (c.user,c.project)

  def binCommitByPeriod(period: Int)(c: Commit) =
    c.copy(timestamp = c.timestamp - (c.timestamp % period))

  // TEST

  printlogln("Build project map")
  val projects = timed("reading") {
    getLines(projectsurl).flatMapReduce[Map[Int,String]]( parseStringToIntString )
  }
  printlogln("Found "+projects.size+" projects")

  printlogln("Build user map")
  val users = timed("reading") {
    getLines(usersurl).flatMapReduce[Map[Int,String]]( parseStringToIntString )
  }
  printlogln("Found "+users.size+" users")

  printlogln("Read commits")
  val cs = timed("reducing") {
    getLines(commitsurl).flatMap( parseStringToBinnedCommit )
  }

  printlogln("Reduce commits to timeindexed Map")
  val (total,bcm) = timed("reducing") {
    cs.mapReduce[(Int,Map[Int,Set[(Commit,Int)]])]( c => {
      val bc = getBinnedCommit(PERIOD)(c)
      (1,(bc.timestamp,(bc,1)))
    } )
  }
  val n = (0 /: bcm)( _ + _._2.size )
  val p = (n.toDouble/total.toDouble)*100.0
  printlogln("Reduced "+total+" commits to "+n+" ("+p+"%) in "+bcm.size+" bins")

  val weekCommitHist = bcm.mapReduce[SortedMap[Int,Int]]( e => e._1 -> e._2.size )
  logln("Commits per week")
  weekCommitHist.foreach( logln )

  val fcs = timed("filter time") {
    bcm.filterKeys( k => k >= FROM && k <= TO ).values.flatten.map( _._1 )
  }
  printlogln("Kept "+fcs.size+" commits after time filter")

  val ups = timed("group projects by user") {
    fcs.mapReduce[Map[Int,Set[Int]]](groupProjectByUser)
  }

  val upHist = ups.mapReduce[SortedMap[Int,Int]]( e => e._2.size -> 1 )
  logln("Projects per users histogram")
  upHist.foreach( logln )
  val totalLinks = (0 /: upHist)( (t,e) => {
      val n = e._1
      val nl = ((n+1)*n)/2
      t + (nl * e._2)
  } )
  printlogln("Total project links would be "+totalLinks)

  val links = timed("calculating project links") {
    ups.values
       .flatMapReduce[Map[Link,Int]]( ps =>
          createProduct(ps).map( l => (Link(l._1,l._2).normalize,1) ) )
  }
  printlogln("Total of "+links.size+" project links")

  val linkHist = links.mapReduce[SortedMap[Int,Int]]( l => l._2 -> 1 )
  logln("Link degree histogram")
  linkHist.foreach( logln )

  printlogln("Done")

}
