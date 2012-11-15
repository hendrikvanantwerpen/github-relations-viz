package nl.tudelft.courses.in4355.github_relations_viz

import org.scalatra._
import scalate.ScalateSupport
import net.liftweb.json.JsonAST._
import net.liftweb.json.Extraction._
import net.liftweb.json.Printer._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Serialization.write
import java.net.URL
import GHEntities._
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import scala.collection.GenMap

class GHRelationsVizServlet extends ScalatraServlet {

  val epoch1990 = 631148400
  val epoch2015 = 1420066800  

  implicit val formats = net.liftweb.json.DefaultFormats

  println( "Create data processor" )
  val PERIOD = 7 * 24 * 3600
  val LINK_LIMIT = 50000
  val datadir = "file:commits/"
  val projectsurl = new URL(datadir+"projects.txt")
  val usersurl = new URL(datadir+"users.txt")
  val forksurl = new URL(datadir+"forks.txt")
  val commitsurl = new URL(datadir+"smallcommits.txt")
  val processor = new GHRelationsViz(projectsurl,usersurl,forksurl,commitsurl,epoch1990,epoch2015,PERIOD)
  println( "Ready to go!" )
  
  get("/range") {
    write(processor.limits)
  }
      
  get("/links") {
    val from = params get "from" map( _.toInt ) getOrElse( Int.MinValue )
    val to = params get "to" map( _.toInt ) getOrElse( Int.MaxValue )
    val minWeight = params get "minWeight" map( d => math.max(1,d.toInt) ) getOrElse( 1 )
    contentType = "application/json;charset=UTF-8"
    val links = processor.getProjectLinks(from, to, minWeight)
    if ( links.size > LINK_LIMIT ) {
      write( ("error" -> "Too many links found. Please limit selection."):JObject )
    } else {
      write(getGraphJSON(links))
    }
  }

  get("/hist") {
    contentType = "application/json;charset=UTF-8"
    write(getHistJSON())
  }
  
  def getGraphJSON(links: GenMap[Link,Int]) = {
    println( "Convert project links to D3 data" )
    val projectMap =
      links
        .mapReduce[Map[ProjectRef,Int]] { t =>
           linkToProjects(t._1).map( p => (p,1) )
         }
    val d3nodes =
      projectMap
        .par.map( e => getProjectJSON(e._1,e._2) )
    val d3links =
      links.map( e => getLinkJSON(e._1,e._2) )
    println( "Return D3 graph" )
     ("links" -> d3links.seq) ~ ("projects" -> d3nodes.seq)
  }  
  
  def getUserJSON(id: UserRef) = {
    val user = processor.getUser(id)
    ( ("id" -> user.id) ~
      ("login" -> user.login) ~
      ("name" -> user.name) )
  }
  
  def getProjectJSON(id: ProjectRef, value: Int) = {
    val project = processor.getProject(id)
    ( ("id" -> project.id) ~
      ("name" -> project.name) ~
      ("desc" -> project.desc) ~
      ("lang" -> project.lang) ~
      ("owner" -> getUserJSON(project.owner)) ~
      ("value" -> value) )
  }
  
  def getLinkJSON(l: Link, value: Int) = {
    ( ("project1" -> l.project1) ~
      ("project2" -> l.project2) ~
      ("value" -> value) )
  }
  
  def linkToProjects(l: Link) =
    Set(l.project1,l.project2)
    
  def getHistJSON() = {
    processor.userProjectLinksPerWeek
             .map( dc => ("date" -> dc._1) ~ ("count" -> dc._2) )
  }
  
  
}
