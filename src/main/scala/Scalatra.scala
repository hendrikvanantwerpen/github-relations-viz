import nl.tudelft.courses.in4355.github_relations_viz._
import org.scalatra._
import javax.servlet.ServletContext

/**
 * This is the Scalatra bootstrap file. You can use it to mount servlets or
 * filters. It's also a good place to put initialization code which needs to
 * run at application start (e.g. database configurations), and init params.
 */
class Scalatra extends LifeCycle {
  override def init(context: ServletContext) {
    val rest = 
    context.mount(new GHRelationsVizServlet, "/*")
  }
}
