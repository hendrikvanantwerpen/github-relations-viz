package nl.tudelft.courses.in4355.github_relations_viz

import org.scalatra._
import scalate.ScalateSupport
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Serialization.write

import GHRelationsViz._

case class SomeResponse

class GHRelationsVizServlet extends ScalatraServlet {
  
  get("/data") {
    val from = params get "from" map( _.toInt ) getOrElse( Int.MinValue )
    val to = params get "to" map( _.toInt ) getOrElse( Int.MaxValue )
    contentType = "application/json;charset=UTF-8"
    implicit val formats = Serialization.formats(NoTypeHints)
    write(getProjectRelations(from,to))
  }
    
}
