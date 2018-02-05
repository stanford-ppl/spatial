package pcc.data

import forge._
import pcc.core._
import pcc.lang.LocalMem
import pcc.util.IntLike._

/**
  * Abstract class for any banking strategy
  */
sealed abstract class Banking {
  def nBanks: Int
  def stride: Int
  def dims: Seq[Int]
  @api def bankSelect[I:IntLike](addr: Seq[I]): I
}

/**
  * Banking address function (alpha*A / B) mod N
  */
case class ModBanking(N: Int, B: Int, alpha: Seq[Int], dims: Seq[Int]) extends Banking {
  override def nBanks: Int = N
  override def stride: Int = B

  @api def bankSelect[I:IntLike](addr: Seq[I]): I = (sum(alpha.zip(addr).map{case (a,i) => i*a }) / B) % N
}
object ModBanking {
  def Unit(rank: Int) = ModBanking(1, 1, Seq.fill(rank)(1), Seq.fill(rank)(1))
}

/**
  * Used during memory analysis to track intermediate results
  */
case class Instance(
  var reads:    Seq[Set[AccessMatrix]], // All reads within this group
  var writes:   Seq[Set[AccessMatrix]], // All writes within this group
  var ctrls:    Set[Ctrl],              // Set of controllers these accesses are in
  var metapipe: Option[Ctrl],           // Controller if at least some accesses require n-buffering
  var banking:  Seq[Banking],           // Banking information
  var depth:    Int,                    // Depth of n-buffer
  var cost:     Int,                    // Cost estimate of this configuration
  var ports:    Map[Sym[_],Int]         // Ports
) {
  // This is an accumulating group if there exists an accumulating write and a read in the same controller
  @stateful def isAcc: Boolean = {
    writes.iterator.flatten.exists{wr =>
      isAccum(wr.access) && reads.iterator.flatten.exists{rd =>
        parentOf(rd.access) == parentOf(wr.access)
      }
    }
  }
}


/**
  * Abbreviated version of MemoryInstance for use outside memory analysis
  */
case class Memory(
  banking: Seq[Banking],  // Banking information
  depth:   Int,           // Buffer depth
  isAccum: Boolean        // Flags whether this instance is an accumulator
) {
  def nBanks: Seq[Int] = banking.map(_.nBanks)
  def totalBanks: Int = banking.map(_.nBanks).product

  @api def bankSelects[T:IntLike](addr: Seq[T]): Seq[T] = banking.map(_.bankSelect(addr))

  @api def bankOffset[T:IntLike,C[_]](mem: LocalMem[_,C], addr: Seq[T]): T = {
    val w = dimsOf(mem).map(_.toInt)
    val D = rankOf(mem)
    val n = banking.map(_.nBanks).product
    if (banking.lengthIs(1)) {
      val b = banking.head.stride

      sum((0 until D).map{t =>
        val xt = addr(t)
        if (t < D - 1) { xt * (w.slice(t+1,D-1).product * math.ceil(w(D-1).toDouble / (n*b)).toInt * b) }
        else           { (xt / (n*b)) * b + xt % b }
      })
    }
    else if (banking.lengthIs(D)) {
      val b = banking.map(_.stride)
      val n = banking.map(_.nBanks)
      val dims = (0 until D).map{t => (t+1 until D).map{k => math.ceil(w(k)/n(k)).toInt }.product }

      sum((0 until D).map{t =>
        val xt = addr(t)
        ( ( xt/(b(t)*n(t)) )*b(t) + xt%b(t) ) * dims(t)
      })
    }
    else {
      // TODO: Bank address for mixed dimension groups
      throw new Exception("Bank address calculation for arbitrary dim groupings unknown")
    }
  }
}

