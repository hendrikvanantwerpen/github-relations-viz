package nl.tudelft.courses.in4355.github_relations_viz.actors

import akka.event.Logging
import java.net.URL
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities._
import nl.tudelft.courses.in4355.github_relations_viz.GHRelationsViz._
import net.van_antwerpen.scala.collection.mapreduce.Monoid._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._

class GHObtainLinks(url: URL, divisor: Int, remainder: Int, maxLines: Int) {
	private val TReg = """([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+)""".r  

	println("Reading commits for modulo %d and remainder %d and a maximum of &d lines".format(divisor, remainder, maxLines))
	val commits = readActorCommitsFromURL(divisor, remainder)
	println("Done reading commits: "+commits.size);
	

	
	
	def obtainLinks(from: Int, until: Int) = {
	  commits
	      .filter( c => isCommitInRange(c, from, until) )
	      .mapReduce[Map[User,Set[Project]]](groupProjectByUser)
	      .values
	      .flatMapReduce[Map[Link,Int]](projectsToLinks)
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
			val c = Commit(Project(pId.toInt,pName),User(uId.toInt,uName),ts.toInt);
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