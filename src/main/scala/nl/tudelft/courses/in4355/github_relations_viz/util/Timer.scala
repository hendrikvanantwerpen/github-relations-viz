/* Copyright 2012-2013 The Github-Relations-Viz Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
