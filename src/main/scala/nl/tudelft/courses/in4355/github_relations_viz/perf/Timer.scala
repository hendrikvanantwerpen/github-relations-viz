package nl.tudelft.courses.in4355.github_relations_viz

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit._

object Timer {
  
  def timed[A](msg: String)(f: => A, timeUnit: TimeUnit = MILLISECONDS): A = {
    print(msg+" ...")
    val start = System.nanoTime()
	val result: A = f
	val end = System.nanoTime()
	println(" in "+timeUnit.convert(end-start, NANOSECONDS)+"ms")
	result
  }
}