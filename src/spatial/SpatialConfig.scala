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
  var enableOptimizedReduce: Boolean = true
  var enableForceBanking: Boolean = false
  var bankingEffort: Int = 1
  var unrollMetapipeOfParallels: Boolean = true
  var unrollParallelOfMetapipes: Boolean = false
  var allowInsanity: Boolean = false

  var enableArchDSE: Boolean = false

  var addRetimeRegisters = true // Enable adding registers after specified comb. logic

  var sramThreshold = 1 // Minimum number of elements to instantiate BRAM over Registers 
  var enableAsyncMem = false
  var enableRetiming = true
  var enableLooseIterDiffs = false
  var fuseAsFMA = true

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
      dstSC.enableOptimizedReduce = enableOptimizedReduce
      dstSC.enableForceBanking = enableForceBanking
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
