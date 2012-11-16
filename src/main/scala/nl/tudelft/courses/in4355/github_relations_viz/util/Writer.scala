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