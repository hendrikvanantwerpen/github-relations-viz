package nl.tudelft.courses.in4355.github_relations_viz


import net.liftweb.json.JsonAST._
import net.liftweb.json.Extraction._
import net.liftweb.json.Printer._    


object GHRelationsApp extends App {

  
  val src = scala.io.Source.fromURL(getClass.getResource("/commits.txt"))
  val proc = new GHRelationsViz(src)
  val raw = proc.getProjectRelations(Int.MinValue,Int.MaxValue,1)
  
  implicit val formats = net.liftweb.json.DefaultFormats
  
  val output = pretty(render(decompose(raw)))
  val out = new java.io.FileWriter("output.json")
  out.write(output)
  out.close
  
  println(output)

}