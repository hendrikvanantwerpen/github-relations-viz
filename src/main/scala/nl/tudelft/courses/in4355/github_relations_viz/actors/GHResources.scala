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
	val resourceDir = ConfigFactory.load.getConfig("resource_kv")
  
 	val epoch1990 = 631148400
 	val epoch2015= 1420066800  
  
  	val commitsurl = new URL(resourceDir + "/commits.txt")
 	val forksurl = new URL(resourceDir + "/forks.txt")  
	 
 	val PERIOD = 7 * 24 * 3600
 	

    val forks = readForks(forksurl)
  
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