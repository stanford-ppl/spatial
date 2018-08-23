package spatial.transform

import argon._
import spatial.transform.unrolling._

case class UnrollingTransformer(IR: State) extends UnrollingBase
  with ForeachUnrolling
  with ReduceUnrolling
  with MemReduceUnrolling
  with MemoryUnrolling
  with SwitchUnrolling
