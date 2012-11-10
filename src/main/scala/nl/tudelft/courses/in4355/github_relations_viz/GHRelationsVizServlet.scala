package nl.tudelft.courses.in4355.github_relations_viz

import org.scalatra._
import scalate.ScalateSupport
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.liftweb.json.Serialization.write
import java.net.URL

class GHRelationsVizServlet extends ScalatraServlet {

  implicit val formats = Serialization.formats(NoTypeHints)

  println( "Create data processor" )
  val PERIOD = 7 * 24 * 3600
  val datadir = "file:///home/hendrik/edu/tudelft/in4355.bzr/github-relations-viz.git/commits"
  val projectsurl = new URL(datadir+"/projects.txt")
  val usersurl = new URL(datadir+"/users.txt")
  val commitsurl = new URL(datadir+"/commits.txt")
  val processor = new GHRelationsViz(projectsurl,usersurl,commitsurl,PERIOD)
  println( "Ready to go!" )
  
  get("/range") {
    write(processor.getLimits)
  }
      
  get("/d3data") {
    val from = params get "from" map( _.toInt ) getOrElse( Int.MinValue )
    val to = params get "to" map( _.toInt ) getOrElse( Int.MaxValue )
    val minDegree = params get "degree" map( d => math.max(1,d.toInt) ) getOrElse( 1 )
    contentType = "application/json;charset=UTF-8"
    write(processor.getD3Data(from,to,minDegree))
  }

}
