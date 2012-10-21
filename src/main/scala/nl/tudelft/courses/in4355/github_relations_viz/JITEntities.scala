package nl.tudelft.courses.in4355.github_relations_viz

object JITEntities {
  case class JITGraphNode(id: String, name: String, adjacencies: Option[Set[String]])
}