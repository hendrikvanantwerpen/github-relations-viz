package nl.tudelft.courses.in4355.github_relations_viz

object Logger {

  class Loggable[A](a: A) {
    def log( f: A => Unit ): A = { f(a); a }
    def log( f: => Unit ): A = { f; a }
  }
  implicit def mkLoggable[A](a: A): Loggable[A] =
    new Loggable[A](a)

}
