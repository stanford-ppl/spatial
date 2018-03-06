package spatial.node

import core._
import forge.tags._

import spatial.lang._

@op case class DRAMNew[A:Bits,C[T]](
    dims: Seq[I32]
    )(implicit tp: Type[C[A]])
  extends MemAlloc[C[A]]

@op case class GetDRAMAddress[A:Bits,C[T]](dram: DRAM[A,C]) extends Primitive[I64]

