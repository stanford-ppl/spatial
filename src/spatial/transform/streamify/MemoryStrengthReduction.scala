package spatial.transform.streamify

import argon.transform.ForwardTransformer

import argon._
import spatial.lang._
import spatial.node._
case class MemoryStrengthReduction(IR: State) extends ForwardTransformer {
  // Can we reduce the strength of a memory?
  // If it's an indexable memory: Does there ei
}