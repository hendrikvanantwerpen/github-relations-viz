package nl.tudelft.courses.in4355.github_relations_viz

import scala.io.Source
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import GHRelationsViz._

class GHRelationsVizSuite extends FunSuite with ShouldMatchers {

  def get(res: String) = Source.fromURL(getClass.getResource(res))

  test("read commits from file") {
    val src = get("/one-commit.txt")
    readCommitsFromSource(src) should be === (List(Commit(Project(1000,"someproject"),User(2000,"someuser"),999)))
  }

  test("filter commits by date") {
    val p = Project(1,"scala")
    val u = User(1,"john")
    List(Commit(p,u,1)).filter( isCommitInRange(_, 2, 4) ).size should be === (0)
    List(Commit(p,u,2)).filter( isCommitInRange(_, 2, 4) ).size should be === (1)
    List(Commit(p,u,4)).filter( isCommitInRange(_, 2, 4) ).size should be === (1)
    List(Commit(p,u,5)).filter( isCommitInRange(_, 2, 4) ).size should be === (0)
    List(Commit(p,u,4),Commit(p,u,2),Commit(p,u,1)).filter( isCommitInRange(_, 2, 4) ).size should be === (2)
  }

}
