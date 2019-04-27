package spatial.traversal.banking

import argon._
import spatial.metadata.access._
import spatial.metadata.memory._

abstract class BankingStrategy {

  def bankAccesses(
    mem:    Sym[_],                        // Memory to be banked
    rank:   Int,                           // Rank of memory to be banked
    reads:  Set[Set[AccessMatrix]],        // Reads to this banked memory
    writes: Set[Set[AccessMatrix]],        // Writes to this banked memory
    attemptDirectives: Seq[BankingOptions], // Enumeration of which banking directives to solve for
    depth: Int                             // Depth of n-buffered mem

  ): Map[BankingOptions, Map[Set[Set[AccessMatrix]], Seq[Banking]]]  // Mapping of a set of banking directives to a mapping of squence of possible multidimensional bankings to the reads associated with it

}
