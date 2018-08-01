package spatial

import argon._
import argon.passes.IRPrinter
import pir.codegen.dot._
import poly.{ConstraintMatrix, ISL}
import spatial.codegen.chiselgen._
import spatial.codegen.cppgen._
import spatial.codegen.scalagen._
import spatial.codegen.treegen._

import spatial.lang.{Tensor1, Text, Void}
import spatial.node.InputArguments
import spatial.metadata.access._
import spatial.targets.HardwareTarget

import spatial.dse._
import spatial.transform._
import spatial.traversal._
import spatial.report._
import spatial.flows.SpatialFlowRules
import spatial.rewrites.SpatialRewriteRules

import spatial.util.spatialConfig


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
  def main(args: Tensor1[Text]): Void

  final def stageApp(sargs: Array[String]): Block[_] = stageBlock{
    args = stage(InputArguments())
    main(args)
  }

  override def initConfig(): Config = new SpatialConfig
  override def flows(): Unit = SpatialFlowRules(state)       // Register standard flow analysis rules
  override def rewrites(): Unit = SpatialRewriteRules(state) // Register initial rewrite rules

  def runPasses[R](block: Block[R]): Block[R] = {
    implicit val isl: ISL = new SpatialISL
    isl.startup()

    // --- Debug
    lazy val printer = IRPrinter(state, enable = config.enDbg)
    lazy val finalIRPrinter = IRPrinter(state, enable = true)

    // --- Checking
    lazy val userSanityChecks  = UserSanityChecks(state)
    lazy val transformerChecks = CompilerSanityChecks(state, enable = spatialConfig.enLog)
    lazy val finalSanityChecks = CompilerSanityChecks(state, enable = true)

    // --- Analysis
    lazy val cliNaming          = CLINaming(state)
    lazy val useAnalyzer        = UseAnalyzer(state)
    lazy val accessAnalyzer     = AccessAnalyzer(state)
    lazy val memoryAnalyzer     = MemoryAnalyzer(state)
    lazy val memoryAllocator    = MemoryAllocator(state)
    lazy val rewriteAnalyzer    = RewriteAnalyzer(state)
    lazy val initiationAnalyzer = InitiationAnalyzer(state)
    lazy val accumAnalyzer      = AccumAnalyzer(state)

    // --- Reports
    lazy val memoryReporter = MemoryReporter(state)
    lazy val retimeReporter = RetimeReporter(state)

    // --- Transformer
    lazy val friendlyTransformer   = FriendlyTransformer(state)
    lazy val switchTransformer     = SwitchTransformer(state)
    lazy val switchOptimizer       = SwitchOptimizer(state)
    lazy val blackboxLowering      = BlackboxLowering(state)
    lazy val memoryDealiasing      = MemoryDealiasing(state)
    lazy val pipeInserter          = PipeInserter(state)
    lazy val transientCleanup      = TransientCleanup(state)
    lazy val unrollTransformer     = UnrollingTransformer(state)
    lazy val rewriteTransformer    = RewriteTransformer(state)
    lazy val flatteningTransformer = FlatteningTransformer(state)
    lazy val retiming              = RetimingTransformer(state)
    lazy val accumTransformer      = AccumTransformer(state)

    // --- Codegen
    lazy val chiselCodegen = ChiselGen(state)
    lazy val cppCodegen    = CppGen(state)
    lazy val irDotCodegen  = IRDotCodegen(state)
    lazy val treeCodegen   = TreeGen(state)
    lazy val scalaCodegen  = ScalaGenSpatial(state)

    val result = {
      block ==> printer     ==>
        cliNaming           ==>
        friendlyTransformer ==> printer ==> transformerChecks ==>
        userSanityChecks    ==>
        /** Black box lowering */
        switchTransformer   ==> printer ==> transformerChecks ==>
        switchOptimizer     ==> printer ==> transformerChecks ==>
        blackboxLowering    ==> printer ==> transformerChecks ==>
        switchTransformer   ==> printer ==> transformerChecks ==>
        switchOptimizer     ==> printer ==> transformerChecks ==>
        memoryDealiasing    ==> printer ==> transformerChecks ==>
        /** Control insertion */
        pipeInserter        ==> printer ==> transformerChecks ==>
        /** Dead code cleanup */
        useAnalyzer         ==>
        transientCleanup    ==> printer ==> transformerChecks ==>
        /** Memory analysis */
        accessAnalyzer      ==>
        memoryAnalyzer      ==>
        memoryAllocator     ==> printer ==>
        /** Unrolling */
        unrollTransformer   ==> printer ==> transformerChecks ==>
        useAnalyzer         ==>
        transientCleanup    ==> printer ==> transformerChecks ==>
        /** Hardware Rewrites **/
        rewriteAnalyzer     ==>
        rewriteTransformer  ==> printer ==> transformerChecks ==>
        /** Pipe Flattening */
        flatteningTransformer ==> printer ==> transformerChecks ==>
        /** Accumulation Specialization **/
        (spatialConfig.enableOptimizedReduce ? accumAnalyzer) ==> printer ==>
        (spatialConfig.enableOptimizedReduce ? accumTransformer) ==> printer ==> transformerChecks ==>
        /** Retiming */
        retiming            ==> printer ==> transformerChecks ==>
        retimeReporter      ==>
        /** Schedule finalization */
        initiationAnalyzer  ==>
        /** Reports */
        memoryReporter      ==>
        finalIRPrinter      ==>
        finalSanityChecks   ==>
        /** Code generation */
        treeCodegen         ==>
        (spatialConfig.enableSim   ? scalaCodegen)  ==>
        (spatialConfig.enableSynth ? chiselCodegen) ==>
        (spatialConfig.enableSynth ? cppCodegen)    ==>
        (spatialConfig.enableDot ? irDotCodegen)
    }

    isl.shutdown(100)
    result
  }

  override def defineOpts(cli: CLIParser): Unit = {
    super.defineOpts(cli)

    overrideRetime = false

    cli.opt[String]("fpga").action{(x,_) => spatialConfig.targetName = x }.text("Set name of FPGA target. [Default]")

    cli.note("")
    cli.note("Design Tuning:")
    cli.opt[Unit]("tune").action{(_,_) => spatialConfig.dseMode = DSEMode.Bruteforce}.text("Enable default design tuning (bruteforce)")
    cli.opt[Unit]("bruteforce").action{(_,_) => spatialConfig.dseMode = DSEMode.Bruteforce }.text("Enable brute force tuning.")
    cli.opt[Unit]("heuristic").action{(_,_) => spatialConfig.dseMode = DSEMode.Heuristic }.text("Enable heuristic tuning.")
    cli.opt[Unit]("experiment").action{(_,_) => spatialConfig.dseMode = DSEMode.Experiment }.text("Enable DSE experimental mode.").hidden()
    cli.opt[Int]("threads").action{(t,_) => spatialConfig.threads = t }.text("Set number of threads to use in tuning.")

    cli.note("")
    cli.note("Backends:")

    cli.opt[Unit]("sim").action { (_,_) =>
      spatialConfig.enableSim = true
      spatialConfig.enableInterpret = false
      spatialConfig.enableSynth = false
      if (!overrideRetime) spatialConfig.enableRetiming = false
    }.text("Enable codegen to Scala (disables synthesis and retiming) [false]")

    cli.opt[Unit]("synth").action{ (_,_) =>
      spatialConfig.enableSynth = true
      spatialConfig.enableSim = false
      spatialConfig.enableInterpret = false
      if (!overrideRetime) spatialConfig.enableRetiming = true
    }.text("Enable codegen to Chisel/C++ (Synthesis) [true]")

    cli.opt[Unit]("dot").action( (_,_) =>
      spatialConfig.enableDot = true
    ).text("Enable dot graph generation [false]")

    cli.opt[Unit]("retime").action{ (_,_) =>
      spatialConfig.enableRetiming = true
      overrideRetime = true
    }.text("Force retiming for --sim")

    cli.opt[Unit]("noretime").action{ (_,_) =>
      spatialConfig.enableRetiming = false
      spatialConfig.enableOptimizedReduce = false
      overrideRetime = true
    }.text("Disable retiming")

    cli.note("")
    cli.note("Experimental:")

    cli.opt[Unit]("broadcast").action{(_,_) => spatialConfig.enableBroadcast = true }.text("Enable broadcast reads")

    cli.opt[Unit]("asyncMem").action{(_,_) => spatialConfig.enableAsyncMem = true }.text("Enable asynchronous memories")

    cli.opt[Int]("compressWires").action{(t,_) => spatialConfig.compressWires = t }.text("Enable string compression on chisel wires to shrink JVM bytecode")

    cli.opt[Unit]("instrumentation").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.enableInstrumentation = true
      spatialConfig.enableRetiming = true
      overrideRetime = true
    }.text("Enable counters for each loop to assist in balancing pipelines")

    cli.opt[Unit]("instrument").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.enableInstrumentation = true
      spatialConfig.enableRetiming = true
      overrideRetime = true
    }.text("Enable counters for each loop to assist in balancing pipelines")

    cli.opt[Unit]("tightControl").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.enableTightControl = true
      spatialConfig.enableRetiming = true
      overrideRetime = true
    }.text("Enable tighter timing between controllers at the expense of potentially failing timing")

    cli.opt[Unit]("debugResources").action { (_,_) => 
      spatialConfig.enableDebugResources = true
    }.text("Copy chisel + fringe templates with DirDep and do not use the published jars for templates")

    cli.opt[Unit]("cheapFifos").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.useCheapFifos = true
      spatialConfig.enableRetiming = true
      overrideRetime = true
    }.text("Enable cheap fifos where accesses must be multiples of each other and not have lane-enables")

    cli.opt[Unit]("optimizeReduce").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.enableOptimizedReduce = true
      spatialConfig.enableRetiming = true
      overrideRetime = true
    }.text("Squeeze II of reductions to 1 where possible, and instantiate specialized reduce node")

    cli.opt[Unit]("runtime").action{ (_,_) =>
      spatialConfig.enableRuntimeModel = true
    }.text("Enable application runtime estimation")
  }

  override def settings(): Unit = {
    // Spatial allows mutable aliases for DRAM, SRAM, etc.
    config.enableMutableAliases = true

    if (this.target eq null) {
      if (spatialConfig.targetName eq null) {
        if (!spatialConfig.enableRetiming) {
          warn("No target specified. Specify target using: --fpga <device> or")
          warn("override val target = <device>")
          warn("Defaulting to 'Default' device.")
          IR.logWarning()
          spatialConfig.target = targets.Default
        }
      }
      else {
        val target = targets.fpgas.find{_.name.toLowerCase == spatialConfig.targetName.toLowerCase }
        if (target.isDefined) {
          spatialConfig.target = target.get
        }
        else {
          if (spatialConfig.enableRetiming) {
            error(s"No target found with the name '${spatialConfig.targetName}'.")
            error(s"Available FPGA targets: ")
            targets.fpgas.foreach{t => error(s"  ${t.name}") }
            IR.logError()
            return
          }
          else {
            warn(s"No target found with the name '${spatialConfig.targetName}'.")
            warn("Defaulting to 'Default' device.")
            IR.logWarning()
          }
          spatialConfig.target = targets.Default
          spatialConfig.targetName = spatialConfig.target.name
        }
      }
    }
    else {
      // If the app defines one, use that one.
      if (spatialConfig.targetName != null) {
        warn("Command line target was ignored as target is specified in application.")
        IR.logWarning()
      }
      spatialConfig.target = this.target
      spatialConfig.targetName = spatialConfig.target.name
    }
    if (spatialConfig.target eq null) {
      error("No target specified. Specify target using: --fpga <device> or")
      error("override val target = <device>")
      IR.logError()
    }
    else {
      spatialConfig.target.areaModel.init()
      spatialConfig.target.latencyModel.init()
    }
  }

}
