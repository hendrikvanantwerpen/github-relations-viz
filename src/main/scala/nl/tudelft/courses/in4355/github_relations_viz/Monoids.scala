package nl.tudelft.courses.in4355.github_relations_viz

import scala.collection.immutable._
import scalaz._
import Scalaz._

object Monoids {

  implicit def OptionNothingZero: Zero[Option[Nothing]] = zero(None)

  implicit def OptionNothingSemigroup: Semigroup[Option[Nothing]] = semigroup( (_,_) => None )

  implicit def SortedMapZero[A,B](implicit ord: scala.math.Ordering[A]): Zero[SortedMap[A,B]] =
    zero(SortedMap.empty[A,B])

  implicit def SortedMapSemigroup[A,B](implicit ord: scala.math.Ordering[A], sg: Semigroup[B]): Semigroup[SortedMap[A,B]] =
    semigroup((ma, mb) => mb.foldLeft(ma)( (m,b) => m + m.get(b._1).map( v => b._1 -> (v |+| b._2) ).getOrElse(b) ))
  
}