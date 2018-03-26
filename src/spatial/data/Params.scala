package spatial.data

import argon._
import spatial.lang._

/**
  * Parameter Range
  * Tracks minimum, step, and maximum for a given design Param
  */
case class ParamRange(min: Int, step: Int, max: Int) extends StableData[ParamRange]
object rangeOf {
  def get(x: I32): Option[(Int,Int,Int)] = metadata[ParamRange](x).map{d => (d.min,d.step,d.max) }
  def apply(x: I32): (Int,Int,Int) = metadata[ParamRange](x).map{d => (d.min,d.step,d.max) }.getOrElse((1,1,1))
  def update(x: I32, rng: (Int,Int,Int)): Unit = metadata.add(x, ParamRange(rng._1,rng._2,rng._3))
}
