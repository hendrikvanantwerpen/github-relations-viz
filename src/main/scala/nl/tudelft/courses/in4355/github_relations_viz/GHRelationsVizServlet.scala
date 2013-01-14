package nl.tudelft.courses.in4355.github_relations_viz

import akka.actor.ActorSystem
import java.net.URL
import net.liftweb.json.{parse}
import net.liftweb.json.JsonAST._
import net.liftweb.json.Extraction._
import net.liftweb.json.Printer._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Serialization.write
import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import net.van_antwerpen.scala.collection.mapreduce.MapReduce._
import org.scalatra.ScalatraServlet
import org.scalatra.akka.AkkaSupport
import scala.collection.{GenMap,GenIterable}
import GHEntities._
import org.scalatra.Ok
import util.Timer._

class GHRelationsVizServlet extends ScalatraServlet {

  private val epoch1990 = 631148400
  private val epoch2015 = 1420066800

  implicit val formats = net.liftweb.json.DefaultFormats

  println( "Create data processor" )
  private val PERIOD = 7 * 24 * 3600
  private val datadir = "file:commits/"
  private val projectsurl = new URL(datadir+"projects.txt")
  private val usersurl = new URL(datadir+"users.txt")
  private val forksurl = new URL(datadir+"forks.txt")
  private val commitsurl = new URL(datadir+"smallcommits.txt")
  private val processor =
    new GHRelationsVizLocal(projectsurl,usersurl,forksurl,commitsurl,epoch1990,epoch2015,PERIOD)
  println( "Ready to go!" )
  
  /* UI
   * option for auto-add languages
   * reset or jump to fixed nodes
   * (semi-)automatic time run with current settings (time-window,languages,fixed projects)
   */
  
  private def time_params = {
    val from = params get "from" map (_.toInt) getOrElse( Int.MinValue )
    val to = params get "to" map (_.toInt) getOrElse( Int.MaxValue )
    (from,to)
  }
  
  private def lang_params = {
    val includeLangs = params get "include_langs" map ( parse(_).extract[List[String]] ) getOrElse Nil
    val excludeLangs = params get "exclude_langs" map ( parse(_).extract[List[String]] ) getOrElse Nil
    val langStrict = params get "langs_strict" map ( _.toLowerCase == "true" ) getOrElse false
    val langFilter = Map.empty[String,Boolean] ++ includeLangs.map( _ -> true ) ++ excludeLangs.map( _ -> false )
    new LangFilter(langFilter map { kv => processor.getLangRef(kv._1) -> kv._2 }, langStrict)
  }
  
  get("/links") {
    val (from,to) = time_params
    val langFilter = lang_params
    val limit = params get "limit" map (_.toInt) getOrElse (Int.MaxValue)
    val minLinkWeight = params get "min_link_weight" map ( d => math.max(1,d.toInt) ) getOrElse 1
    contentType = "application/json;charset=UTF-8"
    processor.getProjectLinks(from, to, langFilter, minLinkWeight, limit)
             .fold( err => Ok(write(("error" -> (err+" Please limit selection.")):JObject)),
                    { links =>
                        val json = timed("Writing graph json",println) { write(getGraphJSON(links)) }
                        Ok(json)
                    } )
  }

  get("/hist") {
    val (from,to) = time_params
    val langFilter = lang_params
    contentType = "application/json;charset=UTF-8"
    val j = processor.getInteractionHistogram(from,to,langFilter)
    Ok(write(getHistJSON(j)))
  }
  
  get("/langs") {
    val (from,to) = time_params
    contentType = "application/json;charset=UTF-8"
    val j = processor.getLanguageHistogram(from,to)
    Ok(write(getLangsJSON(j)))
  }
  
  private def getGraphJSON(links: GenMap[Link,Int]) = {
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
    ("links" -> d3links.seq) ~ ("projects" -> d3nodes.seq)
  }  
  
  private def getUserJSON(user: User) = {
    ( ("id" -> user.id) ~
      ("login" -> user.login) ~
      ("name" -> user.name) )
  }
  
  private def getProjectJSON(project: Project, value: Int) = {
    ( ("id" -> project.id) ~
      ("name" -> project.name) ~
      ("desc" -> project.desc) ~
      ("lang" -> project.lang) ~
      ("owner" -> getUserJSON(processor.getUser(project.owner))) ~
      ("value" -> value) )
  }
  
  private def getLinkJSON(l: Link, value: Int) = {
    ( ("project1" -> l.project1) ~
      ("project2" -> l.project2) ~
      ("value" -> value) )
  }
  
  private def getLangsJSON(langsCount: GenIterable[(String,Int)]) = {
    (JObject(Nil) /: langsCount)( (j,lc) => j ~ ( lc._1 -> lc._2 ) )
  }
  
  private def linkToProjects(l: Link) =
    Set(l.project1,l.project2)
    
  private def getHistJSON( userProjectLinksPerWeek: GenIterable[(Int,Int)] ) = {
    userProjectLinksPerWeek
      .map( dc => ("date" -> dc._1) ~ ("count" -> dc._2) )
      .toList
  }
  
}
