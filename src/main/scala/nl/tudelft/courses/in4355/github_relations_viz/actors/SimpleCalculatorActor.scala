package nl.tudelft.courses.in4355.github_relations_viz.actors
import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import java.io.File

//#actor
class SimpleCalculatorActor extends Actor {
  def receive = {
    case Add(n1, n2) =>
      println("Calculating %d + %d".format(n1, n2))
      sender ! AddResult(n1, n2, n1 + n2)
    case Subtract(n1, n2) =>
      println("Calculating %d - %d".format(n1, n2))
      sender ! SubtractResult(n1, n2, n1 - n2)
    
  }
}
//#actor

class CalculatorApplication {
  //#setup
  println(new File(".").getAbsolutePath()) 
  val system = ActorSystem("CalculatorApplication", ConfigFactory.load.getConfig("calculator"))
  val actor = system.actorOf(Props[SimpleCalculatorActor], "simpleCalculator")
  //#setup

  def startup() {
  }

  def shutdown() {
    system.shutdown()
  }
}

object CalcApp {
  def main(args: Array[String]) {
    new CalculatorApplication
    println("Started Calculator Application - waiting for messages")
  }
}