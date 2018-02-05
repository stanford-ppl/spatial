package pcc.traversal
package analysis

import pcc.core._
import pcc.data._
import pcc.lang._

class MemoryConfigurer[C[_]](mem: Mem[_,C], strategy: BankingStrategy)(implicit state: State) {
  protected val rank: Int = rankOf(mem)
  protected val isGlobal: Boolean = mem.isArgIn || mem.isArgOut

  def configure(): Unit = {
    dbg("\n\n--------------------------------")
    dbg(s"Inferring instances for memory $mem")
    dbg(s"${mem.ctx} " + mem.ctx.content.getOrElse(""))

  }

  //def bank(readers: Seq[Sym[_]], writers: Seq[Sym[_]]): Seq[Instance] = {
  //
  //}



}
