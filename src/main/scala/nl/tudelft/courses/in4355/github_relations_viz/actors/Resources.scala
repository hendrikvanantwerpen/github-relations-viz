package nl.tudelft.courses.in4355.github_relations_viz.actors

import nl.tudelft.courses.in4355.github_relations_viz._
import java.net.URL


object GHResources {
    println("Reading all commits")
	val commits = GHRelationsViz.readCommits(new URL("file:///Users/nielsvankaam/Documents/Studie/FuncProgramming/repo/github-relations-viz/commits/commits.txt"))
	println("Done reading all commits")
	
	def getCommits = {
	  commits
    }
}

	