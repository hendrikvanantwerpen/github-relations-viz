package nl.tudelft.courses.in4355.github_relations_viz.actors

import akka.event.Logging
import java.net.URL
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities._
import nl.tudelft.courses.in4355.github_relations_viz.GHRelationsViz._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import nl.tudelft.courses.in4355.github_relations_viz.Logger._
import scala.collection.parallel.ParMap

class GHObtainLinks(actorCount: Int, actorIndex: Int) {

    def isActorTask(c: Tuple2[UserRef, ProjectRef]) =
        c._1 % actorCount == actorIndex

	println("Reading commits for actor %d out of %d".format(actorIndex, actorCount))
    
	val commits = GHResources.commits.map(kv => kv._1 -> kv._2.filter(isActorTask))
	println("Done reading commits for actor %d out of %d and size %d".format(actorIndex, actorCount, commits.size))  
    
  def getProjectLinks(from: Int, until: Int) = {
    println( "Calculating project links from %d until %d".format(from,until) )
    commits
      .log( cs => println( "Filter %d time bins".format(cs.size) ) )
      .par.filter( e => e._1 >= from && e._1 <= until ).map( kv => kv._2 ).flatten
      .log( cs => println("Reduce %d commits to projects per user".format(cs.size) ) )
      .reduceTo[ParMap[UserRef,Set[ProjectRef]]]
      .map( kv => kv._2 )
      .log( psets => println("Reducing %d project sets to link map".format(psets.size) ) )
      .par.mapReduce[ParMap[Link,Int]]( projectsToLinks )
      .log(cs => println("Done filtering"))
  }
  
			
}