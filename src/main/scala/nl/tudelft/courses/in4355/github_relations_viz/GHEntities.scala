package nl.tudelft.courses.in4355.github_relations_viz

object GHEntities {

  case class Range(min:Int,max:Int)
  case class Commit(projectId:Int,userId:Int,timestamp:Int)
  case class Link(pId1:Int,pId2:Int) {
    def normalize = {
      if ( pId2 < pId1 ) {
        Link(pId2,pId1)
      } else {
        this
      }
    }
  }
}
