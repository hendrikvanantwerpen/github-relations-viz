package nl.tudelft.courses.in4355.github_relations_viz

object GHEntities {

  case class Range(min:Int,max:Int)
  case class User(id:Int,name:String)
  case class Project(id:Int,name:String)
  case class Commit(project:Project,user:User,timestamp:Int)
  case class Link(p1:Project,p2:Project) {
    def normalize = {
      if ( p2.id < p1.id ) {
        Link(p2,p1)
      } else {
        this
      }
    }
  }
}