package spatial.node

import forge.tags._
import core._
import spatial.lang._

/** DRAM **/
@op case class DRAMNew[A:Bits](dims: Seq[I32]) extends MemAlloc[DRAM[A]]

@op case class GetDRAMAddress[A:Bits](dram: DRAM[A]) extends Primitive[I64]

