package spatial.traversal

import core._
import core.passes.Pass
import poly.ISL

import spatial.traversal.banking._
import spatial.data._
import spatial.lang._

case class MemoryAnalyzer(IR: State)(implicit isl: ISL) extends Pass {
  private val strategy: BankingStrategy = ExhaustiveBanking()

  override protected def process[R](block: Block[R]): Block[R] = {
    run()
    enWarn = Some(false)  // Disable warnings after the first run
    block
  }

  protected def configurer[C[_]](mem: Sym[_]): MemoryConfigurer[C] = (mem match {
    case m:SRAM[_,_]    => new MemoryConfigurer(m, strategy)
    case m:RegFile[_,_] => new MemoryConfigurer(m, strategy)
    case m:FIFO[_]      => new MemoryConfigurer(m, strategy)
    case m:LIFO[_]      => new MemoryConfigurer(m, strategy)
    case m:Reg[_]       => new MemoryConfigurer(m, strategy)
    case m:ArgIn[_]     => new MemoryConfigurer(m, strategy)
    case m:ArgOut[_]    => new MemoryConfigurer(m, strategy)
    case _ => throw new Exception(s"Don't know how to bank memory of type ${mem.tp}")
  }).asInstanceOf[MemoryConfigurer[C]]

  def run(): Unit = {
    // Run
    val memories = localMems.all
    memories.foreach{m => configurer(m).configure() }
  }
}
