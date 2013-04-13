/* Copyright 2012-2013 The Github-Relations-Viz Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.tudelft.courses.in4355.github_relations_viz.stats

import java.net.URL
import java.util.Date
import scala.collection.immutable.SortedMap
import net.van_antwerpen.scala.collection.mapreduce.Aggregator
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import scala.collection.{BitSet,SortedSet}
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities._
import nl.tudelft.courses.in4355.github_relations_viz.GHRelationsViz._
import nl.tudelft.courses.in4355.github_relations_viz.util.{FileWriter,TeeWriter}
import nl.tudelft.courses.in4355.github_relations_viz.util.Timer._

object GHRelationsStats extends App {

  implicit val agg = new Aggregator[BitSet,Int] {
    override def zero = BitSet.empty
    override def insert(bs: BitSet, i: Int) = bs + i
  }

  val epoch1990 = 631148400
  val epoch2010 = 1262300400
  val epoch2011 = 1293836400
  val epoch2015 = 1420066800
  
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
    readProjects( _ => 0 )(projectsurl)
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
    fcs.par.reduceTo[Map[UserRef,BitSet]]
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
  
  val secondLinks = timed("Calculating project links (fancy reduce)",logger.writeln) {
    ((Set.empty[Link],BitSet.empty) /: ups.values.par) { (ls,ps) =>
      val links = ls._1
      val seen = ls._2
      val newprojects = ps -- seen
      val newlinks = for (p1 <- newprojects; p2 <- ps) yield Link(p1,p2)
      (links ++ newlinks, seen ++ newprojects)
    }
  }
  logger.writeln("Found %d project links".format(links.size) )
  
  val links = timed("Calculating project links (plain mapReduce)",logger.writeln) {
    ups.values.par.map( _.toSet ).mapReduce[Map[Link,Int]]( projectsToLinks )
  }
  logger.writeln("Found %d project links".format(links.size) )

  val linkHist = links.par.mapReduce[SortedMap[Int,Int]]( l => l._2 -> 1 )
  val ldh = new FileWriter(linkWeightHistFile)
  linkHist.foreach( l => ldh.writeln( "%d %d".format(l._1,l._2) ))
  
  logger.writeln("Done")

}
