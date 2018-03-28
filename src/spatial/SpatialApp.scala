package spatial

import argon.{DSLApp, _}
import argon.passes.IRPrinter
import nova.codegen.dot._
import poly.{ConstraintMatrix, ISL}
import spatial.data._
import spatial.lang.Void
import spatial.dse.DSEMode
import spatial.targets.{HardwareTarget, Targets}
import spatial.transform._
import spatial.traversal._
import spatial.internal.{spatialConfig => cfg}
import spatial.rewrites.SpatialRewriteRules

trait SpatialApp extends DSLApp {
  val target: HardwareTarget = null
  val desc: String = "Spatial compiler"
  val script: String = "spatial"
  private var overrideRetime = false

  private class SpatialISL extends ISL {
    override def domain[K](key: K): ConstraintMatrix[K] = key match {
      case s: Sym[_] => domainOf(s).asInstanceOf[ConstraintMatrix[K]]
      case _ => throw new Exception(s"Cannot get domain of $key")
    }
  }

  def main(): Void

  final def stage(args: Array[String]): Block[_] = stageBlock{ main() }

  override def initConfig(): Config = new SpatialConfig
  override def flows(): Unit = SpatialFlowRules(state)       // Register standard flow analysis rules
  override def rewrites(): Unit = SpatialRewriteRules(state) // Register initial rewrite rules

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
    lazy val retiming          = RetimingTransformer(state)

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
      (cfg.enableRetiming ? retiming) ==>
      //globalAllocation ==>
      //printer ==>
      //puDotCodegen ==>
      irDotCodegen

    isl.shutdown(100)
  }

  override def defineOpts(cli: CLIParser): Unit = {
    super.defineOpts(cli)

    overrideRetime = false

    cli.opt[String]("fpga").action{(x,_) => cfg.targetName = x }.text("Set name of FPGA target. [Default]")

    cli.note("")
    cli.note("Design Tuning:")
    cli.opt[Unit]("tune").action{(_,_) => cfg.dseMode = DSEMode.Bruteforce}.text("Enable default design tuning (bruteforce)")
    cli.opt[Unit]("bruteforce").action{(_,_) => cfg.dseMode = DSEMode.Bruteforce }.text("Enable brute force tuning.")
    cli.opt[Unit]("heuristic").action{(_,_) => cfg.dseMode = DSEMode.Heuristic }.text("Enable heuristic tuning.")
    cli.opt[Unit]("experiment").action{(_,_) => cfg.dseMode = DSEMode.Experiment }.text("Enable DSE experimental mode.").hidden()
    cli.opt[Int]("threads").action{(t,_) => cfg.threads = t }.text("Set number of threads to use in tuning.")

    cli.note("")
    cli.note("Backends:")

    cli.opt[Unit]("sim").action { (_,_) =>
      cfg.enableSim = true
      cfg.enableInterpret = false
      cfg.enableSynth = false
      if (!overrideRetime) cfg.enableRetiming = false
    }.text("Enable codegen to Scala (disables synthesis and retiming) [false]")

    cli.opt[Unit]("synth").action{ (_,_) =>
      cfg.enableSynth = true
      cfg.enableSim = false
      cfg.enableInterpret = false
      if (!overrideRetime) cfg.enableRetiming = true
    }.text("Enable codegen to Chisel/C++ (Synthesis) [true]")

    cli.opt[Unit]("dot").action( (_,_) =>
      cfg.enableDot = true
    ).text("Enable dot graph generation [false]")

    cli.opt[Unit]("retime").action{ (_,_) =>
      cfg.enableRetiming = true
      overrideRetime = true
    }.text("Force retiming for --sim")

    cli.opt[Unit]("noretime").action{ (_,_) =>
      cfg.enableRetiming = false
      overrideRetime = true
    }.text("Disable retiming")

    cli.note("")
    cli.note("Experimental:")

    cli.opt[Unit]("asyncMem").action{(_,_) => cfg.enableAsyncMem = true }.text("Enable asynchronous memories")

    cli.opt[Unit]("instrumentation").action { (_,_) => // Must necessarily turn on retiming
      cfg.enableInstrumentation = true
      cfg.enableRetiming = true
      overrideRetime = true
    }.text("Enable counters for each loop to assist in balancing pipelines")

    cli.opt[Unit]("instrument").action { (_,_) => // Must necessarily turn on retiming
      cfg.enableInstrumentation = true
      cfg.enableRetiming = true
      overrideRetime = true
    }.text("Enable counters for each loop to assist in balancing pipelines")

    cli.opt[Unit]("tightControl").action { (_,_) => // Must necessarily turn on retiming
      cfg.enableTightControl = true
      cfg.enableRetiming = true
      overrideRetime = true
    }.text("Enable tighter timing between controllers at the expense of potentially failing timing")

    cli.opt[Unit]("cheapFifos").action { (_,_) => // Must necessarily turn on retiming
      cfg.useCheapFifos = true
      cfg.enableRetiming = true
      overrideRetime = true
    }.text("Enable cheap fifos where accesses must be multiples of each other and not have lane-enables")

    cli.opt[Unit]("runtime").action{ (_,_) =>
      cfg.enableRuntimeModel = true
    }.text("Enable application runtime estimation")
  }

  override def settings(): Unit = {
    if (this.target eq null) {
      if (cfg.targetName eq null) {
        if (cfg.enableRetiming) {
          error("No target specified. Specify target using: --fpga <device> or")
          error("override val target = <device>")
          IR.logError()
        }
        else {
          warn("No target specified. Specify target using: --fpga <device> or")
          warn("override val target = <device>")
          warn("Defaulting to 'Default' device.")
          IR.logWarning()
        }
      }
      else {
        val target = Targets.targets.find{_.name.toLowerCase == cfg.targetName.toLowerCase }
        if (target.isDefined) {
          cfg.target = target.get
        }
        else {
          if (cfg.enableRetiming) {
            error(s"No target found with the name '${cfg.targetName}'.")
            error(s"Available targets: ")
            Targets.targets.foreach{t => error(s"  ${t.name}") }
            IR.logError()
          }
          else {
            warn(s"No target found with the name '${cfg.targetName}'.")
            warn("Defaulting to 'Default' device.")
            IR.logWarning()
          }
          cfg.target = Targets.Default
          cfg.targetName = cfg.target.name
        }
      }
    }
    else {
      // If the app defines one, use that one.
      if (cfg.targetName != null) {
        warn("Command line target was ignored as target is specified in application.")
        IR.logWarning()
      }
      cfg.target = this.target
      cfg.targetName = cfg.target.name
    }
    cfg.target.areaModel.init()
    cfg.target.latencyModel.init()
  }

}
