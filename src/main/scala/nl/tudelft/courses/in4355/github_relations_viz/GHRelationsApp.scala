package nl.tudelft.courses.in4355.github_relations_viz


import net.liftweb.json.JsonAST._
import net.liftweb.json.Extraction._
import net.liftweb.json.Printer._    


object GHRelationsApp extends App {
  
  val proc = new GHRelationsViz(getClass.getResource("/commits.txt"))
  val range = proc.getLimits
  println("Found range "+range.min+" to "+range.max)
  val links = proc.getProjectLinks(Int.MinValue,Int.MaxValue,1)
  
  /*
  implicit val formats = net.liftweb.json.DefaultFormats
  val json = pretty(render(decompose(links)))
  val out = new java.io.FileWriter("output.json")
  out.write(json)
  out.close
  
  println(json)
  */

}