package nl.tudelft.courses.in4355.github_relations_viz

import scala.collection.immutable.SortedMap
import scalaz._
import Scalaz._
import Monoids._
import Pluroids._
import MapReduce._
import GHEntities._
import Timer._
import java.net.URL

object Playground extends App {
  
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
    .map( GHRelationsViz.parseStringToCommit )
    .toList
  }

  commits.grouped( GROUP ).drop( 0 ).take( 5 ).foreach(cs => {
    def report(m: SortedMap[Int,Set[Commit]]) = println("have "+m.foldLeft(0)( _ + _._2.size )+" left after grouping")
    //val resMR = timed("mapReduce chunk") { mrGroupCommits(PERIOD)(cs) }
    //report(resMR)
    val resMFL = timed("monoid foldLeft chunk") { mflGroupCommits(PERIOD)(cs) }
    report(resMFL)
    val resHFL = timed("handmade foldLeft chunk") { hflGroupCommits(PERIOD)(cs) }
    report(resHFL)
  })
  
  //def mrGroupCommits(w: Int)(cs: Traversable[Commit]) = mapReduceP[Commit,(Int,Commit),SortedMap[Int,Set[Commit]]](groupCommits(w))(cs)
  //def groupCommits(w: Int)(c: Commit) = {
  //  val cc = c.copy(timestamp = c.timestamp - (c.timestamp % w))
  //  (cc.timestamp -> cc)
  //}

  def mflGroupCommits(w: Int)(cs: Traversable[Commit]) = {
    cs.foldLeft(SortedMap.empty[Int,Set[Commit]])( (m,c) => {
      val t = c.timestamp - (c.timestamp % w)
      m |+| SortedMap(t -> Set(c.copy(timestamp = t))) 
    })
  }

  def hflGroupCommits(w: Int)(cs: Traversable[Commit]) = {
    cs.foldLeft(SortedMap.empty[Int,Set[Commit]])( (m,c) => {
      val t = c.timestamp - (c.timestamp % w)
      val cc = c.copy(timestamp = t)
      m + m.get( t ).fold( v => t -> (v + cc), t -> Set(cc) )
    })
  }  
  
}
