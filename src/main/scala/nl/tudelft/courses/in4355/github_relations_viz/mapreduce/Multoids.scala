package nl.tudelft.courses.in4355.github_relations_viz.mapreduce

import scala.collection.generic.CanBuildFrom
import scalaz._
import Scalaz._

object Multoids {

  /* Fourth generation
   *  - Collections and Maps are completely inferred, several layers deep.
   *    The trick was to make Repr explicit in the implicit function, although it
   *    is not in the resulting Multoid
   *  - Monoids are auto wrapped in multoids to continue the work
   *  - Use builders only to create initial type, then use fast collection appends
   *    This is needed because otherwise we rebuild the collection for every element
   *  - The combination of the map functions return type and the mapReduce return type
   *    makes selecting wether a Multoid or a Monoid is needed unambiguous. Maybe
   *    collections with values of the same collection type might give trouble,
   *    in that case you can always build the multoid explicitly.
   */

  trait Multoid[Coll,Elem] {
    def nil: Coll
    def insert(c: Coll, a: Elem): Coll
  }

  implicit def MonoidMultoid[Value]
                (implicit m: Monoid[Value]) =
    new Multoid[Value,Value] {
      override def nil = m.zero
      override def insert(c: Value, a: Value) = m.append(c,a)
    }

  implicit def SeqMultoid[Elem, Repr[X] <: Seq[X]]
                (implicit bf: CanBuildFrom[Repr[Elem],Elem,Repr[Elem]]) =
    new Multoid[Repr[Elem],Elem] {
      override def nil = bf().result
      override def insert(c: Repr[Elem], a: Elem) =
        (a +: c).asInstanceOf[Repr[Elem]]
    }

  implicit def SetMultoid[Elem, Repr[X] <: Set[X]]
                (implicit bf: CanBuildFrom[Repr[Elem],Elem,Repr[Elem]]) =
    new Multoid[Repr[Elem],Elem] {
      override def nil = bf().result
      override def insert(c: Repr[Elem], a: Elem) =
        (c + a).asInstanceOf[Repr[Elem]]
    }

  implicit def Tuple2Multoid[C1,V1,C2,V2]
                (implicit ma: Multoid[C1,V1], mb: Multoid[C2,V2]) =
    new Multoid[(C1,C2),(V1,V2)] {
      override def nil = (ma.nil, mb.nil)
      override def insert(c: (C1,C2), v: (V1,V2)) =
        (ma.insert(c._1, v._1), mb.insert(c._2, v._2))
    }

  implicit def Tuple3Multoid[C1,V1,C2,V2,C3,V3]
                (implicit ma: Multoid[C1,V1], mb: Multoid[C2,V2],
                           mc: Multoid[C3,V3]) =
    new Multoid[(C1,C2,C3),(V1,V2,V3)] {
      override def nil = (ma.nil, mb.nil, mc.nil)
      override def insert(c: (C1,C2,C3), v: (V1,V2,V3)) =
        (ma.insert(c._1,v._1), mb.insert(c._2,v._2), mc.insert(c._3,v._3))
    }

  implicit def Tuple4Multoid[C1,V1,C2,V2,C3,V3,C4,V4]
                (implicit ma: Multoid[C1,V1], mb: Multoid[C2,V2],
                           mc: Multoid[C3,V3], md: Multoid[C4,V4]) =
    new Multoid[(C1,C2,C3,C4),(V1,V2,V3,V4)] {
      override def nil = (ma.nil, mb.nil, mc.nil, md.nil)
      override def insert(c: (C1,C2,C3,C4), v: (V1,V2,V3,V4)) =
        (ma.insert(c._1,v._1), mb.insert(c._2,v._2),
         mc.insert(c._3,v._3), md.insert(c._4,v._4))
    }

  implicit def MapMultoid[Key, Value, Repr[K,V] <: Map[K,V], Elem]
               (implicit m: Multoid[Value,Elem],
                          bf: CanBuildFrom[Repr[Key,Value],
                                           (Key,Value),
                                           Repr[Key,Value]]) =
    new Multoid[Repr[Key,Value],(Key,Elem)] {
      override def nil = bf().result
      override def insert(c: Repr[Key,Value], a: (Key,Elem)) =
        (c + (a._1 -> c.get( a._1 )
                       .map( v => m.insert(v,a._2) )
                       .getOrElse( m.insert(m.nil,a._2) ) )
        ).asInstanceOf[Repr[Key,Value]]
    }

  class MultoidIdentity[Coll](c: Coll) {
    def |<|[Elem](e: Elem)(implicit m: Multoid[Coll,Elem]) = m.insert(c, e)
  }
  implicit def mkMultoid[Coll](c: Coll) = new MultoidIdentity[Coll](c)

}

