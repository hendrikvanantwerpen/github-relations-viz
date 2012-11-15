package nl.tudelft.courses.in4355.github_relations_viz

import akka.actor.ActorSystem
import java.net.URL
import net.liftweb.json.JsonAST._
import net.liftweb.json.Extraction._
import net.liftweb.json.Printer._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Serialization.write
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import org.scalatra.ScalatraServlet
import org.scalatra.akka.AkkaSupport
import scala.collection.{GenMap,GenSeq}
import GHEntities._
import com.typesafe.config.ConfigFactory
import org.scalatra.Ok

class GHRelationsVizServlet extends ScalatraServlet with AkkaSupport {

  val epoch1990 = 631148400
  val epoch2015 = 1420066800

  implicit val formats = net.liftweb.json.DefaultFormats

  println( "Create data processor" )
  val PERIOD = 7 * 24 * 3600
  val LINK_LIMIT = 10000
  val datadir = "file:commits/"
  val projectsurl = new URL(datadir+"projects.txt")
  val usersurl = new URL(datadir+"users.txt")
  val forksurl = new URL(datadir+"forks.txt")
  val commitsurl = new URL(datadir+"smallcommits.txt")
  val (system: ActorSystem, processor:GHRelationsViz) =
    if ( false ) {
      val s = ActorSystem("ghlink", ConfigFactory.load.getConfig("LinkCombine"))
      (s,new GHRelationsVizDist(projectsurl,usersurl,s))
    } else {
      (ActorSystem(),
       new GHRelationsVizLocal(projectsurl,usersurl,forksurl,commitsurl,epoch1990,epoch2015,PERIOD))
    }
  println( "Ready to go!" )
  
  get("/range") {
    Ok(write(processor.getLimits))
  }

  get("/links") {
    val from = params get "from" map( _.toInt ) getOrElse( Int.MinValue )
    val to = params get "to" map( _.toInt ) getOrElse( Int.MaxValue )
    val minWeight = params get "minWeight" map( d => math.max(1,d.toInt) ) getOrElse( 1 )
    contentType = "application/json;charset=UTF-8"
    processor.getProjectLinks(from, to, minWeight)
             .map { links => 
                  if ( links.size > LINK_LIMIT ) {
                      Ok(write(("error" -> "Too many links found. Please limit selection."):JObject))
                  } else {
                      Ok(write(getGraphJSON(links)))
                  }
              }
  }

  get("/hist") {
    contentType = "application/json;charset=UTF-8"
    processor.getUserProjectsLinksPerWeek
             .map( h => Ok(write(getHistJSON(h))) )
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
        .par.map( e => getProjectJSON(processor.getProject(e._1),e._2) )
    val d3links =
      links.map( e => getLinkJSON(e._1,e._2) )
    println( "Return D3 graph" )
     ("links" -> d3links.seq) ~ ("projects" -> d3nodes.seq)
  }  
  
  def getUserJSON(user: User) = {
    ( ("id" -> user.id) ~
      ("login" -> user.login) ~
      ("name" -> user.name) )
  }
  
  def getProjectJSON(project: Project, value: Int) = {
    ( ("id" -> project.id) ~
      ("name" -> project.name) ~
      ("desc" -> project.desc) ~
      ("lang" -> project.lang) ~
      ("owner" -> getUserJSON(processor.getUser(project.owner))) ~
      ("value" -> value) )
  }
  
  def getLinkJSON(l: Link, value: Int) = {
    ( ("project1" -> l.project1) ~
      ("project2" -> l.project2) ~
      ("value" -> value) )
  }
  
  def linkToProjects(l: Link) =
    Set(l.project1,l.project2)
    
  def getHistJSON( userProjectLinksPerWeek: GenSeq[(Int,Int)] ) = {
    userProjectLinksPerWeek
      .map( dc => ("date" -> dc._1) ~ ("count" -> dc._2) )
  }
    
}
