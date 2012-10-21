package nl.tudelft.courses.in4355.github_relations_viz

import scalaz._
import Monoids._
import Pluroids._
import scala.annotation.implicitNotFound

object MapReduce {
  
  trait MapReduceStep[X,Y,T] {
    def zero: T
    def mapper(x: X): Y
    def reducer(t: T, y: Y): T
  }
  
  def mapReduce[X,Y,T](s: MapReduceStep[X,Y,T])(xs: Traversable[X]): T =
    (s.zero /: xs)( (t,x) => s.reducer(t, s.mapper(x)) )

  @implicitNotFound(msg="Cannot find Monoid to use for mapReduceM")
  def mapReduceM[A,B](f: A => B)(as: Traversable[A])(implicit m: Monoid[B]) = 
  	(m.zero /: as)( (b,a) => m.append(b,f(a)) )

  @implicitNotFound(msg="Cannot find Pluroid to use for mapReduceP")
  def mapReduceP[A,B,C](f: A => B)(as: Traversable[A])(implicit p: Pluroid[C,B]) =
  	(p.zero /: as)( (c,a) => p.append(c,f(a)))
    
}