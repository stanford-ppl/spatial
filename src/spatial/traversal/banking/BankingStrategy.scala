package spatial.traversal.banking

import argon._
import spatial.metadata.access._
import spatial.metadata.memory._

abstract class BankingStrategy {
  // A complete banking scheme for a memory
  type FullBanking = Seq[Banking]
  // A collection of valid banking schemes which may only be applied to one dimension if hierarchical
  type PartialBankingChoices = Seq[Banking]
  // A collection of potential FullBanking schemes that are valid, i.e. a Seq where each element is for different dimensions of the mem
  type FullBankingChoices = Seq[FullBanking]
  // A collection of AccessMatrix representing a "group" of accesses
  type SingleAccessGroup = Set[AccessMatrix]
  // A collection of AccessGroups, for tracking which banking schemes satisfy which collections of AccessGroups
  type AccessGroups = Set[SingleAccessGroup]

  def bankAccesses(
    mem:    Sym[_],                        // Memory to be banked
    rank:   Int,                           // Rank of memory to be banked
    reads:  Set[Set[AccessMatrix]],        // Reads to this banked memory
    writes: Set[Set[AccessMatrix]],        // Writes to this banked memory
    attemptDirectives: Seq[BankingOptions], // Enumeration of which banking directives to solve for
    depth: Int                             // Depth of n-buffered mem

  ): Map[BankingOptions, Map[AccessGroups, FullBankingChoices]] // Mapping of a set of banking directives to a mapping of squence of possible multidimensional bankings to the reads associated with it

}
