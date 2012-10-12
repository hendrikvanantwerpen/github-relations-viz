import nl.tudelft.courses.in4355.github_relations_viz._
import akka.actor.ActorSystem
import org.scalatra._
import javax.servlet.ServletContext
import com.typesafe.config.ConfigFactory

/**
 * This is the Scalatra bootstrap file. You can use it to mount servlets or
 * filters. It's also a good place to put initialization code which needs to
 * run at application start (e.g. database configurations), and init params.
 */
class Scalatra extends LifeCycle {
  override def init(context: ServletContext) {
    val config = ConfigFactory.load()
    val app = ActorSystem("GithubRelationsViz", config.getConfig("myapp1").withFallback(config))
    val rest = new GHRelationsVizServlet
    context.mount(rest, "/*")
  }
}
