package nl.tudelft.courses.in4355.github_relations_viz

import nl.tudelft.courses.in4355.github_relations_viz.GHEntities._

class LangFilter(langFilter: Map[LangRef,Boolean], langStrict: Boolean) {

  def apply(lang: LangRef) : Boolean = langFilter get lang getOrElse !langStrict 
  
  override def toString = langFilter.toString + " (" + {if (!langStrict) "non-"} + "strict)"
  
}