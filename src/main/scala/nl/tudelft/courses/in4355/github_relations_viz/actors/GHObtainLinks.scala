package nl.tudelft.courses.in4355.github_relations_viz.actors

import akka.event.Logging
import java.net.URL
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities._
import nl.tudelft.courses.in4355.github_relations_viz.GHRelationsViz._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._

class GHObtainLinks(actorCount: Int, actorIndex: Int) {

    def isActorTask(c: Commit) =
        c.user % actorCount == actorIndex

	println("Reading commits for actor %d out of %d".format(actorIndex, actorCount))
	val commits = GHResources.commits.filter( isActorTask )
	println("Done reading commits for actor %d out of %d and size %d".format(actorIndex, actorCount, commits.size))  
	
	def obtainLinks(from: Int, until: Int) = {
	  //Size uitrekenen kost 1 minuut? Oja, niet geordent :P
	  println("Started filtering lines")
	  //println("Started filtering %d lines".format(commits.size))
	 val filtered = commits.filter( c => c.timestamp >= from && c.timestamp <= until )
	 println("Done filtering, result is %d lines".format(filtered.size))
	 val grouped = filtered.mapReduce[Map[UserRef,Set[ProjectRef]]]( c => (c.user, c.project) )
	 println("Done grouping. Result is %d size".format(grouped.size))
	 val links = grouped.values.mapReduce[Map[Link,Int]](projectsToLinks)
	 println("Done creating links. Size is %d".format(links.size))
	 links
	}
			
}