package nova.traversal
package analysis

import core._
import core.passes.Pass
import nova.data._
import spatial.lang._
import nova.traversal.analysis.banking._
import nova.poly.ISL

case class MemoryAnalyzer(IR: State)(implicit isl: ISL) extends Pass {
  private val strategy: BankingStrategy = ExhaustiveBanking()

  override protected def process[R](block: Block[R]): Block[R] = {
    run()
    enWarn = Some(false)  // Disable warnings after the first run
    block
  }

  protected def configurer[C[_]](mem: Sym[_]): MemoryConfigurer[C] = (mem match {
    case m:SRAM[_] => new MemoryConfigurer(m, strategy)
    case m:FIFO[_] => new MemoryConfigurer(m, strategy)
    case m:LIFO[_] => new MemoryConfigurer(m, strategy)
    case m:Reg[_]  => new MemoryConfigurer(m, strategy)
    case _ => throw new Exception(s"Don't know how to bank memory of type ${mem.tp}")
  }).asInstanceOf[MemoryConfigurer[C]]

  def run(): Unit = {
    // Run
    val memories = localMems.all
    memories.foreach{m => configurer(m).configure() }
  }
}
