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

import scala.io.Source
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import GHEntities._

class GHRelationsVizSuite extends FunSuite with ShouldMatchers {

  def get(res: String) = Source.fromURL(getClass.getResource(res))

  test("read commits from file") {
    val src = get("/one-commit.txt")
   // val proc = new GHRelationsViz(src)
   // proc.commits should be === (List(Commit(Project(1000,"someproject"),User(2000,"someuser"),999)))
  }

  /*test("filter commits by date") {
    val src = get("/one-commit.txt")
    val proc = new GHRelationsViz(src)
    val cs = proc.commits
    cs.filter( isCommitInRange(_, 150, 250) ).size should be === (2)
    cs.filter( isCommitInRange(_, 550, 600) ).size should be === (0)
    cs.filter( isCommitInRange(_, 0, 1000) ).size should be === (8)
    cs.filter( isCommitInRange(_, 200, 300) ).size should be === (4)
    cs.filter( isCommitInRange(_, 500, 500) ).size should be === (2)
  }*/

}
