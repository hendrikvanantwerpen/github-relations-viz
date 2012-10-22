package nl.tudelft.courses.in4355.github_relations_viz.mapreduce

import scala.collection.generic._
import scala.collection.immutable.{SortedMap,TreeMap,SortedSet,TreeSet}
import scalaz._
import Scalaz._
import scala.math.Ordering

object MultoidsV3 {

  /* Third generation
   *  - Infer simple collections and maps over monoids automatically
   *  - Any other Map multoid has to be created for the specific subtype
   *    multoid
   */

  trait Multoid[Coll,Elem] {
    def zero: Coll
    def append(c: Coll, a: Elem): Coll
  }

  implicit def mkCollectionMultoid[Repr <: TraversableOnce[Elem],Elem]
      (implicit bf: CanBuildFrom[Repr,Elem,Repr]) =
    new Multoid[Repr,Elem] {
      override def zero = bf().result
      override def append(c: Repr, a: Elem) = (bf(c) ++=(c) +=(a)).result
  }

  implicit def mkMapMultiodWithMonoid[Key, Value, Repr <: Map[Key,Value]]
      (implicit m: Monoid[Value], bf: CanBuildFrom[Repr,(Key,Value),Repr]) =
    new Multoid[Repr,(Key,Value)] {
      override def zero = bf().result
      override def append(c: Repr, a: (Key,Value)) = {
        (bf(c) ++=(c) +=(a._1 -> c.get( a._1 ).map( v => m.append(v,a._2) ).getOrElse( m.append(m.zero,a._2) ))).result
      }
    }

  def createMapMultoidWithMultoid[Key, Value, Repr[K,V] <: Map[K,V], Elem]
      (implicit p: Multoid[Value,Elem], bf: CanBuildFrom[Repr[Key,Value], (Key,Value), Repr[Key,Value]]) =
    new Multoid[Repr[Key,Value],(Key,Elem)] {
      override def zero = bf().result
      override def append(c: Repr[Key,Value], a: (Key,Elem)) = {
        (bf(c) ++=(c) +=(a._1 -> c.get( a._1 ).map( v => p.append(v,a._2) ).getOrElse( p.append(p.zero,a._2) ))).result
      }
    }

}