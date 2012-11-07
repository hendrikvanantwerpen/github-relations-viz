package nl.tudelft.courses.in4355.github_relations_viz.perf

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit._

object Timer {
  
  def timed[A](msg: String)(f: => A, timeUnit: TimeUnit = MILLISECONDS): A = {
    println(msg+" ...")
    val start = System.nanoTime()
	val result: A = f
	val end = System.nanoTime()
	println("done in "+timeUnit.convert(end-start, NANOSECONDS)+"ms")
	result
  }

}
