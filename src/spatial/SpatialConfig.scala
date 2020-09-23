package spatial

import argon.Config
import spatial.dse.DSEMode
import spatial.targets.HardwareTarget

class SpatialConfig extends Config {

  var targetName: String = _
  var target: HardwareTarget = _

  var dseMode: DSEMode = DSEMode.Disabled
  var threads: Int = 8

  var enableRuntimeModel: Boolean = false

  //Interpreter
  var inputs: Array[String] = Array()
  var enableInterpret: Boolean = false

  // --- Backends --- //
  var enableSim: Boolean = false
  var enableSynth: Boolean = true
  var enableResourceReporter: Boolean = false
  var enableTree: Boolean = true
  var enableDot: Boolean = false

  var enableInstrumentation: Boolean = false
  var enableModular: Boolean = true
  var enableTightControl: Boolean = false
  var useCheapFifos: Boolean = false
  var useCheapSRAMs: Boolean = false
  var prioritizeFlat: Boolean = false
  var numSchemesPerRegion: Int = 2
  var bankingTimeout: Int = 50000
  var mersenneRadius: Int = 16
  var enableOptimizedReduce: Boolean = true
  var distributeStreamCtr: Boolean = true
  var useAreaModels: Boolean = true
  var reportArea: Boolean = false
  var enableForceBanking: Boolean = false
  var groupUnrolledAccess: Boolean = false
  var enableParallelBinding: Boolean = true
  var bankingEffort: Int = 1
  var unrollMetapipeOfParallels: Boolean = true
  var unrollParallelOfMetapipes: Boolean = false
  var allowInsanity: Boolean = false
  var codeWindow: Int = 50
  var hypermapper_doeSamples: Int = 10000
  var hypermapper_iters: Int = 5

  var enableArchDSE: Boolean = false

  var addRetimeRegisters = true // Enable adding registers after specified comb. logic

  var sramThreshold = 1 // Minimum number of elements to instantiate BRAM over Registers 
  var enableAsyncMem = false
  var dualReadPort = false
  var dfsAnalysis = true
  var perpetualIP = false
  var channelAssignment = "AllToOne"
  var enableRetiming = true
  var enableLooseIterDiffs = false
  var optimizeMul = true
  var optimizeMod = true
  var useCrandallMod = false
  var optimizeDiv = false
  var fuseAsFMA = true
  var forceFuseFMA = false

  lazy val HYPERMAPPER: String = sys.env.getOrElse("HYPERMAPPER_HOME", {throw new Exception("Please set the HYPERMAPPER_HOME environment variable."); sys.exit() })

  var enableBroadcast = true // Allow broadcasting reads

  // Internal flag used to mark whether unit pipe transformer has been run or not
  var allowPrimitivesInOuterControl = true


  var ignoreParEdgeCases: Boolean = false
  var vecInnerLoop: Boolean = false
  var enableBufferCoalescing: Boolean = true

  var enablePIR = false
  var enableTsth = false

  var paramSavePath:Option[String] = None

  override def copyTo(dst: Config): Unit = {
    if (dst.isInstanceOf[SpatialConfig]) {
      val dstSC = dst.asInstanceOf[SpatialConfig]
      dstSC.targetName = targetName
      dstSC.target = target
      dstSC.dseMode = dseMode
      dstSC.threads = threads
      dstSC.enableRuntimeModel = enableRuntimeModel
      dstSC.inputs = inputs
      dstSC.enableInterpret = enableInterpret
      dstSC.enableSim = enableSim
      dstSC.enableSynth = enableSynth
      dstSC.enableResourceReporter = enableResourceReporter
      dstSC.enableTree = enableTree
      dstSC.enableDot = enableDot
      dstSC.enableInstrumentation = enableInstrumentation
      dstSC.enableTightControl = enableTightControl
      dstSC.useCheapFifos = useCheapFifos
      dstSC.useCheapSRAMs = useCheapSRAMs
      dstSC.enableOptimizedReduce = enableOptimizedReduce
      dstSC.enableForceBanking = enableForceBanking
      dstSC.groupUnrolledAccess = groupUnrolledAccess
      // dstSC.enableSplitting = enableSplitting
      dstSC.enableArchDSE = enableArchDSE
      dstSC.addRetimeRegisters = addRetimeRegisters
      // dstSC.compressWires = compressWires
      dstSC.sramThreshold = sramThreshold
      dstSC.enableAsyncMem = enableAsyncMem
      dstSC.enableRetiming = enableRetiming
      dstSC.fuseAsFMA = fuseAsFMA
      dstSC.enableBroadcast = enableBroadcast
      dstSC.allowPrimitivesInOuterControl = allowPrimitivesInOuterControl
      dstSC.ignoreParEdgeCases = ignoreParEdgeCases
      dstSC.vecInnerLoop = vecInnerLoop
      dstSC.enableBufferCoalescing = enableBufferCoalescing
      dstSC.enablePIR = enablePIR
      dstSC.paramSavePath = paramSavePath
      super.copyTo(dst)
    } else {
      super.copyTo(dst)
    }
  }

}
