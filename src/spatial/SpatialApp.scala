package spatial

import core._
import core.passes.IRPrinter
import nova.compiler.DSLApp
import spatial.traversal._
import nova.codegen.dot._
import poly.{ConstraintMatrix, ISL}

import spatial.data._
import spatial.transform._
import spatial.traversal.SanityChecks

trait SpatialApp extends DSLApp {
  class SpatialISL extends ISL {
    override def domain[K](key: K): ConstraintMatrix[K] = key match {
      case s: Sym[_] => domainOf(s).asInstanceOf[ConstraintMatrix[K]]
      case _ => throw new Exception(s"Cannot get domain of $key")
    }
  }

  override def initConfig(): Config = new SpatialConfig

  def runPasses[R](block: Block[R]): Unit = {
    implicit val isl: ISL = new SpatialISL
    isl.startup()

    // --- Debug
    lazy val printer           = IRPrinter(state)

    // --- Checking
    lazy val sanityChecks      = SanityChecks(state)

    // --- Analysis
    lazy val accessAnalyzer    = AccessAnalyzer(state)
    lazy val memoryAnalyzer    = MemoryAnalyzer(state)

    // --- Transformer
    lazy val friendlyTransformer = FriendlyTransformer(state)
    lazy val switchTransformer = SwitchTransformer(state)
    lazy val switchOptimizer   = SwitchOptimizer(state)
    lazy val pipeInserter      = PipeInserter(state)
    lazy val unrollTransformer = UnrollingTransformer(state)

    lazy val globalAllocation = GlobalAllocation(state)

    // --- Codegen
    lazy val irDotCodegen = IRDotCodegen(state)
    lazy val puDotCodegen = PUDotCodegen(state)

    block ==>
      printer ==>
      friendlyTransformer ==>
      sanityChecks ==>
      switchTransformer ==>
      switchOptimizer ==>
      pipeInserter ==>
      printer ==>
      accessAnalyzer ==>
      printer ==>
      memoryAnalyzer ==>
      printer ==>
      unrollTransformer ==>
      printer ==>
      //globalAllocation ==>
      //printer ==>
      //puDotCodegen ==>
      irDotCodegen

    isl.shutdown(100)
  }

}
