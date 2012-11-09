package nl.tudelft.courses.in4355.github_relations_viz.stats

import java.net.URL
import java.util.Date

import scala.collection.immutable.SortedMap

import scalaz._
import Scalaz._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
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

class Logger(name: String) {
  val logwriter = scalax.io.Resource.fromFile(name+".log").writer
  def log(msg: Any) = { print(msg);trace(msg) }
  def logln(msg: Any) = { println(msg);traceln(msg) }
  def trace(msg: Any) = logwriter.write(msg.toString)
  def traceln(msg: Any) = logwriter.write(msg.toString+"\n")
}

object GHRelationsStats extends App {

  val epoch_20100101 = 1262300400
  val epoch_20100701 = 1277935200
  val epoch_20110101 = 1293836400
  val epoch_20110701 = 1309471200

  val PERIOD    = 7 * 24 * 3600
  val FROM      = epoch_20100701
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

  def timed[A](msg: String, l: String => Unit)(f: => A): A = {
    l(msg+" ...")
    val start = System.nanoTime()
    val result: A = f
    val end = System.nanoTime()
    val dt = end-start
    if ( dt > 1e9 ) {
      l("done in %.2fs".format(dt/1e9))
    } else if ( dt > 1e6 ) {
      l("done in %.2fms".format(dt/1e6))
    } else {
      l("done in %.2fus".format(dt/1e3))
    }
    result
  }

  def test(name: String)(f: (TraversableOnce[String],TraversableOnce[String],TraversableOnce[String],Logger) => Unit) = {
    val commitlines = getLines(commitsurl)
    val projectlines = getLines(projectsurl)
    val userlines = getLines(usersurl)
    val logger = new Logger(name)
    logger.logln("Starting test: "+name)
    logger.logln("")
    f(commitlines,projectlines,userlines,logger)
  }

  // TESTS

  test("test 1") { (commitlines,projectlines,userlines,logger) =>

    logger.logln("Build project map")
    val projects = timed("reading",logger.logln) {
      getLines(projectsurl).flatMapReduce[Map[Int,String]]( parseStringToIntString )
    }
    logger.logln("Found "+projects.size+" projects")

    logger.logln("Build user map")
    val users = timed("reading",logger.logln) {
      getLines(usersurl).flatMapReduce[Map[Int,String]]( parseStringToIntString )
    }
    logger.logln("Found "+users.size+" users")

    logger.logln("Read commits")
    val cs = timed("reading",logger.logln) {
      getLines(commitsurl).flatMap( parseStringToBinnedCommit )
    }

    logger.logln("Reduce commits to timeindexed Map")
    val (total,bcm) = timed("reducing",logger.logln) {
      cs.mapReduce[(Int,Map[Int,Set[Commit]])]( c => {
        val bc = getBinnedCommit(PERIOD)(c)
        (1,(bc.timestamp,bc))
      } )
    }
    val n = (0 /: bcm)( _ + _._2.size )
    val p = (n.toDouble/total.toDouble)*100.0
    logger.logln("Reduced "+total+" commits to "+n+" ("+p+"%) in "+bcm.size+" bins")

    val weekCommitHist = bcm.mapReduce[SortedMap[Int,Int]]( e => e._1 -> e._2.size )
    logger.traceln("Commits per week")
    weekCommitHist.foreach( logger.traceln )

    val fcs = timed("filter time",logger.logln) {
      bcm.filterKeys( k => k >= FROM && k <= TO ).values.flatten
    }
    logger.logln("Kept "+fcs.size+" commits after time filter")

    val ups = timed("group projects by user",logger.logln) {
      fcs.mapReduce[Map[Int,Set[Int]]](groupProjectByUser)
    }

    val upHist = ups.mapReduce[SortedMap[Int,Int]]( e => e._2.size -> 1 )
    logger.traceln("Projects per users histogram")
    upHist.foreach( logger.traceln )
    val totalLinks = (0 /: upHist)( (t,e) => {
        val n = e._1
        val nl = ((n+1)*n)/2
        t + (nl * e._2)
    } )
    logger.logln("Total project links would be "+totalLinks)

    val links = timed("calculating project links",logger.logln) {
      ups.values
         .flatMapReduce[Map[Link,Int]]( ps =>
            createProduct(ps).map( l => (Link(l._1,l._2).normalize,1) ) )
    }
    logger.logln("Total of "+links.size+" project links")

    val linkHist = links.mapReduce[SortedMap[Int,Int]]( l => l._2 -> 1 )
    logger.traceln("Link degree histogram")
    linkHist.foreach( logger.traceln )

    logger.logln("Done")

  }
  test("test 2") { (commitlines,projectlines,userlines,logger) =>

    logger.logln("Build project map")
    val projects = timed("reading",logger.logln) {
      getLines(projectsurl).flatMapReduce[Map[Int,String]]( parseStringToIntString )
    }
    logger.logln("Found "+projects.size+" projects")

    logger.logln("Build user map")
    val users = timed("reading",logger.logln) {
      getLines(usersurl).flatMapReduce[Map[Int,String]]( parseStringToIntString )
    }
    logger.logln("Found "+users.size+" users")

    logger.logln("Read commits")
    val cs = timed("reducing",logger.logln) {
      getLines(commitsurl).flatMap( parseStringToBinnedCommit )
    }

    logger.logln("Reduce commits to set")
    val (total,bcs) = timed("reducing",logger.logln) {
      cs.mapReduce[(Int,Set[Commit])]( c => {
        val bc = getBinnedCommit(PERIOD)(c)
        (1,bc)
      } )
    }
    val n = bcs.size
    val p = (n.toDouble/total.toDouble)*100.0
    logger.logln("Reduced "+total+" commits to "+n)

    val fcs = timed("filter time",logger.logln) {
      bcs.filter( c => c.timestamp >= FROM && c.timestamp <= TO )
    }
    logger.logln("Kept "+fcs.size+" commits after time filter")

    val ups = timed("group projects by user",logger.logln) {
      fcs.mapReduce[Map[Int,Set[Int]]](groupProjectByUser)
    }

    val links = timed("calculating project links",logger.logln) {
      ups.values
         .flatMapReduce[Map[Link,Int]]( ps =>
            createProduct(ps).map( l => (Link(l._1,l._2).normalize,1) ) )
    }
    logger.logln("Total of "+links.size+" project links")

    logger.logln("Done")

  }

}
