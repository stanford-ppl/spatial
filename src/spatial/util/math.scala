package spatial.util

import argon._
import forge.tags._

import spatial.lang._
import spatial.node._
import emul.ResidualGenerator._
import spatial.metadata.bounds._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.metadata.rewrites._
import spatial.metadata.retiming._
import spatial.metadata.math._

import argon.node._

import emul.FixedPoint
import forge.tags.stateful

import utils.math.{isPow2,log2,gcd}

object math {
  /** Convert mod of pow2 into FixAnd */
  @stateful def selectMod[S,I,F](x: FixPt[S,I,F], y: Int): FixPt[S,I,F] = {
    implicit val S: BOOL[S] = x.fmt.s
    implicit val I: INT[I] = x.fmt.i
    implicit val F: INT[F] = x.fmt.f
    if (log2(y.toDouble) == 0) x.from(0)
    // TODO: Consider making a node like case class BitRemap(data: Bits[_], remap: Seq[Int], outType: Bits[T]) that wouldn't add to retime latency
    else x & (scala.math.pow(2,log2(y.toDouble))-1).to[Fix[S,I,F]] 
  }

  /** Convert mod to a constant value */
  @stateful def constMod[S,I,F](x: FixPt[S,I,F], y: Seq[Int]): FixPt[S,I,F] = {
    implicit val S: BOOL[S] = x.fmt.s
    implicit val I: INT[I] = x.fmt.i
    implicit val F: INT[F] = x.fmt.f
    if (y.size==1) {
      x.from(y.head)
    } else {
      val b = boundVar[FixPt[S,I,F]]
      b.vecConst = y
      b
    }
  }

  @stateful def staticMod(lin: scala.Int, iter: Num[_], y: scala.Int): Boolean = {
    if (iter.counter.ctr.isStaticStartAndStep) {
      val Final(step) = iter.counter.ctr.step
      val par = iter.counter.ctr.ctrPar.toInt
      val g = gcd(par*step*lin,y)
      if (g == 1) false // Residue set of lin % y is {0,1,...,y-1}
      else if (g % y == 0) true // Residue set of lin % y is {0}
      else false // Residue set of lin % y is {0, g, 2g, ..., y-g}
    }
    else false
  }

  @stateful def residual(lin: scala.Int, iter: Num[_], ofs: scala.Int, y: scala.Int): ResidualGenerator = {
    if (iter.getCounter.isDefined && iter.counter.ctr.isStaticStartAndStep) {
      val Final(start) = iter.counter.ctr.start
      val Final(step) = iter.counter.ctr.step
      val par = iter.counter.ctr.ctrPar.toInt
      val lanes = iter.counter.lanes
      if (y != 0) {
        val A = gcd(par * step * lin, y)
        val B = lanes.map { lane => (((start + ofs + lane * step * lin) % y) + y) % y }
        dbgs(s"Residual Generator for lane $lanes with step $step, lin $lin and start $start + $ofs under mod $y = $A, $B")
        ResidualGenerator(A, B, y)
      } else {
        val A = par * step * lin
        val B = lanes.map { lane => start + ofs + lane * step * lin }
        ResidualGenerator(A, B, y)
      }
    }
    else ResidualGenerator(1, 0, y)
  }

  @stateful def getPosMod(lin: scala.Int, x: Num[_], ofs: scala.Int, mod: scala.Int): Seq[scala.Int] = {
    val res = residual(lin, x, ofs, mod)
    res.resolvesTo.get
  }

}
