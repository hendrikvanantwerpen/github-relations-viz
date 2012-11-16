package nl.tudelft.courses.in4355.github_relations_viz.actors

import akka.actor.Actor
import java.net.URL
import nl.tudelft.courses.in4355.github_relations_viz.GHEntities.Link
import akka.actor.Address
import scala.collection.GenMap

trait ActorCommand

case class obtainLinks(from : Int, until : Int) extends ActorCommand

case class obtainLinksFilter(From: Int, until : Int, degree : Int) extends ActorCommand

case class obtainLinksFilterPass(From: Int, until : Int, degree : Int) extends ActorCommand

//The config for a child computer to intialise on
case class LinkComputerConfig(modulo: Int, remainder: Int)


//Initialization command for an Actor
trait ActorInitCommand

//Case class which tells a linkCombinerActor to intialize another set of more linkCombiners
case class ActorCombinerSet(configs: List[ActorCombinerConfig]) extends ActorInitCommand
//Case class containing the configuration for a linkcombineractor
case class ActorCombinerConfig(system: Address, initCommand: ActorInitCommand)
//Case class which tells a linkCombiner to intialize a set of linkComputers
case class ActorComputationConfig(computers: List[LinkComputerConfig]) extends ActorInitCommand

//Ask for the amount of links per week, needed for histogram
case class userProjectsPerWeek()
//same as above, but skip the first actor. This is a bit of an ugly solution, but a quick fix for the presentation on friday
case class userProjectsPerWeekSkip()

trait linkResults

case class linkResult(map: GenMap[Link, Int])