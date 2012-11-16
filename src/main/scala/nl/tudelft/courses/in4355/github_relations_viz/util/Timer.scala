package nl.tudelft.courses.in4355.github_relations_viz.util

class Timer {
  private var lastTime: Long = System.nanoTime
  def tick = {
    val old = lastTime
    lastTime = System.nanoTime
    lastTime - old
  }
}

object Timer {
    
  def fmtNanoTime(t: Long) = {
    if ( t > 3600e9 ) {
      "%.2fh".format(t/3600e9)
    } else if ( t > 60e9 ) {
      "%.2fm".format(t/60e9)
    } else if ( t > 1e9 ) {
      "%.2fs".format(t/1e9)
    } else if ( t > 1e6 ) {
      "%.2fms".format(t/1e6)
    } else {
      "%.2fµs".format(t/1e3)
    }
  }

  def timed[A](msg: String, l: String => Unit)(f: => A): A = {
    l(msg+" ...")
    val start = System.nanoTime
    val result: A = f
    val end = System.nanoTime
    val dt = end-start
    l(fmtNanoTime(dt))
    result
  }
    
  class TimeFormatter(t: Long) {
    def nanoTimeToString = fmtNanoTime(t)
  }
  implicit def mkTimeFormatter(t: Long) = new TimeFormatter(t)
  
}
