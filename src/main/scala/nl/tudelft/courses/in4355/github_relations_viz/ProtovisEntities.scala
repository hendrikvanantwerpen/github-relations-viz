package nl.tudelft.courses.in4355.github_relations_viz

object ProtovisEntities {

  case class PVGraph(nodes: Seq[PVNode], links: Seq[PVLink])
  case class PVNode(nodeName: String)
  case class PVLink(source: Int, target: Int, value: Int)
  
}