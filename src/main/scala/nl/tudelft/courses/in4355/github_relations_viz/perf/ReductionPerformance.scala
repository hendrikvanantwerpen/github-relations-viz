package nl.tudelft.courses.in4355.github_relations_viz

import java.net.URL
import scala.collection._
import scala.collection.immutable.{Map,SortedMap}
import scalaz._
import Scalaz._
import GHEntities._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.CollectionAggregators._
import net.van_antwerpen.scala.collection.mapreduce.ValueAggregators._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import Timer._

object ReductionPerformance extends App {
  
  val LIMIT = 1e6.toInt
  val GROUP = 1e5.toInt
  val PERIOD = 7 * 24 * 3600
  
  val resource = "/commits.txt"
  def readLines(res: String) = {
    import scalax.io.Resource
    Resource.fromURL(this.getClass.getResource(res)).lines()
  }
  
  val commits = timed("read commits") {
    readLines(resource)
    .drop( 0*LIMIT )
    .take( LIMIT )
    .par
    .flatMap( GHRelationsViz.parseStringToCommit )
    .seq
  }

  commits.grouped( GROUP ).drop( 0 ).take( 5 ).foreach( group => {
    def report(m: Map[Int,Set[Commit]]) = println("have "+m.foldLeft(0)( _ + _._2.size )+" left after grouping")
    println
    val cs = group.par
    val resHFL = timed("handmade foldLeft chunk") { hflGroupCommits(PERIOD)(cs) }
    report(resHFL)
    val resMR = timed("mapReduce chunk") { mrGroupCommits(PERIOD)(cs) }
    report(resMR)
    val resMFL = timed("monoid foldLeft chunk") { mflGroupCommits(PERIOD)(cs) }
    report(resMFL)
  })
  
  def mrGroupCommits(w: Int)(cs: GenTraversableOnce[Commit]) =
    cs.mapReduce[SortedMap[Int,Set[Commit]]](groupCommits(w))
  def groupCommits(w: Int)(c: Commit) = {
    val cc = c.copy(timestamp = c.timestamp - (c.timestamp % w))
    (cc.timestamp -> cc)
  }

  def mflGroupCommits(w: Int)(cs: GenTraversableOnce[Commit]) = {
    cs.foldLeft(Map.empty[Int,Set[Commit]])( (m,c) => {
      val cc = c.copy(timestamp = c.timestamp - (c.timestamp % w))
      m |+| Map(cc.timestamp -> Set(cc))
    })
  }

  def hflGroupCommits(w: Int)(cs: GenTraversableOnce[Commit]) = {
    cs.foldLeft(SortedMap.empty[Int,Set[Commit]])( (m,c) => {
      val cc = c.copy(timestamp = c.timestamp - (c.timestamp % w))
      m + m.get( cc.timestamp ).fold( v => cc.timestamp -> (v + cc), cc.timestamp -> Set(cc) )
    })
  }  
 
}
