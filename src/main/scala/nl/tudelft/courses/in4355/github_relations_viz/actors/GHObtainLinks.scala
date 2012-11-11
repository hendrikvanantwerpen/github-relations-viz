package nl.tudelft.courses.in4355.github_relations_viz.actors

import akka.event.Logging
import java.net.URL
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities._
import nl.tudelft.courses.in4355.github_relations_viz.GHRelationsViz._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._

class GHObtainLinks(url: URL, divisor: Int, remainder: Int) {
	private val TReg = """([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+)""".r  

	println("Reading commits for modulo %d and remainder %d".format(divisor, remainder))
	val commits = readActorCommitsFromURL(divisor, remainder)
	println("Done reading commits: "+commits.size);
	

	
	
	def obtainLinks(from: Int, until: Int) = {
	  //Size uitrekenen kost 1 minuut? Oja, niet geordent :P
	  println("Started filtering lines")
	  //println("Started filtering %d lines".format(commits.size))
	 val filtered = commits.filter( c => c.timestamp >= from && c.timestamp <= until )
	 println("Done filtering, result is %d lines".format(filtered.size))
	 val grouped = filtered.mapReduce[Map[Int,Set[Int]]](groupProjectByUser)
	 println("Done grouping. Result is %d size".format(grouped.size))
	 val links = grouped.values.mapReduce[Map[Link,Int]](projectsToLinks)
	 println("Done creating links. Size is %d".format(links.size))
	 links
	}
	
	
	/**
	 * Reads the commits from a file within the from and until range
	 * @param url
	 * @param from
	 * @param until
	 * @return
	 */
	def readActorCommitsFromURL(from : Int, until : Int) =
		scalax.io.Resource.fromURL(url)
		.lines()
		.flatMap(parseActorStringToCommit(from, until)) 	
	
		
	/**
	 * Parses a string into a commit object. Returns none for lines that should not be in the list
	 * @param from
	 * @param until
	 * @param str
	 * @return
	 */
	def parseActorStringToCommit(divisor : Int, remainder : Int)(str: String) = {
		try {
			val TReg(pId, pName, uId, uName, ts) = str
			val c = Commit(pId.toInt,uId.toInt,ts.toInt)
			if(isActorTask(c, divisor, remainder)) {
				Some(c) 
			} else {
				None
			}
		} catch {
			case _ => None
		}
	}  
}