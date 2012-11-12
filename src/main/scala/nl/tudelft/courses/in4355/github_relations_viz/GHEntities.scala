package nl.tudelft.courses.in4355.github_relations_viz

object GHEntities {

  type UserRef = Int
  type ProjectRef = Int

  case class Range(min:Int,max:Int)
  case class User(id:Int,login:String,name:String)
  case class Project(id:Int,name:String,lang:String,desc:String)
  case class Commit(projectId:Int,userId:Int,timestamp:Int)
  case class Fork(projectId:Int,parentId:Int)
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
