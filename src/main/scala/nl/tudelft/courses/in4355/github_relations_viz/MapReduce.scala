package nl.tudelft.courses.in4355.github_relations_viz

import scalaz._
import Monoids._
import Multoids._

object MapReduce {
  
  def mapReduce[Elem,ResultElem,ResultColl](f: Elem => ResultElem)(as: TraversableOnce[Elem])(implicit p: Multoid[ResultColl,ResultElem]) =
  	(p.nil /: as)( (c,a) => p.insert(c,f(a)))
    
  def flatMapReduce[Elem, ResultElem, ResultColl](f: Elem => TraversableOnce[ResultElem])(as: Traversable[Elem])(implicit p: Multoid[ResultColl,ResultElem]) =
  	(p.nil /: as)( (c,a) => (c /: f(a))( (cc,aa) =>  p.insert(cc,aa) ) )
    
}
