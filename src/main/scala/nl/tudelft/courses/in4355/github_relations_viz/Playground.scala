package nl.tudelft.courses.in4355.github_relations_viz

import scala.collection.immutable.SortedMap
import scalaz._
import Scalaz._
import Monoids._
import Multoids._
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
    println
    def report(m: SortedMap[Int,Set[Commit]]) = println("have "+m.foldLeft(0)( _ + _._2.size )+" left after grouping")
    val resMR = timed("mapReduce chunk") { mrGroupCommits(PERIOD)(cs) }
    report(resMR)
    val resMFL = timed("monoid foldLeft chunk") { mflGroupCommits(PERIOD)(cs) }
    report(resMFL)
    val resHFL = timed("handmade foldLeft chunk") { hflGroupCommits(PERIOD)(cs) }
    report(resHFL)
  })
  
  def mrGroupCommits(w: Int)(cs: Traversable[Commit]) =
    mapReduce[Commit,(Int,Commit),SortedMap[Int,Set[Commit]]](groupCommits(w))(cs)
  def groupCommits(w: Int)(c: Commit) = {
    val cc = c.copy(timestamp = c.timestamp - (c.timestamp % w))
    (cc.timestamp -> cc)
  }

  def mflGroupCommits(w: Int)(cs: Traversable[Commit]) = {
    cs.foldLeft(SortedMap.empty[Int,Set[Commit]])( (m,c) => {
      val cc = c.copy(timestamp = c.timestamp - (c.timestamp % w))
      m |+| SortedMap(cc.timestamp -> Set(cc))
    })
  }

  def hflGroupCommits(w: Int)(cs: Traversable[Commit]) = {
    cs.foldLeft(SortedMap.empty[Int,Set[Commit]])( (m,c) => {
      val cc = c.copy(timestamp = c.timestamp - (c.timestamp % w))
      m + m.get( cc.timestamp ).fold( v => cc.timestamp -> (v + cc), cc.timestamp -> Set(cc) )
    })
  }  
 
}
