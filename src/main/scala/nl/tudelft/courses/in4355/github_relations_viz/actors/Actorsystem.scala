package nl.tudelft.courses.in4355.github_relations_viz.actors

import nl.tudelft.courses.in4355.github_relations_viz._
import nl.tudelft.courses.in4355.github_relations_viz.GHRelationsViz._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities._
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory


object Actorsystem {
	def main(args: Array[String]) {
	  val system = ActorSystem("ghlink", ConfigFactory.load.getConfig("LinkCombineHost_ovh"))
	  println("Actorsystem started"+system.name+" reading commits...")
	  println("Done reading resources: "+GHResources.commits.size)
	}
}
