package nl.tudelft.courses.in4355.github_relations_viz

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit._
import scala.collection.immutable.{TreeSet,HashSet,SortedSet,TreeMap,HashMap,SortedMap}
import scala.collection.generic.CanBuildFrom
import scala.util.Random
import scalaz._
import Scalaz._
import Monoids._
import scala.math.Ordering

object Performance extends App {

val numEls = 1e2.toInt
println("Collection size: "+numEls+" elements")

def chrono[A](f: => A, timeUnit: TimeUnit = MILLISECONDS): (A,Long) = {
  val start = System.nanoTime()
  val result: A = f
  val end = System.nanoTime()
  (result, timeUnit.convert(end-start, NANOSECONDS))
}

def testSimple[A](msg: String)(f: => A) = {
  val (res,rt) = chrono(f)
  println(msg+": "+rt+"ms")
}

def testWithIter[A](msg: String)(e: A => Int)(f: => TraversableOnce[A]) = {
  val (values,ct) = chrono(f)
  val (_,it) = chrono { values.foldLeft(0)( (s,a) => s + e(a) ) }
  println(msg+": "+ct+"ms & "+it+"ms")
}

def buildSuite[A](msg: String, c: Int => A, e: A => Int, values: TraversableOnce[Int])(implicit ordering: Ordering[A]) = {
  def elEx(x: A): Int = e(x)
  def mapEx(x: (A,Int)): Int = e(x._1)
  println
  println("===========================")
  println("BuildSuite: "+msg)
  println("===========================")
  testWithIter("List")(elEx) { (List.empty[A] /: values)( (l,e) => c(e) :: l ) }
  testWithIter("Set")(elEx) { (Set.empty[A] /: values)(_ + c(_)) }
  testWithIter("HashSet")(elEx) { (HashSet.empty[A] /: values)(_ + c(_)) }
  testWithIter("TreeSet")(elEx) { (TreeSet.empty[A] /: values)(_ + c(_)) }
  testWithIter("SortedSet")(elEx) { (SortedSet.empty[A] /: values)(_ + c(_)) }
  testWithIter("Map")(mapEx) { (Map.empty[A,Int] /: values)( (m,e) => m + (c(e) -> 0)) }
  testWithIter("HashMap")(mapEx) { (HashMap.empty[A,Int] /: values)( (m,e) => m + (c(e) -> 0)) }
  testWithIter("TreeMap")(mapEx) { (TreeMap.empty[A,Int] /: values)( (m,e) => m + (c(e) -> 0)) }
  testWithIter("SortedMap")(mapEx) { (SortedMap.empty[A,Int] /: values)( (m,e) => m + (c(e) -> 0)) }
}

def mergeSuite[A](msg: String)(rs1: List[A], rs2: List[A]) {
  println
  println("===========================")
  println("MergeSuite: "+msg+" - "+rs1.size+" + "+rs2.size)
  println("===========================")

  val s1 = Set.empty[Int] ++ rs1
  val s2 = Set.empty[Int] ++ rs2
  testSimple("Set") { s1 |+| s2 }

  val ss1 = SortedSet.empty[Int] ++ rs1
  val ss2 = SortedSet.empty[Int] ++ rs2
  testSimple("SortedSet") { ss1 |+| ss2 }
  
  val hs1 = HashSet.empty[Int] ++ rs1
  val hs2 = HashSet.empty[Int] ++ rs2
  testSimple("HashSet") { hs1 |+| hs2 }
  
  val ts1 = TreeSet.empty[Int] ++ rs1
  val ts2 = TreeSet.empty[Int] ++ rs2
  testSimple("TreeSet") { ts1 |+| ts2 }
  
  val m1 = Map() ++ rs1.map( _ -> 0 )
  val m2 = Map() ++ rs2.map( _ -> 0)
  testSimple("Map") { m1 |+| m2 }
  
}

def appendSuite[A](msg: String)(is: List[A]) = {
  println
  println("===========================")
  println("AppendSuite: "+msg+" - "+is.size)
  println("===========================")

  testSimple( "Set" ) {
      is.foldLeft(Set.empty[A])( (s,i) => s + i )
  }
  testSimple( "SetBuilder" ) {
      val bf = implicitly[CanBuildFrom[Set[A],A,Set[A]]]
      is.foldLeft(Set.empty[A])( (s,i) => {
          bf(s) ++=(s) +=(i) result
      } )
  }
}

def rands(xs: Traversable[_]) = {
  val rnd = new Random
  xs.map( _ => rnd.nextInt ).toList
}

case class Wrap(value: Int)
implicit object WrapOrdering extends Ordering[Wrap] {
  def intOrder(implicit ordering: Ordering[Int]): Ordering[Int] = ordering
  def compare(w1: Wrap, w2: Wrap): Int = intOrder.compare(w1.value,w2.value)
}

val values = rands(1 to numEls)
val wrapped = values.map( Wrap(_) )
appendSuite("Simple")(values)
appendSuite("Wrapped")(wrapped)
buildSuite("Simple", x => x, (x:Int) => x, values)
buildSuite("Wrapped", x => Wrap(x), (x:Wrap) => x.value, values)

val rs1 = rands(1 to numEls)
val rs2 = rands(1 to numEls)
mergeSuite("Simple")(rs1, rs2)
mergeSuite("Strings")(rs1.map( _.toString ), rs2.map( _.toString ))
mergeSuite("Wrapped")(rs1.map( Wrap(_) ), rs2.map( Wrap(_) ))

}
