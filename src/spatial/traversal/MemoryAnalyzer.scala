package spatial.traversal

import argon._
import argon.passes.Pass
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
    case m:LUT[_,_]     => new MemoryConfigurer(m, strategy)
    case m:FIFO[_]      => new FIFOConfigurer(m, strategy)  // No buffering
    case m:LIFO[_]      => new FIFOConfigurer(m, strategy)  // No buffering
    case m:Reg[_]       => new MemoryConfigurer(m, strategy)
    case m:StreamIn[_]  => new MemoryConfigurer(m, strategy)
    case m:StreamOut[_] => new MemoryConfigurer(m, strategy)
    case _ => throw new Exception(s"Don't know how to bank memory of type ${mem.tp}")
  }).asInstanceOf[MemoryConfigurer[C]]

  def run(): Unit = {
    val memories = localMems.all
    memories.foreach{m => configurer(m).configure() }
  }
}
