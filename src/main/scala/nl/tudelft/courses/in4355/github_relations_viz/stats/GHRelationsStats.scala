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

  val PERIOD    = 7 * 24 * 3600
  val FROM      = epoch1990
  val TO        = epoch2015
  val MINWEIGHT = 1

  // GENERAL FUNCTIONS

  val commitsurl = new URL("file:commits/commits.txt")
  val usersurl = new URL("file:commits/users.txt")
  val projectsurl = new URL("file:commits/projects.txt")
  val forksurl = new URL("file:commits/forks.txt")
  val logfile = "stats.log"
    
  val dataDir = "doc/images/"
  val userProjectLinksPerWeekFile = dataDir+"user-project-links-per-week.dat"
  val projectsPerUserHistFile = dataDir+"projects-per-user-histogram.dat"
  val linkWeightHistFile = dataDir+"link-weight-histogram.dat"
  
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

  val logger = new TeeWriter(new FileWriter(logfile))

  logger.write("Starting statistics calculation")
  logger.writeln("")

  logger.writeln("Build user map")
  val users = timed("reading",logger.writeln) {
    readUsers(usersurl)
  }
  def userResolv(id: UserRef) = users.get(id).getOrElse(User.unknown(id))
  logger.writeln("Found %d users".format(users.size) )

  logger.writeln("Build project map")
  val projects = timed("reading",logger.writeln) {
    readProjects(projectsurl)
  }
  def projectResolv(id: ProjectRef) = projects.get(id).getOrElse(Project.unknown(id))
  logger.writeln("Found %d projects".format(projects.size) )
  
  logger.writeln("Read forks")
  val forks = timed("reading",logger.writeln) {
    readForks(forksurl)
  }
  logger.writeln("Found %d forks".format(forks.size) )
  
  logger.writeln("Read commits to timeindexed Map")
  val (total,bcm) = timed("reading",logger.writeln) {
    readCommits(commitsurl)
      .mapReduce[(Int,Map[Int,Set[(UserRef,ProjectRef)]])] { c =>
        Some(c).filter( c => c.timestamp != 0 &&
                             c.timestamp >= FROM &&
                             c.timestamp <= TO &&
                             !forks.contains(c.project) )
               .map { c => 
                  (1,(getBinnedTime(PERIOD)(c.timestamp),(c.user,c.project))) 
                }
               .toList
      }
  }
  val n = (0 /: bcm)( _ + _._2.size )
  val p = (n.toDouble/total.toDouble)*100.0
  logger.writeln("Reduced %d commits to %d (%.2f%%) user-project links in %d bins".format(total,n,p,bcm.size) )
  
  val userProjectLinksPerWeek = bcm.par.mapReduce[SortedMap[Int,Int]]( e => e._1 -> e._2.size )
  val wch = new FileWriter(userProjectLinksPerWeekFile)
  userProjectLinksPerWeek.foreach( l => wch.writeln( "%d %d".format(l._1,l._2) ))
  
  val fcs = timed("filter time",logger.writeln) {
    bcm.par.filter( e => e._1 >= FROM && e._1 <= TO ).seq.values.flatten
  }
  logger.writeln("Kept %d user-project links after time filter".format(fcs.size) )
  
  val ups = timed("group projects by user",logger.writeln) {
    fcs.par.reduceTo[Map[UserRef,Set[ProjectRef]]]
  }
  
  val projectsPerUserHist = ups.par.mapReduce[SortedMap[Int,Int]]( e => e._2.size -> 1 )
  val ppuh = new FileWriter(projectsPerUserHistFile)
  projectsPerUserHist.foreach( l => ppuh.writeln( "%d %d".format(l._1,l._2) ))
  val totalLinks = projectsPerUserHist.mapReduce[Int]( t => {
    val nUsers = t._2
    val nProjects = t._1
    val maxLinks = nUsers * ((nProjects+1)*nProjects) / 2
    maxLinks
  } )
  logger.writeln("Going to process %d project links".format(totalLinks) )
  
  val links = timed("Calculating project links",logger.writeln) {
    ups.values.par.mapReduce[Map[Link,Int]]( projectsToLinks )
  }
  logger.writeln("Found %d project links".format(links.size) )
  
  val linkHist = links.par.mapReduce[SortedMap[Int,Int]]( l => l._2 -> 1 )
  val ldh = new FileWriter(linkWeightHistFile)
  linkHist.foreach( l => ldh.writeln( "%d %d".format(l._1,l._2) ))
  
  logger.writeln("Done")

}
