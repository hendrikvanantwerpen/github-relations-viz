package nl.tudelft.courses.in4355.github_relations_viz.actors

import nl.tudelft.courses.in4355.github_relations_viz._
import nl.tudelft.courses.in4355.github_relations_viz.GHRelationsViz._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import scala.collection.immutable.SortedMap
import scala.collection.parallel.ParMap
import GHEntities._
import java.net.URL
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object GHResources {
	
	val resourceDir = ConfigFactory.load.getString("resources.location")
  
	println("Initializing resources with url: "+resourceDir)
	
 	val epoch1990 = 631148400
 	val epoch2015= 1420066800  
  
 		println("created epochs, initializin with: "+resourceDir+"/commits.txt")
  	val commitsurl = new URL("file://"+resourceDir+"/commits.txt")
		println("Initialized commits")
 	val forksurl = new URL("file://"+resourceDir + "/forks.txt")  
	 
 	val PERIOD = 7 * 24 * 3600
 	
 		println("Initializing forks with: "+forksurl)
    val forks = readForks(forksurl)
   		println("Initializing commits with: "+commitsurl)
	val commits = readCommits(commitsurl).mapReduce[ParMap[Int,Set[(UserRef,ProjectRef)]]] { c =>
      Some(c).filter( c => c.timestamp >= epoch1990 &&
                           c.timestamp <= epoch2015 &&
                           !forks.contains(c.project) )
             .map( c => (getBinnedTime(PERIOD)(c.timestamp),(c.user,c.project)) )
             .toList
     } 	 
	println("Done reading all commits")
			
			def getCommits = {
			  commits
		    }
}