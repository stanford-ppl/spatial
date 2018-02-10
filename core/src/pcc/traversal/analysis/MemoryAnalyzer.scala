package pcc.traversal
package analysis

import pcc.core._
import pcc.data._
import pcc.lang._
import pcc.poly.ISL
import banking._

case class MemoryAnalyzer(IR: State)(implicit isl: ISL) extends Pass {
  override val name = "Memory Analyzer"
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
    case _ => throw new Exception(s"Don't know how to bank memory of type ${mem.typeName}")
  }).asInstanceOf[MemoryConfigurer[C]]

  def run(): Unit = {
    // Reset metadata
    metadata.clear[Duplicates]
    metadata.clear[Dispatch]
    metadata.clear[Ports]
    // Run
    val memories = localMems.all
    memories.foreach{m => configurer(m).configure() }
  }
}
