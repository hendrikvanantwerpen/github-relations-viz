package nl.tudelft.courses.in4355.github_relations_viz

import scalaz._
import Scalaz._

object Monoids {

  implicit object OptionNothingZero extends Zero[Option[Nothing]] {
    val zero = None
  }

  implicit object OptionNothingSemigroup extends Semigroup[Option[Nothing]] {
    def append(s1: Option[Nothing], s2: => Option[Nothing]) = None
  }  

}