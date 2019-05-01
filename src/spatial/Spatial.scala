package spatial

import argon._
import argon.passes.IRPrinter
import poly.{ConstraintMatrix, ISL}
import spatial.codegen.chiselgen._
import spatial.codegen.cppgen._
import spatial.codegen.scalagen._
import spatial.codegen.treegen._
import spatial.codegen.pirgen._
import spatial.codegen.tsthgen._
import spatial.codegen.dotgen._
import spatial.codegen.resourcegen._

import spatial.lang.{Tensor1, Text, Void}
import spatial.node.InputArguments
import spatial.metadata.access._
import spatial.targets.HardwareTarget
import spatial.dse._
import spatial.transform._
import spatial.traversal._
import spatial.model.RuntimeModelGenerator
import spatial.report._
import spatial.flows.SpatialFlowRules
import spatial.rewrites.SpatialRewriteRules
import spatial.util.spatialConfig
import spatial.util.ParamLoader

trait Spatial extends Compiler with ParamLoader {

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
    lazy val userSanityChecks  = UserSanityChecks(state, enable = !spatialConfig.allowInsanity)
    lazy val transformerChecks = CompilerSanityChecks(state, enable = spatialConfig.enLog && !spatialConfig.allowInsanity)
    lazy val finalSanityChecks = CompilerSanityChecks(state, enable = !spatialConfig.allowInsanity)

    // --- Analysis
    lazy val cliNaming          = CLINaming(state)
    lazy val useAnalyzer        = UseAnalyzer(state)
    lazy val accessAnalyzer     = AccessAnalyzer(state)
    lazy val memoryAnalyzer     = MemoryAnalyzer(state)
    lazy val memoryAllocator    = MemoryAllocator(state)
    lazy val rewriteAnalyzer    = RewriteAnalyzer(state)
    lazy val bufferRecompute       = BufferRecompute(state)
    lazy val retimingAnalyzer   = RetimingAnalyzer(state)
    lazy val iterationDiffAnalyzer = IterationDiffAnalyzer(state)
    lazy val initiationAnalyzer = InitiationAnalyzer(state)
    lazy val accumAnalyzer      = AccumAnalyzer(state)
    lazy val broadcastCleanup   = BroadcastCleanupAnalyzer(state)

    // --- DSE
    lazy val paramAnalyzer        = ParameterAnalyzer(state)
    lazy val dsePass              = DSEAnalyzer(state)

    // --- Reports
    lazy val memoryReporter = MemoryReporter(state)
    lazy val retimeReporter = RetimeReporter(state)

    // --- Transformer
    lazy val friendlyTransformer   = FriendlyTransformer(state)
    lazy val switchTransformer     = SwitchTransformer(state)
    lazy val switchOptimizer       = SwitchOptimizer(state)
    lazy val blackboxLowering1     = BlackboxLowering(state, lowerTransfers = false)
    lazy val blackboxLowering2     = BlackboxLowering(state, lowerTransfers = true)
    lazy val memoryDealiasing      = MemoryDealiasing(state)
    lazy val pipeInserter          = PipeInserter(state)
    lazy val transientCleanup      = TransientCleanup(state)
    lazy val unrollTransformer     = UnrollingTransformer(state)
    lazy val rewriteTransformer    = RewriteTransformer(state)
    lazy val flatteningTransformer = FlatteningTransformer(state)
    lazy val retiming              = RetimingTransformer(state)
    lazy val accumTransformer      = AccumTransformer(state)
    lazy val regReadCSE            = RegReadCSE(state)

    // --- Codegen
    lazy val chiselCodegen = ChiselGen(state)
    lazy val resourceReporter = ResourceReporter(state)
    lazy val cppCodegen    = CppGen(state)
    lazy val treeCodegen   = TreeGen(state)
    lazy val irCodegen     = HtmlIRGenSpatial(state)
    lazy val scalaCodegen  = ScalaGenSpatial(state)
    lazy val dseRuntimeModelGen = RuntimeModelGenerator(state, version = "dse")
    lazy val finalRuntimeModelGen = RuntimeModelGenerator(state, version = "final")
    lazy val pirCodegen    = PIRGenSpatial(state)
    lazy val tsthCodegen   = TungstenHostGenSpatial(state)
    lazy val dotFlatGen    = DotFlatGenSpatial(state)
    lazy val dotHierGen    = DotHierarchicalGenSpatial(state)

