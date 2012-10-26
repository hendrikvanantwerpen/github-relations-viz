package nl.tudelft.courses.in4355.github_relations_viz.actors

import akka.actor.Actor
import akka.event.Logging
import java.net.URL
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities._
import nl.tudelft.courses.in4355.github_relations_viz.GHRelationsViz._

class GHObtainLinks extends Actor {
	private val TReg = """([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+) ([^ ]+)""".r  
  
	val log = Logging(context.system, this)
	
	def receive = {
	  case "Test" => log.info("received test")
	  case _ => log.info("received unknown shit")	  
	}
	
	def readCommitsFromURL(url: URL, from : Int, until : Int) =
		scalax.io.Resource.fromURL(url)
		.lines()
		.flatMap(parseStringToCommit(from, until)) 	
	
	def parseStringToCommit(from : Int, until : Int)(str: String) = {
		try {
			val TReg(pId, pName, uId, uName, ts) = str
			val c = Commit(Project(pId.toInt,pName),User(uId.toInt,uName),ts.toInt);
			if(isCommitInRange(c, from, until)) {
				Some(c) 
			} else {
				None
			}
		} catch {
			case _ => None
		}
	}  
}