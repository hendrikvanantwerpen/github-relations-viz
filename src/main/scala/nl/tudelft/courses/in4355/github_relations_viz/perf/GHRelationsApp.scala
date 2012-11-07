package nl.tudelft.courses.in4355.github_relations_viz.perf

import net.van_antwerpen.scala.collection.mapreduce.Monoid._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
//import scala.collection.parallel._

import Timer._
import nl.tudelft.courses.in4355.github_relations_viz.Logger._

object GHRelationsApp extends App {
  import nl.tudelft.courses.in4355.github_relations_viz.GHEntities._
  import nl.tudelft.courses.in4355.github_relations_viz.GHRelationsViz._
  
  val url = getClass.getResource("/commits.txt")
  val nodes = 10
  val node = 1
  val PERIOD = 7 * 24 * 3600
  val from = Int.MinValue
  val to = Int.MaxValue
  val minDegree = 1

  val links = 
    scalax.io.Resource.fromURL(url)
      .lines()
      .log(println("read commits"))
      .flatMapReduce[Set[Commit]]( parseStringToCommitFiltered(from, to, PERIOD) )
      .log(println("binned commits"))
      .filter( c => c.timestamp > from && c.timestamp < to )
      .log(println("filtered commits"))
      .mapReduce[Map[User,Set[Project]]](groupProjectByUser)
      .log(println("grouped commits per user"))
      .filter( t => t._2.size < 1000 )
      .log(println("filtered big ones out"))
      .flatMapReduce[Map[Link,Int]]( t => {
          if ( t._2.size > 500 ) println(t._1+" is a big fish ("+t._2.size+" big)")
          projectsToLinks(t._2)
       } )
      .log(println("created link map"))
      .filter( l => l._2 >= minDegree )
      .log(println("done"))

  println("Returned "+links.size+" unique links")

}
