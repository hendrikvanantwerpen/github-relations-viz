package nl.tudelft.courses.in4355.github_relations_viz

object GHRelationsApp extends App {
  import GHRelationsViz._
  getProjectRelations(Int.MinValue,Int.MaxValue)
}