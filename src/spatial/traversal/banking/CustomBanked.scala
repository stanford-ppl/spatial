package spatial.traversal.banking

import argon._
import poly.ISL
import spatial.metadata.access._
import spatial.metadata.memory._

/** Dummy class for memories that are custom banked (i.e. LockSRAM), meaning the user or the backend (i.e. Plasticine)
  * inherently promises correctness
  */
case class CustomBanked()(implicit IR: State, isl: ISL) extends BankingStrategy {

  override def bankAccesses(
    mem:    Sym[_],
    rank:   Int,
    reads:  Set[Set[AccessMatrix]],
    writes: Set[Set[AccessMatrix]],
    attemptDirectives: Seq[BankingOptions],
    depth: Int
  ): Map[BankingOptions, Map[Set[Set[AccessMatrix]], Seq[Banking]]] = {
    Map(attemptDirectives.head -> Map(reads ++ writes -> Seq(UnspecifiedBanking(Seq.tabulate(rank){i => i}))))
  }

}
