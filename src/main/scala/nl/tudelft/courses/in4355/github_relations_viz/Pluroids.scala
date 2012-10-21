package nl.tudelft.courses.in4355.github_relations_viz

import scala.collection.generic._
import scala.collection.immutable.{SortedMap,TreeMap}
import scalaz._
import Scalaz._
import scala.math.Ordering

object PluroidsV1 {

  /* First generation Pluroids
   *  - Completely inferred Pluroids
   *  - Need two implementations, one for monoid and one for pluroid values
   *  - Pluroids are implemented for concrete types, so for every map type
   *    a new Pluroid implementation has to be made, that is identical except
   *    for return types.
   */
  
  trait Pluroid[Coll,Elem] {
    def zero: Coll
    def append(c: Coll, a: Elem): Coll
  }

  implicit def MapMPluroid[A,B](implicit m: Monoid[B]) = new Pluroid[Map[A,B],(A,B)] {
     override def zero = Map.empty
     override def append(c: Map[A,B], a: (A,B)) =
        c + (a._1 -> c.get( a._1 ).map( v => m.append(v,a._2) ).getOrElse( m.append(m.zero,a._2) ) )
  }
  
  implicit def MapPPluroid[Key,ValueColl,Value]
                (implicit p: Pluroid[ValueColl,Value]) = 
    new Pluroid[Map[Key,ValueColl],(Key,Value)] {
     override def zero = Map.empty
     override def append(c: Map[Key,ValueColl], a: (Key,Value)) =
        c + (a._1 -> c.get( a._1 ).map( v => p.append(v,a._2) ).getOrElse( p.append(p.zero,a._2) ) )
  }

}

object PluroidsV2 {

  /* Second generation Pluroids
   *  - The Pluroids are now generic over types, so all Maps share one
   *    Pluroid implementation.
   *  - Versions for Monoid and Pluroid are still very much alike.
   *  - We lost complete inference, because we want to know about the value type
   *    of the sub-pluroid. We made this a simple (i.e. not higher order) type
   *    parameter so we are not dependent on the number of type parameters of
   *    the underlying collection. But because of this, we have no access to
   *    the underlying collections value type and we must specify it, resulting
   *    in an ugly Pluroid lookup for every mapReduce call where it's used. 
   */
  
  trait Pluroid[Coll,Elem] {
    def zero: Coll
    def append(c: Coll, a: Elem): Coll
  }

  implicit def givePluroidForMonoidMap[Key,Value,MapRepr <: Map[Key,Value]]
                (implicit m: Monoid[Value], bf: CanBuildFrom[MapRepr,(Key,Value),MapRepr])
                : Pluroid[MapRepr,(Key,Value)] = {
    new Pluroid[MapRepr,(Key,Value)] {
      override def zero = bf().result
      override def append(c: MapRepr, a: (Key,Value)) = {
        (bf() ++=(c) +=(a._1 -> c.get( a._1 ).map( v => m.append(v,a._2) ).getOrElse( m.append(m.zero,a._2) ))).result
      }
    }
  }

  implicit def givePluroidForPluroidMap[Key,ValueColl,ValueElem,MapRepr <: Map[Key,ValueColl]]
                (implicit p: Pluroid[ValueColl,ValueElem], bf: CanBuildFrom[MapRepr,(Key,ValueColl),MapRepr])
                : Pluroid[MapRepr,(Key,ValueElem)] = {
    new Pluroid[MapRepr,(Key,ValueElem)] {
      override def zero = bf().result
      override def append(c: MapRepr, a: (Key,ValueElem)) = {
        (bf() ++=(c) +=(a._1 -> c.get( a._1 ).map( v => p.append(v,a._2) ).getOrElse( p.append(p.zero,a._2) ))).result
      }
    }
  }

}

object Pluroids {

  trait Pluroid[Coll,Elem] {
    def zero: Coll
    def append(c: Coll, a: Elem): Coll
  }

