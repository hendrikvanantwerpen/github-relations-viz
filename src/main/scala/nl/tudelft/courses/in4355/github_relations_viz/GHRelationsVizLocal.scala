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
package nl.tudelft.courses.in4355.github_relations_viz

import java.net.URL
import java.util.Date
import scala.io.Source
import scala.util.matching.Regex
import GHEntities._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import util.Logger._
import util.Timer
import util.Timer._
import util.ThresholdCountingSet
import scala.collection.immutable.SortedMap
import scala.collection.parallel.ParMap
import scala.collection.GenMap
import nl.tudelft.courses.in4355.github_relations_viz.util.Logger

class GHRelationsVizLocal(projectsurl: URL,
                          usersurl: URL,
                          forksurl: URL,
                          commitsurl: URL,
                          minFrom: Int,
                          maxTo: Int,
                          period: Int) {
  import GHRelationsViz._

  private val timer = new Timer
  
  println( "Reading users" )
  timer.tick
  private val users = readUsers(usersurl).log( println("Done in %s".format(timer.tick.nanoTimeToString)) )
  def getUser(id: UserRef) = users.get(id).getOrElse(User.unknown(id))
  
  /***********************************
   * BEGIN MUTABLE SECTION - CAREFUL!!
   ***********************************/
  /**/ private val languages = scala.collection.mutable.Map.empty[String,LangRef] + ("" -> -1)
  /**/ 
  /**/ println( "Reading projects" )
  /**/ timer.tick
  /**/ private val projects = readProjects(getLangRef)(projectsurl).log( println("Done in %s".format(timer.tick.nanoTimeToString)) )
  /**/ 
  /**/ private val invLanguages = languages.map( kv => kv._2 -> kv._1 )
  /********************************
   * END MUTABLE SECTION - RELAX :)
   ********************************/
  
  def getLangRef(lang: String) = {
    languages getOrElseUpdate( lang, {
                 languages.size
              } )
  }
  def getProject(id: ProjectRef) = projects.get(id).getOrElse(Project.unknown(id))
  def getLang(id: LangRef) = invLanguages(id)
  
  println( "Reading forks" )
  timer.tick
  private val forks = 
    readForks(forksurl).map( kv => kv._1 )
                       .toSet
                       .log( println("Done in %s".format(timer.tick.nanoTimeToString)) )

  println( "Reading commits" )
  timer.tick
  private val interactions = readCommits(commitsurl)
    .mapReduce[ParMap[Int,ParMap[LangRef,Set[Interaction]]]] { c =>
       Some(c).filter( c => !forks.contains(c.project) )
              .filter( c => c.timestamp >= minFrom &&
                            c.timestamp <= maxTo )
              .map { c => getBinnedTime(period)(c.timestamp) -> (getProject(c.project).lang -> Interaction(c.user,c.project)) }
              .toList
     }.log( println("Done in %s".format(timer.tick.nanoTimeToString)) )

  println( "Calculating language counts" )
  timer.tick
  private val languagesPerPeriod =
    interactions
      .map { pv => pv._1 -> pv._2.map { lv => lv._1 -> lv._2.size } }
      .log( println("Done in %s".format(timer.tick.nanoTimeToString)) )
       
  private def getFilteredInteractions(from: Int, to: Int, langFilter: LangFilter) = {
      println( "Get commits from %d until %d for %s".format(
                 from, to, 
                 langFilter.toString ) )
      
      val interactionsInTime = timed( "Filter %d time bins".format(interactions.size), println ) {
        interactions.filter( e => e._1 >= from && e._1 <= to )
                    .mapReduce[ParMap[LangRef,Set[Interaction]]] { kv => kv._2 }
      }

      val interactionsForLanguages = timed( "Filter for languages".format(interactionsInTime.size), println ) { 
        interactionsInTime
          .filter( li => langFilter (li._1) )
          .mapReduce[Set[Interaction]]( _._2 )
      }

      interactionsForLanguages
  }

  def getInteractionHistogram(from: Int, to: Int, langFilter: LangFilter) = {
    println( "Calculating project-user/week histogram" )
    val timer = new Timer
    val emptyMap = SortedMap.empty[Int,Int] ++ interactions.map( _._1 -> 0 )
	val actualMap =
	  interactions
	    .filter( kv => kv._1 >= from && kv._1 <= to )
	    .mapReduce[Map[Int,Int]] { tlis => 
	       tlis._1 -> tlis._2.filter( lis => langFilter (lis._1) )
	                         .map { lis => lis._2.size }
	     }
        .log( println("Done in %s".format(timer.tick.nanoTimeToString)) )
    emptyMap |<| actualMap
  }
  
  def getLanguageHistogram(from: Int, to: Int) = {
    println( "Calculating language histogram" )
	val timer = new Timer
	languagesPerPeriod
	  .filter( kv => kv._1 >= from && kv._1 <= to )
	  .mapReduce[Map[LangRef,Int]] { plc => plc._2 }
      .map { lc => getLang(lc._1) -> lc._2 }
      .log( println("Done in %s".format(timer.tick.nanoTimeToString)) )
  }
  
  def getProjectLinks(from: Int, to: Int, langFilter: LangFilter, minLinkWeight: Int, limit: Int) = {
    val timer = new Timer
    println( "Calculating project links with minimum weight %d (limit %d)".format(from,to,minLinkWeight,limit) )
    getFilteredInteractions(from, to, langFilter)
      .log( cs => println("Done in %s.\nReduce %d commits to projects per user".format(timer.tick.nanoTimeToString,cs.size) ) )
      .mapReduce[ParMap[UserRef,Set[ProjectRef]]]( i => (i.user,i.project) )
      .map( kv => kv._2 )
      .log( psets => println("Done in %s.\nReducing %d project sets to link map".format(timer.tick.nanoTimeToString, psets.size) ) )
      .par.foldLeft(Right(ThresholdCountingSet[Link](minLinkWeight)):Either[String,ThresholdCountingSet[Link]]) { (ecs,ps) =>
         ecs.right.flatMap { cs =>
           if ( cs.included.size > limit ) Left("Limit exceeded.")
           else Right( cs ++ projectsToLinksWithoutCount(ps) )
         }
       }
      .log( _.fold( _ => println( "Interrupted because limit of %d links was exceeded.".format(limit) ),
                    _ => println( "Done in %s.".format(timer.tick.nanoTimeToString) ) ) )
      .right.map ( _.included )
    }
  
}
