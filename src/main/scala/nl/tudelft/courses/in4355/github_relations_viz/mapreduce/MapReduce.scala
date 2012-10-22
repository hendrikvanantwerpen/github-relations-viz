package nl.tudelft.courses.in4355.github_relations_viz.mapreduce

import Multoids._

object MapReduce {

  class MapReducer[Elem](as: Traversable[Elem]){
    
    def mapReduce[ResultElem, ResultColl]
                 (f: Elem => ResultElem)
                 (implicit p: Multoid[ResultColl,ResultElem]) =
  	(p.nil /: as)( (c,a) => p.insert(c, f(a)) )
  	
    def flatMapReduce[ResultElem, ResultColl]
                   (f: Elem => TraversableOnce[ResultElem])
                   (implicit p: Multoid[ResultColl,ResultElem]) =
  	  (p.nil /: as)( (c,a) => (c /: f(a))( (cc,aa) =>  p.insert(cc,aa) ) )

  }

  implicit def mkMapReducable[Elem](as: Traversable[Elem]) =
    new MapReducer[Elem](as)

}
