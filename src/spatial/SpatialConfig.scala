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
  var allowInsanity: Boolean = false

  var enableArchDSE: Boolean = false

  var addRetimeRegisters = true // Enable adding registers after specified comb. logic

  var sramThreshold = 1 // Minimum number of elements to instantiate BRAM over Registers 
  var enableAsyncMem = false
  var enableRetiming = true
  var fuseAsFMA = true

  var enableBroadcast = true // Allow broadcasting reads

  // Internal flag used to mark whether unit pipe transformer has been run or not
  var allowPrimitivesInOuterControl = true

  var ignoreParEdgeCases: Boolean = false
  var noInnerLoopUnroll: Boolean = false
  var enableBufferCoalescing: Boolean = true

  var enablePIR = false
  var enableTsth = false

  var paramSavePath:Option[String] = None

}