  /*implicit def MapMPluroid[A,B](implicit m: Monoid[B]) = new Pluroid[Map[A,B],(A,B)] {
     override def zero = Map.empty
     override def append(c: Map[A,B], a: (A,B)) =
        c + (a._1 -> c.get( a._1 ).map( v => m.append(v,a._2) ).getOrElse( m.append(m.zero,a._2) ) )
  }*/
  
  implicit def givePluroidForMonoidMap[Key,Value,MapRepr <: Map[Key,Value]]
                (implicit m: Monoid[Value], bf: CanBuildFrom[MapRepr,(Key,Value),MapRepr])
                : Pluroid[MapRepr,(Key,Value)] = {
    new Pluroid[MapRepr,(Key,Value)] {
      override def zero = bf().result
      override def append(c: MapRepr, a: (Key,Value)) = {
        (bf() ++=(c) +=(a._1 -> c.get( a._1 ).map( v => m.append(v,a._2) ).getOrElse( m.append(m.zero,a._2) ))).result
      }
    }
  }

  /*implicit def MapPPluroid[Key,ValueColl,Value]
                (implicit p: Pluroid[ValueColl,Value]) = 
    new Pluroid[Map[Key,ValueColl],(Key,Value)] {
     override def zero = Map.empty
     override def append(c: Map[Key,ValueColl], a: (Key,Value)) =
        c + (a._1 -> c.get( a._1 ).map( v => p.append(v,a._2) ).getOrElse( p.append(p.zero,a._2) ) )
  }*/

  implicit def givePluroidForPluroidMap[Key,ValueColl,ValueElem,MapRepr <: Map[Key,ValueColl]]
                (implicit p: Pluroid[ValueColl,ValueElem], bf: CanBuildFrom[MapRepr,(Key,ValueColl),MapRepr])
                : Pluroid[MapRepr,(Key,ValueElem)] = {
    new Pluroid[MapRepr,(Key,ValueElem)] {
      override def zero = bf().result
      override def append(c: MapRepr, a: (Key,ValueElem)) = {
        (bf() ++=(c) +=(a._1 -> c.get( a._1 ).map( v => p.append(v,a._2) ).getOrElse( p.append(p.zero,a._2) ))).result
      }
    }
  }

  // Pluroid[SortedMap[A,C],(A,E)] -> C = Map[E]
  // Pluroid[SortedMap[A,C],(A,E)] -> C = Map[E]
  
}

object PluroidsTest extends App {
  import Pluroids._
  import Monoids._
  import MapReduce._
  
  val words = List("aap","anders","bijna","boer","noot","mies","meisje","zaag","zonder","fiets","zonder","aap","mies","aap")
  
  def wordCount(s: String) = (s,1)
  val wordMap = mapReduceP[String,(String,Int),Map[String,Int]](wordCount)(words)
  println(wordMap)

  def wordLength(s: String) = (s.size,s)
  val wordLengths = mapReduceP[String,(Int,String),SortedMap[Int,String]](wordLength)(words)
  println(wordLengths)
  
  def nestedWordCount(s: String) = (s.head.toString,(s,1))
  val nwc = givePluroidForPluroidMap[String,Map[String,Int],(String,Int),SortedMap[String,Map[String,Int]]]
  val nestedWordMap = mapReduceP[String,(String,(String,Int)),SortedMap[String,Map[String,Int]]](nestedWordCount)(words)(nwc)
  println(nestedWordMap)
  
  def nestedWordLength(s: String) = (s.size,(s,1))
  val nwl = givePluroidForPluroidMap[Int,Map[String,Int],(String,Int),SortedMap[Int,Map[String,Int]]]
  val nestedWordLengths = mapReduceP[String,(Int,(String,Int)),SortedMap[Int,Map[String,Int]]](nestedWordLength)(words)(nwl)
  println(nestedWordLengths)
  
}
