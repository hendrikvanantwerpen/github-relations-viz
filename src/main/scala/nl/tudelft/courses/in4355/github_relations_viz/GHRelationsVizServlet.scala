package nl.tudelft.courses.in4355.github_relations_viz

import org.scalatra._
import scalate.ScalateSupport
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Serialization.write

class GHRelationsVizServlet extends ScalatraServlet {

  implicit val formats = Serialization.formats(NoTypeHints)

  val url = getClass.getResource("/commits.txt")
  val processor = new GHRelationsViz(url)
  
  get("/range") {
    write(processor.getLimits)
  }
  
  get("/jitdata") {
    val from = params get "from" map( _.toInt ) getOrElse( Int.MinValue )
    val to = params get "to" map( _.toInt ) getOrElse( Int.MaxValue )
    val minDegree = params get "degree" map( d => math.max(1,d.toInt) ) getOrElse( 1 )
    contentType = "application/json;charset=UTF-8"
    write(processor.getJITData(from,to,minDegree))
  }
    
  get("/d3data") {
    val from = params get "from" map( _.toInt ) getOrElse( Int.MinValue )
    val to = params get "to" map( _.toInt ) getOrElse( Int.MaxValue )
    val minDegree = params get "degree" map( d => math.max(1,d.toInt) ) getOrElse( 1 )
    contentType = "application/json;charset=UTF-8"
    write(processor.getD3Data(from,to,minDegree))
  }

}