    val result = {

        block ==> printer     ==>
        cliNaming           ==>
        (friendlyTransformer) ==> printer ==> transformerChecks ==>
        userSanityChecks    ==>
        /** Black box lowering */
        (switchTransformer)   ==> printer ==> transformerChecks ==>
        (switchOptimizer)     ==> printer ==> transformerChecks ==>
        (blackboxLowering1)   ==> printer ==> transformerChecks ==>
        /** More black box lowering */
        (blackboxLowering2)   ==> printer ==> transformerChecks ==>
        /** DSE */
        ((spatialConfig.enableArchDSE) ? paramAnalyzer) ==> 
        /** Optional scala model generator */
        ((spatialConfig.enableRuntimeModel) ? retimingAnalyzer) ==>
        ((spatialConfig.enableRuntimeModel) ? initiationAnalyzer) ==>
        ((spatialConfig.enableRuntimeModel) ? dseRuntimeModelGen) ==>
        (spatialConfig.enableArchDSE ? dsePass) ==> 
        //blackboxLowering    ==> printer ==> transformerChecks ==>
        switchTransformer   ==> printer ==> transformerChecks ==>
        switchOptimizer     ==> printer ==> transformerChecks ==>
        memoryDealiasing    ==> printer ==> transformerChecks ==>
        /** Control insertion */
        pipeInserter        ==> printer ==> transformerChecks ==>
        /** CSE on regs */
        regReadCSE          ==>
        /** Dead code elimination */
        useAnalyzer         ==>
        transientCleanup    ==> printer ==> transformerChecks ==>
        /** Memory analysis */
        retimingAnalyzer    ==>
        accessAnalyzer      ==>
        iterationDiffAnalyzer   ==>
        memoryAnalyzer      ==>
        memoryAllocator     ==> printer ==>
        /** Unrolling */
        unrollTransformer   ==> printer ==> transformerChecks ==>
        /** CSE on regs */
        regReadCSE          ==>
        /** Dead code elimination */
        useAnalyzer         ==>
        transientCleanup    ==> printer ==> transformerChecks ==>
        /** Hardware Rewrites **/
        rewriteAnalyzer     ==>
        (spatialConfig.enableOptimizedReduce ? accumAnalyzer) ==> printer ==>
        rewriteTransformer  ==> printer ==> transformerChecks ==>
        /** Pipe Flattening */
        flatteningTransformer ==> 
        /** Update buffer depths */
        bufferRecompute     ==> printer ==> transformerChecks ==>
        /** Accumulation Specialization **/
        (spatialConfig.enableOptimizedReduce ? accumAnalyzer) ==> printer ==>
        (spatialConfig.enableOptimizedReduce ? accumTransformer) ==> printer ==> transformerChecks ==>
        /** Retiming */
        retimingAnalyzer    ==> printer ==>
        retiming            ==> printer ==> transformerChecks ==>
        retimeReporter      ==>
        /** Broadcast cleanup */
        broadcastCleanup    ==> printer ==>
        /** Schedule finalization */
        initiationAnalyzer  ==>
        /** final model generation */
        ((spatialConfig.enableRuntimeModel) ? finalRuntimeModelGen) ==>
        /** Reports */
        memoryReporter      ==>
        finalIRPrinter      ==>
        finalSanityChecks   ==>
        /** Code generation */
        treeCodegen         ==>
        //(spatialConfig.enableDot ? dotFlatGen)      ==>
        (spatialConfig.enableDot ? dotHierGen)      ==>
        (spatialConfig.enableSim   ? scalaCodegen)  ==>
        (spatialConfig.enableSynth ? chiselCodegen) ==>
        (spatialConfig.enableSynth ? cppCodegen) ==>
        (spatialConfig.enableResourceReporter ? resourceReporter) ==>
        (spatialConfig.enablePIR ? pirCodegen) ==>
        (spatialConfig.enableTsth ? tsthCodegen) ==>
        irCodegen           
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
    cli.opt[Unit]("tune").action{(_,_) => spatialConfig.dseMode = DSEMode.Bruteforce; spatialConfig.enableArchDSE = true; spatialConfig.enableRuntimeModel = true}.text("Enable default design tuning (bruteforce)")
    cli.opt[Unit]("bruteforce").action{(_,_) => spatialConfig.dseMode = DSEMode.Bruteforce; spatialConfig.enableArchDSE = true; spatialConfig.enableRuntimeModel = true }.text("Enable brute force tuning.")
    cli.opt[Unit]("heuristic").action{(_,_) => spatialConfig.dseMode = DSEMode.Heuristic; spatialConfig.enableArchDSE = true; spatialConfig.enableRuntimeModel = true }.text("Enable heuristic tuning.")
    cli.opt[Unit]("experiment").action{(_,_) => spatialConfig.dseMode = DSEMode.Experiment; spatialConfig.enableArchDSE = true; spatialConfig.enableRuntimeModel = true }.text("Enable DSE experimental mode.").hidden()
    cli.opt[Unit]("hypermapper").action{(_,_) => spatialConfig.dseMode = DSEMode.HyperMapper; spatialConfig.enableArchDSE = true; spatialConfig.enableRuntimeModel = true }.text("Enable hypermapper dse.").hidden()
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

    cli.opt[Unit]("reporter").action( (_,_) =>
      spatialConfig.enableResourceReporter = true
    ).text("Enable resource reporter [false]")

    cli.opt[Unit]("pir").action { (_,_) =>
      spatialConfig.enablePIR = true
      spatialConfig.enableTsth = true
      spatialConfig.enableInterpret = false
      spatialConfig.enableSynth = false
      spatialConfig.enableRetiming = false
      //spatialConfig.enableBroadcast = false
      spatialConfig.vecInnerLoop = true
      //spatialConfig.ignoreParEdgeCases = true
      spatialConfig.enableBufferCoalescing = false
      spatialConfig.targetName = "Plasticine"
      spatialConfig.enableForceBanking = true
    }.text("Enable codegen to PIR [false]")

    cli.opt[Unit]("tsth").action { (_,_) =>
      spatialConfig.enablePIR = false
      spatialConfig.enableTsth = true
      spatialConfig.enableInterpret = false
      spatialConfig.enableSynth = false
      spatialConfig.enableRetiming = false
      //spatialConfig.enableBroadcast = false
      spatialConfig.vecInnerLoop = true
      //spatialConfig.ignoreParEdgeCases = true
      spatialConfig.enableBufferCoalescing = false
      //spatialConfig.enableDot = true
      spatialConfig.targetName = "Plasticine"
      spatialConfig.enableForceBanking = true
    }.text("Enable Tungsten Host Codegen [false]")

    cli.note("")
    cli.note("Experimental:")

    cli.opt[Unit]("mop").action{ (_,_) => 
      spatialConfig.unrollMetapipeOfParallels = true
      spatialConfig.unrollParallelOfMetapipes = false
    }.text("""
      Unroll outer loops into a metapipe of parallel controllers (default).  i.e: 
      Foreach(10 by 1 par 2)
       |-- Pipe1{...}
       |-- Pipe2{...}
          * becomes *
      Foreach(10 by 1 par 2)
       |-- Parallel 
       |    |-- Pipe1{...}
       |    |-- Pipe1{...}
       |-- Parallel 
            |-- Pipe2{...}
            |-- Pipe2{...}
""")

    cli.opt[Unit]("pom").action{ (_,_) => 
      spatialConfig.unrollParallelOfMetapipes = true
      spatialConfig.unrollMetapipeOfParallels = false
    }.text("""
      Unroll outer loops into a parallel of metapipe controllers.  i.e: 
      Foreach(10 by 1 par 2)
       |-- Pipe1{...}
       |-- Pipe2{...}
          * becomes * 
      Parallel
       |-- Foreach(0 until 10 by 2) 
       |    |-- Pipe1{...}
       |    |-- Pipe2{...}
       |-- Foreach(1 until 10 by 2)
            |-- Pipe1{...}
            |-- Pipe2{...}
""")

    cli.opt[Unit]("looseIterDiffs").action{ (_,_) => 
      spatialConfig.enableLooseIterDiffs = true
    }.text("Ignore iteration difference analysis for loops where some iterators are not part of the accumulator cycle but its leading iterators run for an unknown duration")

    cli.opt[Unit]("retime").action{ (_,_) =>
      spatialConfig.enableRetiming = true
      overrideRetime = true
    }.text("Force retiming for --sim")

    cli.opt[Unit]("noretime").action{ (_,_) =>
      spatialConfig.enableRetiming = false
      spatialConfig.enableOptimizedReduce = false
      overrideRetime = true
    }.text("Disable retiming (NOTE: May generate buggy verilog)")

    cli.opt[Unit]("noFuseFMA").action{(_,_) => spatialConfig.fuseAsFMA = false}.text("Do not fuse patterns in the form of Add(Mul(a,b),c) as FMA(a,b,c)")

    cli.opt[Unit]("noBroadcast").action{(_,_) => spatialConfig.enableBroadcast = false }.text("Disable broadcast reads")

    cli.opt[Unit]("asyncMem").action{(_,_) => spatialConfig.enableAsyncMem = true }.text("Enable asynchronous memories")

    cli.opt[Unit]("insanity").action{(_,_) => spatialConfig.allowInsanity = true }.text("Disable sanity checks, allows insanity to happen.  Not recommended!")

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

    cli.opt[Unit]("nomodular").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.enableModular = false
    }.text("Disables modular codegen and puts all logic in AccelTop")

