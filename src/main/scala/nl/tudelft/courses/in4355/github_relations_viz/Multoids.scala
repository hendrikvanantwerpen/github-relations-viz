package nl.tudelft.courses.in4355.github_relations_viz

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

object Multoids {

  /* Fourth generation
   *  - Collections and Maps are completely inferred, several layers deep.
   *    The trick was to make Repr explicit in the implicit function, although it
   *    is not in the resulting Multoid
   *  - Monoids are auto wrapped in multoids to continue the work
   * Todo
   *  - Drop the builders, but do it in a typesafe matter (not asInstanceOf)
   *    This makes it very slow again, many objects created and copied
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

  /* this one is not explicit because it causes ambiguitues with
   * the map multoid, apparently there's no more-specific preference here
   */
  def CollectionMultoid[Elem, Repr[X] <: TraversableOnce[X]]
      (bf: CanBuildFrom[Repr[Elem],Elem,Repr[Elem]]) =
    new Multoid[Repr[Elem],Elem] {
      override def nil = bf().result
      override def insert(c: Repr[Elem], a: Elem) = (bf(c) ++=(c) +=(a)).result
    }

  implicit def SeqMultoid[Elem, Repr[X] <: Seq[X]]
               (implicit bf: CanBuildFrom[Repr[Elem],Elem,Repr[Elem]]) =
    CollectionMultoid[Elem, Repr](bf)

  implicit def SetMultoid[Elem, Repr[X] <: Set[X]]
               (implicit bf: CanBuildFrom[Repr[Elem],Elem,Repr[Elem]]) =
    CollectionMultoid[Elem, Repr](bf)

  implicit def Tuple2Multoid[C1,V1,C2,V2]
               (implicit ma: Multoid[C1,V1], mb: Multoid[C2,V2]) =
    new Multoid[(C1,C2),(V1,V2)] {
      override def nil = (ma.nil, mb.nil)
      override def insert(c: (C1,C2), v: (V1,V2)) = (ma.insert(c._1, v._1), mb.insert(c._2, v._2))
    }

  implicit def MapMultoid[Key, Value, Repr[K,V] <: Map[K,V], Elem]
               (implicit m: Multoid[Value,Elem], bf: CanBuildFrom[Repr[Key,Value], (Key,Value), Repr[Key,Value]]) =
    new Multoid[Repr[Key,Value],(Key,Elem)] {
      override def nil = bf().result
      override def insert(c: Repr[Key,Value], a: (Key,Elem)) =
        (bf(c) ++=(c) +=(a._1 -> c.get( a._1 ).map( v => m.insert(v,a._2) ).getOrElse( m.insert(m.nil,a._2) ))).result
    }

}

object MultoidsTest extends App {
  import Monoids._
  import Multoids._
  import MapReduce._
  
  val words = List("aap","anders","bijna","boer","noot","mies","meisje","zaag","zonder","fiets","zonder","aap","mies","aap")
  
  def wordCount(s: String) = (s,1)
  val wordMap = mapReduce[String,(String,Int),Map[String,Int]](wordCount)(words)
  println(wordMap)

  def wordLength(s: String) = (s.size,s)
  val wordLengths = mapReduce[String,(Int,String),SortedMap[Int,String]](wordLength)(words)
  println(wordLengths)
  
  def countAndConcat(s: String) = (1,s)
  implicit val oops = MonoidMultoid[(Int,String)]
  val countedAndTogether = mapReduce[String,(Int,String),(Int,String)](wordInstance)(words)
  println(countedAndTogether)

  def wordInstance(s: String) = (1,s)
  val totalWords = mapReduce[String,(Int,String),(Int,Set[String])](wordInstance)(words)
  println(totalWords)

  def word(s: String) = s
  val wordSet = mapReduce[String,String,SortedSet[String]](word)(words)
  println(wordSet)

  def nestedWordCount(s: String) = (s.head.toString,(s,1))
  val nestedWordMap = mapReduce[String,(String,(String,Int)),SortedMap[String,Map[String,Int]]](nestedWordCount)(words)
  println(nestedWordMap)
  
  def nestedWordLength(s: String) = (s.size,(s,1))
  val nestedWordLengths = mapReduce[String,(Int,(String,Int)),SortedMap[Int,Map[String,Int]]](nestedWordLength)(words)
  println(nestedWordLengths)

  def goCrazy(s: String) = (s.size,(s,("count",1)))
  val crazy = mapReduce[String,(Int,(String,(String,Int))),SortedMap[Int,Map[String,Map[String,Int]]]](goCrazy)(words)
  println(crazy)
  
}
