package nl.tudelft.courses.in4355.github_relations_viz.mapreduce

import scala.collection.generic._
import scala.collection.immutable.{SortedMap,TreeMap,SortedSet,TreeSet}
import scalaz._
import Scalaz._
import scala.math.Ordering

object MultoidsV2 {

  /* Second generation Multoids
   *  - The Multoids are now generic over types, so all Maps share one
   *    Multoid implementation.
   *  - Versions for Monoid and Multoid are still very much alike.
   *  - We lost complete inference, because we want to know about the value type
   *    of the sub-multoid. We made this a simple (i.e. not higher order) type
   *    parameter so we are not dependent on the number of type parameters of
   *    the underlying collection. But because of this, we have no access to
   *    the underlying collections value type and we must specify it, resulting
   *    in an ugly Multoid lookup for every mapReduce call where it's used. 
   */
  
  trait Multoid[Coll,Elem] {
    def zero: Coll
    def append(c: Coll, a: Elem): Coll
  }

  implicit def giveMultoidForMonoidMap[Key,Value,MapRepr <: Map[Key,Value]]
                (implicit m: Monoid[Value], bf: CanBuildFrom[MapRepr,(Key,Value),MapRepr])
                : Multoid[MapRepr,(Key,Value)] = {
    new Multoid[MapRepr,(Key,Value)] {
      override def zero = bf().result
      override def append(c: MapRepr, a: (Key,Value)) = {
        (bf() ++=(c) +=(a._1 -> c.get( a._1 ).map( v => m.append(v,a._2) ).getOrElse( m.append(m.zero,a._2) ))).result
      }
    }
  }

  implicit def giveMultoidForMultoidMap[Key,ValueColl,ValueElem,MapRepr <: Map[Key,ValueColl]]
                (implicit p: Multoid[ValueColl,ValueElem], bf: CanBuildFrom[MapRepr,(Key,ValueColl),MapRepr])
                : Multoid[MapRepr,(Key,ValueElem)] = {
    new Multoid[MapRepr,(Key,ValueElem)] {
      override def zero = bf().result
      override def append(c: MapRepr, a: (Key,ValueElem)) = {
        (bf() ++=(c) +=(a._1 -> c.get( a._1 ).map( v => p.append(v,a._2) ).getOrElse( p.append(p.zero,a._2) ))).result
      }
    }
  }

}