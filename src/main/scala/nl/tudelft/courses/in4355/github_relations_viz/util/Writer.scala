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

trait Writer {
  def write(msg: Any)
  def writeln(msg: Any)
}

class FileWriter(filename: String) extends Writer {
  private val res = scalax.io.Resource.fromFile(filename)
  res.truncate(0)
  private val writer = res.writer
  override def write(msg: Any) = writer.write(msg.toString)
  override def writeln(msg: Any) = writer.write(msg.toString+"\n")
}

class TeeWriter(writer: Writer) extends Writer {
  override def write(msg: Any) = { print(msg.toString); writer.write(msg) }
  override def writeln(msg: Any) = { println(msg.toString); writer.writeln(msg.toString) }
}
