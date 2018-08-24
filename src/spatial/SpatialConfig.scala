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
  var enableTree: Boolean = true
  var enableDot: Boolean = false
  var enablePythonModel = false

  var enableInstrumentation: Boolean = false
  var enableTightControl: Boolean = false
  var enableDebugResources: Boolean = false
  var useCheapFifos: Boolean = false
  var enableOptimizedReduce: Boolean = true

  var enableSplitting: Boolean = false
  var enableArchDSE: Boolean = false

  var addRetimeRegisters = true // Enable adding registers after specified comb. logic

  var compressWires = 0
  var enableAsyncMem = false
  var enableRetiming = true

  var enableBroadcast = true // Allow broadcasting reads

  // Internal flag used to mark whether unit pipe transformer has been run or not
  var allowPrimitivesInOuterControl = true

  def ignoreParEdgeCases: Boolean = false
  def noInnerLoopUnroll: Boolean = false
  def enableBufferCoalescing: Boolean = true


}
