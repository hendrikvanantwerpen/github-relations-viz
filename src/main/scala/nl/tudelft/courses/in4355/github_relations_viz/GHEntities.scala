package nl.tudelft.courses.in4355.github_relations_viz

object GHEntities {

  type UserRef = Int
  type ProjectRef = Int
  
  case class User(id:Int,login:String,name:String) {
    override def equals(a: Any) = a.isInstanceOf[User] && a.asInstanceOf[User].id == id
    override def hashCode = id
  }
  object User {
    def unknown(id: Int) = User(id,"Unknown user %d".format(id),"")
  }
  case class Project(id:Int,owner:UserRef,name:String,lang:String,desc:String) {
    override def equals(a: Any) = a.isInstanceOf[Project] && a.asInstanceOf[Project].id == id
    override def hashCode = id
  }
  object Project {
    def unknown(id: Int) = Project(id,-1,"Unknown project %d".format(id),"","")    
  }
  case class Commit(project:ProjectRef,user:UserRef,timestamp:Int)
  case class Fork(project:ProjectRef,parent:ProjectRef)
  case class Link(project1:ProjectRef,project2:ProjectRef) {
    def normalize = {
      if ( project2 < project1 ) {
        Link(project2,project1)
      } else {
        this
      }
    }
  }
  
  case class Range(min:Int,max:Int,step:Int)
}
