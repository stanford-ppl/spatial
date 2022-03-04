package spatial

import argon._
import argon.passes.{IRPrinter, RepeatedTraversal}
import poly.{ConstraintMatrix, ISL}
import models.AreaEstimator
import spatial.codegen.chiselgen._
import spatial.codegen.cppgen._
import spatial.codegen.roguegen._
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
import spatial.transform.{FIFOAccessFusion, _}
import spatial.traversal._
import spatial.model.RuntimeModelGenerator
import spatial.report._
import spatial.flows.SpatialFlowRules
import spatial.metadata.memory.{Dispatch, Duplicates}
import spatial.rewrites.SpatialRewriteRules
import spatial.transform.stream._
import spatial.util.spatialConfig
import spatial.util.ParamLoader

trait Spatial extends Compiler with ParamLoader {

  val target: HardwareTarget = null   // Optionally overridden by the application
  final val desc: String = "Spatial compiler"
  final val script: String = "spatial"
  private var overrideRetime = false
  implicit val mlModel: AreaEstimator = new AreaEstimator

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
    mlModel.startup(spatialConfig.useAreaModels)

    // --- Debug
    lazy val printer = IRPrinter(state, enable = config.enDbg)
    lazy val finalIRPrinter = IRPrinter(state, enable = true)
    lazy val textCleanup = TextCleanup(state)

    // --- Checking
    lazy val userSanityChecks  = UserSanityChecks(state, enable = !spatialConfig.allowInsanity)
    lazy val transformerChecks = CompilerSanityChecks(state, enable = spatialConfig.enLog && !spatialConfig.allowInsanity)
    lazy val finalSanityChecks = CompilerSanityChecks(state, enable = !spatialConfig.allowInsanity)
    lazy val streamChecks = CompilerSanityChecks(state, enable = true)

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
    lazy val streamTransformer     = StreamTransformer(state)
    lazy val switchTransformer     = SwitchTransformer(state)
    lazy val switchOptimizer       = SwitchOptimizer(state)
    lazy val blackboxLowering1     = BlackboxLowering(state, lowerTransfers = false)
    lazy val blackboxLowering2     = BlackboxLowering(state, lowerTransfers = true)
    lazy val memoryDealiasing      = MemoryDealiasing(state)
    lazy val pipeInserter          = PipeInserter(state)
    lazy val transientCleanup      = TransientCleanup(state)
    lazy val unrollTransformer     = UnrollingTransformer(state)
    lazy val rewriteTransformer    = RewriteTransformer(state)
    lazy val memoryCleanup         = MemoryCleanupTransformer(state)
    lazy val laneStaticTransformer = LaneStaticTransformer(state)
    lazy val flatteningTransformer = FlatteningTransformer(state)
    lazy val bindingTransformer    = BindingTransformer(state)
    lazy val retiming              = RetimingTransformer(state)
    lazy val accumTransformer      = AccumTransformer(state)
    lazy val regReadCSE            = RegReadCSE(state)
    lazy val unitPipeToForeach     = UnitPipeToForeachTransformer(state)
    lazy val foreachToUnitpipe     = ForeachToUnitpipeTransformer(state)
    lazy val metapipeToStream      = MetapipeToStreamTransformer(state)
    lazy val regElim               = RegWriteReadElimination(state)
    lazy val allocMotion           = AllocMotion(state)
    lazy val reduceToForeach       = ReduceToForeach(state)
    lazy val unitpipeCombine       = UnitpipeCombineTransformer(state)
    lazy val streamifyAnnotator    = StreamifyAnnotator(state)
    lazy val transientMotion       = TransientMotion(state)
    lazy val fifoAccessFusion      = FIFOAccessFusion(state)
    import Stripper.S
    lazy val retimeStrippers = MetadataStripper(state, S[Duplicates], S[Dispatch], S[spatial.metadata.memory.Ports], S[spatial.metadata.memory.GroupId])

