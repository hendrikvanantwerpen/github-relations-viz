package nl.tudelft.courses.in4355.github_relations_viz.util

import net.van_antwerpen.scala.collection.mapreduce.Aggregator._
import scala.collection.GenTraversable

trait ThresholdCountingSet[A] {
  def +(a: A): ThresholdCountingSet[A]
  def ++(as: GenTraversable[A]): ThresholdCountingSet[A]
  def included: Map[A,Int]
  def size: Int
}

private class AlwaysIncludedSet[A](override val included: Map[A,Int], override val size: Int) extends ThresholdCountingSet[A] {
  override def +(a: A) = new AlwaysIncludedSet(included |<| (a,1), size + 1)
  override def ++(as: GenTraversable[A]) = new AlwaysIncludedSet(included |<| as.map( (_,1) ), size + 1)
}

private class DefaultThresholdCountingSet[A](override val included: Map[A,Int], potential: Map[A,Int], threshold: Int, override val size: Int) extends ThresholdCountingSet[A] {

  override def +(a: A) = {
    val (newI,newP,cnt) = doTheWork(included,potential,size,a)
    new DefaultThresholdCountingSet(newI, newP, threshold, cnt)
  }

  override def ++(as: GenTraversable[A]) = {
    val (newI,newP,cnt) = ((included,potential,0) /: as ) ( (ip,a) => doTheWork(ip._1,ip._2,ip._3,a) )
    new DefaultThresholdCountingSet(newI, newP, threshold, cnt)
  }
  
  private def doTheWork(i:Map[A,Int], p:Map[A,Int], cnt:Int, a: A) = {
    if ( i contains (a) ) {
      (i |<| (a,1), p, cnt)
    } else {
      val count = (p get (a) getOrElse (0)) + 1
      if ( count >= threshold ) {
        (i |<| (a,count), p - a, cnt + 1)
      } else {
        (i, p |<| (a,1), cnt)
      }
    }
    
  }
  
}

object ThresholdCountingSet {
  def apply[A](threshold: Int): ThresholdCountingSet[A] =
    if ( threshold <= 1 ) {
      new AlwaysIncludedSet[A](Map.empty[A,Int],0)
    } else {
      new DefaultThresholdCountingSet[A](Map.empty[A,Int],Map.empty[A,Int],threshold,0)
    }
}