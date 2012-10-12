package nl.tudelft.courses.in4355.github_relations_viz
import scala.util.matching.Regex

	
object HelloWorld {
	val TReg = """(\w+),(\w+),(\w+),(\w+),(\w+)""".r
	
	type Commit = (Int, String, Int, String, Int)
	type User = (Int, String)
	type Project = (Int, String)
	type projectsPerUser =  Map[User, Set[Project]]
  
	//Geeft de file als dataarray terug
	def readFile(filename: String) = {
	  scala.io.Source.fromFile(filename).getLines
	}
	
	
	def main(args: Array[String]) {
		println(System.getProperty("user.dir"))
		println("WHAAAA RUN!!!!");
		( readFile("gittext.gittext")
		  filter( l => !(l isEmpty) )
		  map(parseLine)
		  foldLeft(Map.empty)(groupOperator)
		)
	}
	

	
	def groupOperator(commit: Commit, state: projectsPerUser): projectsPerUser = {
	  asdsaa
	}
	
	/**
	 * Parse a single line from the data into a tuple
	 * @param line the input line as string
	 * @return the tuple with data
	 */
	def parseLine(line: String) = {
	  val TReg(pId, pName, uId, uName, ts) = line
	  (pId.toInt, pName, uId.toInt, uName, ts.toInt)
	}
}