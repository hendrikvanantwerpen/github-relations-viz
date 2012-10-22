package nl.tudelft.courses.in4355.github_relations_viz.mapreduce

import scala.collection.generic._
import scala.collection.immutable.{SortedMap,TreeMap,SortedSet,TreeSet}
import scalaz._
import Scalaz._
import scala.math.Ordering

object MultoidsV1 {

  /* First generation Multoids
   *  - Completely inferred Multoids
   *  - Need two implementations, one for monoid and one for multoid values
   *  - Multoids are implemented for concrete types, so for every map type
   *    a new Multoid implementation has to be made, that is identical except
   *    for return types.
   */
  
  trait Multoid[Coll,Elem] {
    def zero: Coll
    def append(c: Coll, a: Elem): Coll
  }

  implicit def MapMMultoid[A,B](implicit m: Monoid[B]) = new Multoid[Map[A,B],(A,B)] {
     override def zero = Map.empty
     override def append(c: Map[A,B], a: (A,B)) =
        c + (a._1 -> c.get( a._1 ).map( v => m.append(v,a._2) ).getOrElse( m.append(m.zero,a._2) ) )
  }
  
  implicit def MapPMultoid[Key,ValueColl,Value]
                (implicit p: Multoid[ValueColl,Value]) = 
    new Multoid[Map[Key,ValueColl],(Key,Value)] {
     override def zero = Map.empty
     override def append(c: Map[Key,ValueColl], a: (Key,Value)) =
        c + (a._1 -> c.get( a._1 ).map( v => p.append(v,a._2) ).getOrElse( p.append(p.zero,a._2) ) )
  }

}