package nl.tudelft.courses.in4355.github_relations_viz

object D3Entities {

  case class D3Graph(nodes: TraversableOnce[D3Node], links: TraversableOnce[D3Link])
  case class D3Node(id: Int, name: String, weight: Int)
  case class D3Link(source: Int, target: Int, value: Int)
  
}
