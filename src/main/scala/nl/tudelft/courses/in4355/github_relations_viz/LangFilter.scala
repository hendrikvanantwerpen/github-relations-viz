/* Copyright 2012-2013 The Github-Relations-Viz Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.tudelft.courses.in4355.github_relations_viz

import nl.tudelft.courses.in4355.github_relations_viz.GHEntities._

class LangFilter(langFilter: Map[LangRef,Boolean], langStrict: Boolean) {

  def apply(lang: LangRef) : Boolean = langFilter get lang getOrElse !langStrict 
  
  override def toString = langFilter.toString + " (" + {if (!langStrict) "non-"} + "strict)"
  
}
