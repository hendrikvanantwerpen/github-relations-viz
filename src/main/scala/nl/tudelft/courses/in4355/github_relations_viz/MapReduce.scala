package nl.tudelft.courses.in4355.github_relations_viz

import scalaz._
import Scalaz._

object MapReduce {
  
  def mapReduce[X,T](f: X => T)(xs: Traversable[X])(implicit m: Monoid[T]): T =
    (m.zero /: xs.par)( (t,x) => m.append(t,f(x)) )
  
}