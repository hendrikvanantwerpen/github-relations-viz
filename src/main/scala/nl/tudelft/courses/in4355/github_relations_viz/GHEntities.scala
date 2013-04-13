/* Copyright 2012-2013 The Github-Relations-Viz Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.tudelft.courses.in4355.github_relations_viz

object GHEntities {

  type UserRef = Int
  type ProjectRef = Int
  type LangRef = Int
  
  case class User(id:Int,login:String,name:String) {
    override def equals(a: Any) = a.isInstanceOf[User] && a.asInstanceOf[User].id == id
    override def hashCode = id
  }
  object User {
    def unknown(id: Int) = User(id,"Unknown user %d".format(id),"")
  }
  case class Project(id:Int,owner:UserRef,name:String,lang:LangRef,desc:String) {
    override def equals(a: Any) = a.isInstanceOf[Project] && a.asInstanceOf[Project].id == id
    override def hashCode = id
  }
  object Project {
    def unknown(id: Int) = Project(id,-1,"Unknown project %d".format(id),-1,"")    
  }
  case class Interaction(user:UserRef,project:ProjectRef)
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
  
}
