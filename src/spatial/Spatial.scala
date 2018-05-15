package spatial

import argon._
import argon.passes.IRPrinter
import pir.codegen.dot._
import poly.{ConstraintMatrix, ISL}
import spatial.codegen.scalagen._
import spatial.codegen.cppgen._
import spatial.codegen.chiselgen._
import spatial.data._
import spatial.lang.{Tensor1, Text, Void}
import spatial.dse.DSEMode
import spatial.flows.SpatialFlowRules
import spatial.targets.{HardwareTarget, Targets}
import spatial.transform._
import spatial.traversal._
import spatial.internal.{spatialConfig => cfg}
import spatial.node.InputArguments
import spatial.report._
import spatial.rewrites.SpatialRewriteRules


trait Spatial extends Compiler {

  val target: HardwareTarget = null   // Optionally overridden by the application
  final val desc: String = "Spatial compiler"
  final val script: String = "spatial"
  private var overrideRetime = false

  private class SpatialISL extends ISL {
    override def domain[K](key: K): ConstraintMatrix[K] = key match {
      case s: Sym[_] => s.domain.asInstanceOf[ConstraintMatrix[K]]
      case _ => throw new Exception(s"Cannot get domain of $key")
    }
  }

  var args: Tensor1[Text] = _
  def entry(args: Tensor1[Text]): Void

  final def stageApp(sargs: Array[String]): Block[_] = stageBlock{
    args = stage(InputArguments())
    entry(args)
  }

  override def initConfig(): Config = new SpatialConfig
  override def flows(): Unit = SpatialFlowRules(state)       // Register standard flow analysis rules
  override def rewrites(): Unit = SpatialRewriteRules(state) // Register initial rewrite rules

  def runPasses[R](block: Block[R]): Block[R] = {
    implicit val isl: ISL = new SpatialISL
    isl.startup()

    // --- Debug
    lazy val printer = IRPrinter(state)

    // --- Checking
    lazy val sanityChecks = SanityChecks(state)

    // --- Analysis
    lazy val cliNaming          = CLINaming(state)
    lazy val useAnalyzer        = UseAnalyzer(state)
    lazy val accessAnalyzer     = AccessAnalyzer(state)
    lazy val memoryAnalyzer     = MemoryAnalyzer(state)
    lazy val memoryAllocator    = MemoryAllocator(state)
    lazy val initiationAnalyzer = InitiationAnalyzer(state)

    // --- Reports
    lazy val memoryReporter = MemoryReporter(state)
    lazy val retimeReporter = RetimeReporter(state)

    // --- Transformer
    lazy val friendlyTransformer = FriendlyTransformer(state)
    lazy val switchTransformer = SwitchTransformer(state)
    lazy val switchOptimizer   = SwitchOptimizer(state)
    lazy val blackboxLowering  = BlackboxLowering(state)
    lazy val memoryDealiasing  = MemoryDealiasing(state)
    lazy val pipeInserter      = PipeInserter(state)
    lazy val registerCleanup   = RegisterCleanup(state)
    lazy val unrollTransformer = UnrollingTransformer(state)
    lazy val retiming          = RetimingTransformer(state)

    lazy val globalAllocation = GlobalAllocation(state)

    // --- Codegen
    lazy val chiselCodegen = ChiselGen(state)
    lazy val cppCodegen = CppGen(state)
    lazy val irDotCodegen = IRDotCodegen(state)
    // lazy val treeCodegen = TreeCodegen(state)
    lazy val scalaCodegen = ScalaGenSpatial(state)
    lazy val puDotCodegen = PUDotCodegen(state)

    val result = {
      block ==> printer ==>
        cliNaming ==>
        friendlyTransformer ==>
        sanityChecks ==>
        switchTransformer ==>
        switchOptimizer ==> printer ==>
        blackboxLowering ==> printer ==>
        memoryDealiasing ==> printer ==>
        pipeInserter ==> printer ==>
        useAnalyzer ==> printer ==>
        registerCleanup ==> printer ==>
        accessAnalyzer ==> printer ==>
        memoryAnalyzer ==> printer ==>
        memoryAllocator ==>
        memoryReporter  ==>
        unrollTransformer ==> printer ==>
        (cfg.enableRetiming ? retiming) ==> printer ==>
        (cfg.enableRetiming ? retimeReporter) ==>
        initiationAnalyzer ==>
        printer ==>
        (cfg.enableSim ? scalaCodegen) ==>
        // (cfg.enableTree ? irTreeCodegen) ==>
        (cfg.enableSynth ? chiselCodegen) ==>
        (cfg.enableSynth ? cppCodegen) ==>
        (cfg.enableDot ? irDotCodegen)
    }

    isl.shutdown(100)
    result
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

    cli.opt[Int]("compressWires").action{(t,_) => cfg.compressWires = t }.text("Enable string compression on chisel wires to shrink JVM bytecode")

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
    // Spatial allows mutable aliases for DRAM, SRAM, etc.
    config.enableMutableAliases = true

    if (this.target eq null) {
      if (cfg.targetName eq null) {
        if (!cfg.enableRetiming) {
          warn("No target specified. Specify target using: --fpga <device> or")
          warn("override val target = <device>")
          warn("Defaulting to 'Default' device.")
          IR.logWarning()
          cfg.target = Targets.Default
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
            return
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
    if (cfg.target eq null) {
      error("No target specified. Specify target using: --fpga <device> or")
      error("override val target = <device>")
      IR.logError()
    }
    else {
      cfg.target.areaModel.init()
      cfg.target.latencyModel.init()
    }
  }

}