    lazy val streamifyStripper = MetadataStripper(state, S[spatial.metadata.transform.Streamify], S[spatial.metadata.memory.StreamBufferIndex])

    def createDump(n: String) = Seq(TreeGen(state, n, s"${n}_IR"), HtmlIRGenSpatial(state, s"${n}_IR"))

    lazy val streamBufferExpansion = StreamBufferExpansion(state)

    lazy val bankingAnalysis = Seq(retimingAnalyzer, accessAnalyzer, iterationDiffAnalyzer, memoryAnalyzer, memoryAllocator, printer)
    lazy val streamifyAnalysis = Seq(reduceToForeach, pipeInserter, unitPipeToForeach, printer, transientMotion) ++ DCE ++
      bankingAnalysis ++ Seq(streamifyAnnotator, printer, metapipeToStream, printer, streamBufferExpansion, printer, foreachToUnitpipe, printer, allocMotion, pipeInserter, streamifyStripper, printer, fifoAccessFusion, printer) ++ Seq(streamChecks)

    lazy val streamify = createDump("PreStream") ++ Seq(RepeatedTraversal(state, streamifyAnalysis ++ Seq(retimeStrippers), (iter: Int) => {
      createDump(s"streamify_$iter")
    }, maxIters = spatialConfig.maxStreamifyIters))

    // --- Codegen
    lazy val chiselCodegen = ChiselGen(state)
    lazy val resourceReporter = ResourceReporter(state, mlModel)
    lazy val cppCodegen    = CppGen(state)
    lazy val rogueCodegen   = RogueGen(state)
    lazy val treeCodegen   = TreeGen(state)
    lazy val irCodegen     = HtmlIRGenSpatial(state)
    lazy val scalaCodegen  = ScalaGenSpatial(state)
    lazy val dseRuntimeModelGen = RuntimeModelGenerator(state, version = "dse")
    lazy val finalRuntimeModelGen = RuntimeModelGenerator(state, version = "final")
    lazy val pirCodegen    = PIRGenSpatial(state)
    lazy val tsthCodegen   = TungstenHostGenSpatial(state)
    lazy val dotFlatGen    = DotFlatGenSpatial(state)
    lazy val dotHierGen    = DotHierarchicalGenSpatial(state)

    // Utilities
    lazy val DCE = Seq(useAnalyzer, transientCleanup, printer, transformerChecks)

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
        (spatialConfig.enableSynth && !spatialConfig.enableSim) ? textCleanup ==>
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
        ((!spatialConfig.vecInnerLoop) ? laneStaticTransformer)   ==>  printer ==>
        /** Control insertion */
        pipeInserter        ==> printer ==> transformerChecks ==>
        /** CSE on regs */
        regReadCSE          ==>
        /** Dead code elimination */
        DCE ==>
        /** Metapipelines to Streams */
        spatialConfig.streamify ? streamify ==>
        /** Stream controller rewrites */
        (spatialConfig.distributeStreamCtr ? streamTransformer) ==> printer ==>
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
        DCE ==> Seq(retimingAnalyzer, printer, streamChecks) ==>
        /** Hardware Rewrites **/
        rewriteAnalyzer     ==>
        (spatialConfig.enableOptimizedReduce ? accumAnalyzer) ==> printer ==>
        rewriteTransformer  ==> printer ==> transformerChecks ==>
        memoryCleanup  ==> printer ==> transformerChecks ==>
        /** Pipe Flattening */
        flatteningTransformer ==> bindingTransformer ==>
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
        irCodegen           ==>
        (spatialConfig.enableFlatDot ? dotFlatGen)      ==>
        (spatialConfig.enableDot ? dotHierGen)      ==>
        (spatialConfig.enableSim   ? scalaCodegen)  ==>
        (spatialConfig.enableSynth ? chiselCodegen) ==>
        ((spatialConfig.enableSynth && spatialConfig.target.host == "cpp") ? cppCodegen) ==>
        ((spatialConfig.target.host == "rogue") ? rogueCodegen) ==>
        (spatialConfig.reportArea ? resourceReporter) ==>
        (spatialConfig.enablePIR ? pirCodegen) ==>
        (spatialConfig.enableTsth ? tsthCodegen)
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
    cli.opt[Int]("hypermapper_evalsPerIter").action{(t,_) => spatialConfig.hypermapper_doeSamples = t }.text("Set number of evaluations per optimization iteration hypermapper should use.")
    cli.opt[Int]("hypermapper_doeSamples").action{(t,_) => spatialConfig.hypermapper_doeSamples = t }.text("Set number of random samples hypermapper should use in design of experiment.")
    cli.opt[Int]("hypermapper_iters").action{(t,_) => spatialConfig.hypermapper_iters = t }.text("Set number of optimization iterations for hypermapper.")

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

