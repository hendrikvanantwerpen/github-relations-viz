package nl.tudelft.courses.in4355.github_relations_viz

object Timer {
  var lastTime: Long = System.currentTimeMillis
  
  def tick() = {
    val old = lastTime
    lastTime = System.currentTimeMillis
    lastTime - old
  }
  
}
