package spatial.traversal.banking

import argon._
import spatial.data._

abstract class BankingStrategy {

  def bankAccesses(
    mem:    Sym[_],                        // Memory to be banked
    rank:   Int,                           // Rank of memory to be banked
    reads:  Set[Set[AccessMatrix]],        // Reads to this banked memory
    writes: Set[Set[AccessMatrix]],        // Writes to this banked memory
    dimGrps: Seq[Seq[Seq[Int]]]            // Sequence of dimension groupings
  ): Seq[Seq[Banking]]                     // Sequence of possible multidimensional bankings

}
