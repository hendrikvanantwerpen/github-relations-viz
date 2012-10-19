package nl.tudelft.courses.in4355.github_relations_viz

import scala.io.Source
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import GHEntities._
import GHRelationsViz._

class GHRelationsVizSuite extends FunSuite with ShouldMatchers {

  def get(res: String) = Source.fromURL(getClass.getResource(res))

  test("read commits from file") {
    val src = get("/one-commit.txt")
    readCommitsFromSource(src).toList should be === (List(Commit(Project(1000,"someproject"),User(2000,"someuser"),999)))
  }

  test("filter commits by date") {
    val src = get("/some-commits.txt")
    val cs = readCommitsFromSource(src)
    cs.filter( isCommitInRange(_, 150, 250) ).size should be === (2)
    /*cs.filter( isCommitInRange(_, 550, 600) ).size should be === (0)
    cs.filter( isCommitInRange(_, 0, 1000) ).size should be === (8)
    cs.filter( isCommitInRange(_, 200, 300) ).size should be === (4)
    cs.filter( isCommitInRange(_, 500, 500) ).size should be === (2)*/
  }

}