    cli.opt[Unit](name="fdot").action( (_, _) =>
      spatialConfig.enableFlatDot = true
    ).text("Enable flat dot graph generation [false]")

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
      spatialConfig.groupUnrolledAccess = true
      spatialConfig.targetName = "Plasticine"
      spatialConfig.enableForceBanking = true
      spatialConfig.enableParallelBinding = false
      //spatialConfig.unrollParallelOfMetapipes = true
      //spatialConfig.unrollMetapipeOfParallels = false
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
      spatialConfig.enableParallelBinding = false
      //spatialConfig.unrollParallelOfMetapipes = true
      //spatialConfig.unrollMetapipeOfParallels = false
    }.text("Enable Tungsten Host Codegen [false]")

    cli.note("")
    cli.note("Experimental:")

    cli.opt[Unit]("noModifyStream").action{ (_,_) => 
      spatialConfig.distributeStreamCtr = false
    }.text("Disable transformer that converts Stream.Foreach controllers into Stream Unit Pipes with the counter tacked on to children counters (default: do modify stream [i.e. run transformer])")

    cli.opt[Unit]("streamify").action {(_, _) =>
      spatialConfig.streamify = true
    }.text("Enable automatically transforming controllers into streams.")

    cli.opt[Unit]("nostreamify").action {(_, _) =>
      spatialConfig.streamify = false
    }.text("Disable automatically transforming controllers into streams.")

    cli.opt[Int]("maxStreamifyIters").action { (i, _) =>
      spatialConfig.maxStreamifyIters = i }.text("Maximum number of iterations transforming controllers into streams")

    cli.opt[Unit]("noBindParallels").action{ (_,_) => 
      spatialConfig.enableParallelBinding = false
    }.text("""Automatically wrap consecutive stages of a controller in a Parallel pipe if they do not have any dependencies (default: bind parallels)""")

    cli.opt[Int]("codeWindow").action{ (t,_) => 
      spatialConfig.codeWindow = t
    }.text("""Size of code window for Java-style chunking, which breaks down large IR blocks into multiple levels of Java objects.  Increasing can sometimes solve the GC issue during Chisel compilation (default: 50)""")

    cli.opt[Int]("bankingEffort").action{ (t,_) =>
      assert(t == 0 || t == 1 || t == 2, "Only efforts of 0, 1, or 2 are supported.  See --numSchemesPerRegion or --bankingTimeout for more fine-grained controls")
      spatialConfig.bankingEffort = t
      if (t == 0) spatialConfig.numSchemesPerRegion = 1
    }.text("""Specify the level of effort to put into banking local memories.  i.e:
      0: Quit banking analyzer after first banking scheme is found
      1: (default) Allow banking analyzer to search 4 regions (flat, hierarchical, flat+full_duplication, hierarchical+full_duplication)
      2: Allow banking analyzer to search each BankingView/RegroupDims combination.  Good enough for most cases (i.e. flat+full_duplication, flat+duplicateAxis(0), flat+duplicateAxes(0,1), etc)
""")

    cli.opt[Int]("numSchemesPerRegion").action{ (t,_) =>
      spatialConfig.numSchemesPerRegion = t
    }.text("""Specify how many valid schemes to look for in each region [Default: 2]""")

    cli.opt[Unit]("bfsAnalysis").action{ (_,_) =>
      spatialConfig.dfsAnalysis = false
    }.text("""Use BFS when searching for consumers in IR [Default: DFS]""")

    cli.opt[Unit]("dfsAnalysis").action{ (_,_) =>
      spatialConfig.dfsAnalysis = true
    }.text("""Use DFS when searching for consumers in IR [Default: DFS]""")

    cli.opt[Int]("mersenneRadius").action{ (t,_) =>
      spatialConfig.mersenneRadius = t
    }.text(
      """Specify how many multiples of modulus to search when trying to optimize FixMod as a PriorityMux + MersenneMod
        | (i.e. x % 5 requires mersenneRadius >= 3 in order to express it as a 3-way Priority mux on x % 15, since 15 is the closest Mersenne number [Default: 16]""".stripMargin)

    cli.opt[Int]("bankingTimeout").action{ (t,_) =>
      spatialConfig.bankingTimeout = t
    }.text("""Specify how many schemes to attempt before quitting the banking analyzer. (default: 50000)""")

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

    cli.opt[Unit]("noFuseFMA").action{(_,_) => spatialConfig.fuseAsFMA = false}.text("Do not fuse patterns in the form of Add(Mul(a,b),c) as FMA(a,b,c) [false]")
    cli.opt[Unit]("forceFuseFMA").action{(_,_) => spatialConfig.forceFuseFMA = true}.text("Force all Add(Mul(a,b),c) patterns to become FMA(a,b,c), even if they increase initiation interval.  --noFuseFMA takes priority [false]")

    cli.opt[Unit]("perpetualIP").action{(_,_) => spatialConfig.perpetualIP = true}.text("Force SpatialIP external enable signal to be hardcoded to true.  Good for apps where you plan to rip the SpatialIP verilog and plug it into something else [false]")
    cli.opt[String]("dmaChannelAssignment").action{(t,_) => spatialConfig.channelAssignment = t}.text("Select how app load/store channels are allocated to DRAM channels. The number of channels is FPGA specific. Options are AllToOne, BasicRoundRobin, ColoredRoundRobin, AdvancedColored [AllToOne]")

    cli.opt[Unit]("noOptimizeMul").action{(_,_) => spatialConfig.optimizeMul = false}.text("Do not optimize multiplications if they can be rewritten as sum/subtraction of two pow2 multiplies")
    cli.opt[Unit]("noOptimizeMod").action{(_,_) => spatialConfig.optimizeMod = false}.text("Do not optimize modulo if they can be rewritten shifts and adds using Mersenne numbers")
    cli.opt[Unit]("crandallMod").action{(_,_) => spatialConfig.useCrandallMod = true}.text("Do not optimize modulo if they can be rewritten shifts and adds using Mersenne numbers")
    cli.opt[Unit]("optimizeDiv").action{(_,_) => spatialConfig.optimizeDiv = true}.text("Replace division by Crandall's Algorithm to avoid using DSPs.  Also turns on Crandall-based modulo since the datapath will already contain most of the nodes required to replace modulo")
    cli.opt[Unit]("dualReadPort").action{(_,_) => spatialConfig.dualReadPort = true}.text("Treat all memories as dual read port")

    cli.opt[Unit]("noBroadcast").action{(_,_) => spatialConfig.enableBroadcast = false }.text("Disable broadcast reads")

    cli.opt[Unit]("asyncMem").action{(_,_) => spatialConfig.enableAsyncMem = true }.text("Enable asynchronous memories")

    cli.opt[Unit]("insanity").action{(_,_) => spatialConfig.allowInsanity = true }.text("Disable sanity checks, allows insanity to happen.  Not recommended!")

    cli.opt[Unit]("prioritizeFlat").action{(_,_) => spatialConfig.prioritizeFlat = true }.text("Prioritize flat banking schemes over hierarchical ones when searching. Not recommended!")
    cli.opt[Unit]("legacyBanking").action{(_,_) =>
      spatialConfig.prioritizeFlat = true
      spatialConfig.useAreaModels = false
      spatialConfig.numSchemesPerRegion = 1
      spatialConfig.bankingEffort = 0
      spatialConfig.optimizeMod = false
      spatialConfig.optimizeMul = false
    }.text("Prioritize flat banking schemes over hierarchical ones when searching. Not recommended!")

    cli.opt[Unit]("instrumentation").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.enableInstrumentation = true
      spatialConfig.enableRetiming = true
      overrideRetime = true
    }.text("Enable counters for each loop to assist in balancing pipelines")

    cli.opt[Unit]("noAreaModels").action { (_,_) => 
      spatialConfig.useAreaModels = false
    }.text("Only use crude models for estimating area during banking, and do not generate area report.  Use this flag if you do not have the correct python dependencies to run modeling")

    cli.opt[Unit]("reportArea").action { (_,_) =>
      spatialConfig.reportArea = true
    }.text("Generate area model report.")

    cli.opt[Unit]("instrument").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.enableInstrumentation = true
      spatialConfig.enableRetiming = true
      overrideRetime = true
    }.text("Enable counters for each loop to assist in balancing pipelines")

    cli.opt[Unit]("nomodular").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.enableModular = false
    }.text("Disables modular codegen and puts all logic in AccelUnit")

    cli.opt[Unit]("modular").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.enableModular = true
    }.text("Enables modular codegen")

    cli.opt[Unit]("forceBanking").action { (_,_) => 
      spatialConfig.enableForceBanking = true
    }.text("Ensures that memories will always get banked and compiler will never decide that it is cheaper to duplicate")

    cli.opt[Unit]("bank-groupUnroll").action { (_,_) => 
      spatialConfig.groupUnrolledAccess = true
    }.text("Group access in memory configure will prioritize grouping unrolled accesses first")

    cli.opt[Unit]("tightControl").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.enableTightControl = true
      spatialConfig.enableRetiming = true
      overrideRetime = true
    }.text("Enable tighter timing between controllers at the expense of potentially failing timing")

    cli.opt[Unit]("cheapFifos").action { (_,_) => // Must necessarily turn on retiming
      spatialConfig.useCheapFifos = true
      spatialConfig.enableRetiming = true
      overrideRetime = true
    }.text("<DEPRECATED> Enable cheap fifos where accesses must be multiples of each other and not have lane-enables")

    cli.opt[Boolean]("cheapSRAMs").action { (t,_) => // Must necessarily turn on retiming
      spatialConfig.useCheapSRAMs = t
    }.text("Enable/Disable cheap SRAM templates, which is mostly safe but could give wrong results in stream-based control structures or complex banking schemes (i.e. remove SRAM enable hysteresis) (Default: True)")

    cli.opt[Int]("sramThreshold").action { (t,_) => spatialConfig.sramThreshold = t }.text("Minimum number of elements in memory to instantiate BRAM over Registers")

    cli.opt[Unit]("noOptimizeReduce").action { (_,_) => 
      spatialConfig.enableOptimizedReduce = false
    }.text("Do not squeeze II of reductions to 1 where possible, and instantiate specialized reduce node")

    cli.opt[Unit]("imperfect").action { (_, _) =>
      spatialConfig.imperfect = true
    }.text("Do not attempt to perfect loops")

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
      spatialConfig.target.areaModel(mlModel).init()
      spatialConfig.target.latencyModel.init()
    }
    if (config.genDirOverride && java.nio.file.Files.exists(java.nio.file.Paths.get(config.genDir)) ||
        !config.genDirOverride && java.nio.file.Files.exists(java.nio.file.Paths.get(config.genDir + "/" + config.name))) {
      warn(s"Output directory ${if (config.genDirOverride) config.genDir else {config.genDir + "/" + config.name}} already exists!  Generated files")
      warn(s"from other targets may still exist and may interfere with this build!")
    }
  }

}
