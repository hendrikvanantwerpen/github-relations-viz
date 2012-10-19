package nl.tudelft.courses.in4355.github_relations_viz

object GHRelationsApp extends App {
  val src = scala.io.Source.fromURL(getClass.getResource("/commits.txt"))
  val proc = new GHRelationsViz(src)
  proc.getProjectRelations(Int.MinValue,Int.MaxValue)
}