package nl.tudelft.courses.in4355.github_relations_viz.mapreduce

import scala.collection.immutable.{TreeMap,SortedMap,SortedSet,TreeSet}
import Monoids._
import Multoids._
import MapReduce._
import scala.math.Ordering

object MultoidsTest extends App {
  
  val words = List("aap","anders","bijna","boer","noot","mies","meisje","zaag","zonder","fiets","zonder","aap","mies","aap")
  
  def wordCount(s: String) = (s,1)
  val wordMap = words.mapReduce[(String,Int),Map[String,Int]](wordCount)
  println(wordMap)

  def wordPlusLength(s: String) = (s.size,s)
  val wordPlusLengths = words.mapReduce[(Int,String),SortedMap[Int,String]](wordPlusLength)
  println(wordPlusLengths)

  def wordLength(s: String) = s.size
  val totalLength = words.mapReduce[Int,Int](wordLength)
  println(totalLength)
  
  def countAndConcat(s: String) = (1,s)
  implicit val tupleIntStringMultoid = MonoidMultoid[(Int,String)]
  val countedAndTogether = words.mapReduce[(Int,String),(Int,String)](wordInstance)
  println(countedAndTogether)

  def wordInstance(s: String) = (1,s)
  val totalWords = words.mapReduce[(Int,String),(Int,Set[String])](wordInstance)
  println(totalWords)

  def word(s: String) = s
  val wordSet = words.mapReduce[String,SortedSet[String]](word)
  println(wordSet)

  def nestedWordCount(s: String) = (s.head.toString,(s,1))
  val nestedWordMap = words.mapReduce[(String,(String,Int)),SortedMap[String,Map[String,Int]]](nestedWordCount)
  println(nestedWordMap)
  
  def nestedWordLength(s: String) = (s.size,(s,1))
  val nestedWordLengths = words.mapReduce[(Int,(String,Int)),SortedMap[Int,Map[String,Int]]](nestedWordLength)
  println(nestedWordLengths)

  def goCrazy(s: String) = (s.size,(s,("count",1)))
  val crazy = words.mapReduce[(Int,(String,(String,Int))),SortedMap[Int,Map[String,Map[String,Int]]]](goCrazy)
  println(crazy)
  
}