    cli.opt[Unit]("modular").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.enableModular = true
    }.text("Enables modular codegen")

    cli.opt[Unit]("forceBanking").action { (_,_) => 
      spatialConfig.enableForceBanking = true
    }.text("Ensures that memories will always get banked and compiler will never decide that it is cheaper to duplicate")

    cli.opt[Unit]("tightControl").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.enableTightControl = true
      spatialConfig.enableRetiming = true
      overrideRetime = true
    }.text("Enable tighter timing between controllers at the expense of potentially failing timing")

    cli.opt[Unit]("cheapFifos").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.useCheapFifos = true
      spatialConfig.enableRetiming = true
      overrideRetime = true
    }.text("Enable cheap fifos where accesses must be multiples of each other and not have lane-enables")

    cli.opt[Int]("sramThreshold").action { (t,_) => spatialConfig.sramThreshold = t }.text("Minimum number of elements in memory to instantiate BRAM over Registers")

    cli.opt[Unit]("noOptimizeReduce").action { (_,_) => 
      spatialConfig.enableOptimizedReduce = false
    }.text("Do not squeeze II of reductions to 1 where possible, and instantiate specialized reduce node")

    cli.opt[Unit]("runtime").action{ (_,_) =>
      spatialConfig.enableRuntimeModel = true
    }.text("Enable application runtime estimation")

    cli.opt[String]("load-param").action{(x,_) => 
      loadParams(x)
    }.text("Set path to load application parameter")

    cli.opt[String]("save-param").action{(x,_) => 
      spatialConfig.paramSavePath = Some(x)
    }.text("Set path to store application parameter")
  }

  override def postprocess(block: Block[_]): Unit = {
    spatialConfig.paramSavePath.foreach(path => saveParams(path))
    super.postprocess(block)
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
        val target = targets.all.find{_.name.toLowerCase == spatialConfig.targetName.toLowerCase }
        if (target.isDefined) {
          spatialConfig.target = target.get
        }
        else {
          if (spatialConfig.enableRetiming) {
            error(s"No target found with the name '${spatialConfig.targetName}'.")
            error(s"Available FPGA targets: ")
            targets.fpgas.foreach{t => error(s"  ${t.name}") }
            error(s"Available Other targets: ")
            (targets.all -- targets.fpgas).foreach{t => error(s"  ${t.name}") }
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
    if (config.genDirOverride && java.nio.file.Files.exists(java.nio.file.Paths.get(config.genDir)) ||
        !config.genDirOverride && java.nio.file.Files.exists(java.nio.file.Paths.get(config.genDir + "/" + config.name))) {
      warn(s"Output directory ${if (config.genDirOverride) config.genDir else {config.genDir + "/" + config.name}} already exists!  Generated files")
      warn(s"from other targets may still exist and may interfere with this build!")
    }
  }

}
