package nl.tudelft.courses.in4355.github_relations_viz.actors

import akka.actor.Actor
import java.net.URL
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities.Link

trait ActorCommand

case class obtainLinks(from : Int, until : Int) extends ActorCommand

trait linkResults

case class linkResult(map: Map[Link, Int])