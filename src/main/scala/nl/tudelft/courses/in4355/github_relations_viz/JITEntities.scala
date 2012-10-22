package nl.tudelft.courses.in4355.github_relations_viz

object JITEntities {

  case class JITNode(id: String, name: String, adjacencies: Option[TraversableOnce[String]])

}