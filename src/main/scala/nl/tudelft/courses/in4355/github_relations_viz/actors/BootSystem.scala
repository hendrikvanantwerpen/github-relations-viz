package nl.tudelft.courses.in4355.github_relations_viz.actors

import akka.actor.AddressFromURIString

object boot {
  def main(args: Array[String]) {
    val vankaamserver = AddressFromURIString("akka://ghlink@vankaam.net:2553")
    
  }
}