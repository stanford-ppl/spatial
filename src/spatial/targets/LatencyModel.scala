package spatial.targets

import core._
import models._
import forge.tags._
import spatial.internal.spatialConfig

abstract class LatencyModel extends SpatialModel[LatencyFields] {
  val FILE_NAME: String

  final def DSP_CUTOFF: Int = target.DSP_CUTOFF
  final implicit def RESOURCE_FIELDS: LatencyFields[Double] = target.LATENCY_FIELDS
  final implicit def MODEL_CONFIG: LatencyFields[NodeModel] = target.LMODEL_FIELDS

  @stateful def latencyOf(s: Sym[_], inReduce: Boolean): Double = {
    if (inReduce) latencyInReduce(s) else latencyOfNode(s)
  }

  @stateful def latencyInReduce(s: Sym[_]): Double = model(s, "LatencyInReduce")


  @stateful def requiresRegisters(s: Sym[_], inReduce: Boolean): Boolean = {
     (inReduce && requiresRegistersInReduce(s)) || (!inReduce && requiresRegisters(s))
  }

  @stateful def requiresRegistersInReduce(s: Sym[_]): Boolean = {
    spatialConfig.addRetimeRegisters && model(s, "RequiresInReduce") > 0
  }
  @stateful def requiresRegisters(s: Sym[_]): Boolean = {
    spatialConfig.addRetimeRegisters && model(s, "RequiresRegs") > 0
  }

  @stateful def latencyOfNode(s: Sym[_]): Double = model(s, "LatencyOf")


  /** For some templates, I have to manually put a delay inside the template to break a
    * critical path that retime would not do on its own.
    *
    * For example, FIFODeq has a critical path for certain apps on the deq boolean, so we need to
    * stick a register on this input and then treat the output as having a latency of 2 but needing
    * only 1 injected by transformer
    */
  @stateful def builtInLatencyOfNode(s: Sym[_]): Double = model(s, "BuiltInLatency")

}