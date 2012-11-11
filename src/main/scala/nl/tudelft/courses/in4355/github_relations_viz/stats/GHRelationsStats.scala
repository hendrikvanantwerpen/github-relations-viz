package nl.tudelft.courses.in4355.github_relations_viz.stats

import java.net.URL
import java.util.Date
import scala.collection.immutable.SortedMap
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import scala.collection.SortedSet
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities._
import nl.tudelft.courses.in4355.github_relations_viz.GHRelationsViz._

trait Writer {
  def write(msg: Any)
  def writeln(msg: Any)
}

class FileWriter(filename: String) extends Writer {
  private val res = scalax.io.Resource.fromFile(filename)
  res.truncate(0)
  private val writer = res.writer
  override def write(msg: Any) = writer.write(msg.toString)
  override def writeln(msg: Any) = writer.write(msg.toString+"\n")
}

class TeeWriter(writer: Writer) extends Writer {
  override def write(msg: Any) = { print(msg.toString); writer.write(msg) }
  override def writeln(msg: Any) = { println(msg.toString); writer.writeln(msg.toString) }
}

object GHRelationsStats extends App {

  val epoch1990 = 631148400
  val epoch2015= 1420066800  

  val PERIOD    = 7 * 24 * 3600
  val FROM      = epoch1990
  val TO        = epoch2015
  val MINWEIGHT = 1

  // GENERAL FUNCTIONS

  val commitsurl = new URL("file:commits/commits.txt")
  val usersurl = new URL("file:commits/users.txt")
  val projectsurl = new URL("file:commits/projects.txt")
  val logfile = "stats.log"
    
  val dataDir = "doc/images/"
  val userProjectLinksPerWeekFile = dataDir+"user-project-links-per-week.dat"
  val projectsPerUserHistFile = dataDir+"projects-per-user-histogram.dat"
  val linkWeightHistFile = dataDir+"link-weight-histogram.dat"
  
  def getLines(url: URL) =
    scalax.io.Resource.fromURL(url)
                      .lines()

  def getBinnedTime(period: Int)(time: Int) =
    time - (time % period)

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
  def parseStringToCommitTuple(str: String) = {
    try {
      val CommitReg(pId, pName, uId, uName, ts) = str
      Some( (pId.toInt,uId.toInt,ts.toInt) )
    } catch {
      case _ => None
    }
  }  

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

  val commitlines = getLines(commitsurl)
  val projectlines = getLines(projectsurl)
  val userlines = getLines(usersurl)
  val logger = new TeeWriter(new FileWriter(logfile))

  logger.write("Starting statistics calculation")
  logger.writeln("")

  logger.writeln("Build project map")
  val projects = timed("reading",logger.writeln) {
    getLines(projectsurl).mapReduce[Map[Int,String]]( parseStringToIntString(_) toList )
  }
  logger.writeln("Found "+projects.size+" projects")
  
  logger.writeln("Build user map")
  val users = timed("reading",logger.writeln) {
    getLines(usersurl).mapReduce[Map[Int,String]]( parseStringToIntString(_) toList )
  }
  logger.writeln("Found "+users.size+" users")
  
  logger.writeln("Read commits")
  val cs = timed("reading",logger.writeln) {
    getLines(commitsurl).flatMap( parseStringToCommitTuple )
  }
  
  logger.writeln("Reduce commits to timeindexed Map")
  val (total,bcm) = timed("reducing",logger.writeln) {
    cs.filter( c => c._3 >= FROM && c._3 <= TO )
      .mapReduce[(Int,Map[Int,Set[(Int,Int)]])]( c => {
        val tb = getBinnedTime(PERIOD)(c._3)
        (1,(tb,(c._1,c._2)))
      } )
  }
  val n = (0 /: bcm)( _ + _._2.size )
  val p = (n.toDouble/total.toDouble)*100.0
  logger.writeln("Reduced "+total+" commits to "+n+" ("+p+"%) user-project links in "+bcm.size+" bins")
  
  val userProjectLinksPerWeek = bcm.mapReduce[SortedMap[Int,Int]]( e => e._1 -> e._2.size )
  val wch = new FileWriter(userProjectLinksPerWeekFile)
  userProjectLinksPerWeek.foreach( l => wch.writeln( "%d %d".format(l._1,l._2) ))
  
  val fcs = timed("filter time",logger.writeln) {
    bcm.filterKeys( k => k >= FROM && k <= TO ).values.flatten
  }
  logger.writeln("Kept "+fcs.size+" user-project links after time filter")
  
  val ups = timed("group projects by user",logger.writeln) {
    fcs.mapReduce[Map[Int,Set[Int]]]( c => (c._2,c._1) )
  }
  
  val projectsPerUserHist = ups.mapReduce[SortedMap[Int,Int]]( e => e._2.size -> 1 )
  val ppuh = new FileWriter(projectsPerUserHistFile)
  projectsPerUserHist.foreach( l => ppuh.writeln( "%d %d".format(l._1,l._2) ))
  val totalLinks = projectsPerUserHist.mapReduce[Int]( t => {
    val nUsers = t._2
    val nProjects = t._1
    val maxLinks = nUsers * ((nProjects+1)*nProjects) / 2
    maxLinks
  } )
  logger.writeln("Going to process "+totalLinks+" project links")
  
  val links = timed("Calculating project links",logger.writeln) {
    ups.values
       .mapReduce[Map[Link,Int]]( ps =>
          createProduct(ps).map( l => (Link(l._1,l._2).normalize,1) ) )
  }
  logger.writeln("Found "+links.size+" project links")
  
  val linkHist = links.mapReduce[SortedMap[Int,Int]]( l => l._2 -> 1 )
  val ldh = new FileWriter(linkWeightHistFile)
  linkHist.foreach( l => ldh.writeln( "%d %d".format(l._1,l._2) ))
  
  logger.writeln("Done")

}
