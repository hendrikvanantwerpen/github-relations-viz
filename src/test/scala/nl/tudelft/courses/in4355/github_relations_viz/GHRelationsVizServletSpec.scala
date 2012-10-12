package nl.tudelft.courses.in4355.github_relations_viz

import org.scalatra.test.specs2._

// For more on Specs2, see http://etorreborre.github.com/specs2/guide/org.specs2.guide.QuickStart.html
class GHRelationsVizServletSpec extends ScalatraSpec { def is =
  "GET / on GHRelationsVizServlet"                     ^
    "should return status 200"                  ! root200^
                                                end

  addServlet(classOf[GHRelationsVizServlet], "/*")

  def root200 = get("/") {
    status must_== 200
  }
}
