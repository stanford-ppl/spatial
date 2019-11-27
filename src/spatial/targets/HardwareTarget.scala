package spatial.targets

import models._
import forge.tags.stateful
import spatial.dse._

abstract class HardwareTarget {
  import HardwareTarget._
  
  def host: String = "cpp"   // Target backend for host code, which is either cpp or Rogue
  def name: String    // FPGA name
  def burstSize: Int  // Size of DRAM burst (in bits)

  val AFIELDS: Array[String] // Area resource fields
  val LFIELDS: Array[String] = Array(RequiresRegs, RequiresInReduce, LatencyOf, LatencyInReduce, BuiltInLatency) // Latency resource fields
  val DSP_CUTOFF: Int        // Smallest integer addition (in bits) which uses DSPs
  var clockRate = 150.0f     // Frequency in MHz
  val baseCycles = 43000     // Number of cycles required for startup
  private var __areaModel: Option[AreaModel] = None
  private var __latencyModel: Option[LatencyModel] = None

  lazy implicit val AREA_FIELDS: AreaFields[Double] = AreaFields[Double](AFIELDS, 0.0)
  lazy implicit val LATENCY_FIELDS: LatencyFields[Double] = LatencyFields[Double](LFIELDS, 0.0)

  lazy implicit val AMODEL_FIELDS: AreaFields[NodeModel] = AreaFields[NodeModel](AFIELDS, Right(0.0))
  lazy implicit val LMODEL_FIELDS: LatencyFields[NodeModel] = LatencyFields[NodeModel](LFIELDS, Right(0.0))
  lazy implicit val LINEAR_FIELDS: AreaFields[LinearModel] = AreaFields[LinearModel](AFIELDS, LinearModel(Nil,Set.empty))

  /** Device resource maximum, in terms of FIELDS */
  def capacity: Area

  protected def makeAreaModel(mlModel: AreaEstimator): AreaModel        // Area model for this target
  protected def makeLatencyModel: LatencyModel  // Latency model for this target

  final def areaModel(mlModel: AreaEstimator): AreaModel = {
    if (__areaModel.isEmpty) __areaModel = Some(makeAreaModel(mlModel))
    __areaModel.get
  }
  final def latencyModel: LatencyModel = {
    if (__latencyModel.isEmpty) __latencyModel = Some(makeLatencyModel)
    __latencyModel.get
  }

  val memoryResources: List[MemoryResource]
  val defaultResource: MemoryResource

  @stateful final def areaAnalyzer(mlModel: AreaEstimator): DSEAreaAnalyzer = DSEAreaAnalyzer(state, makeAreaModel(mlModel), makeLatencyModel)
  @stateful final def cycleAnalyzer: LatencyAnalyzer = LatencyAnalyzer(state, makeLatencyModel)

}

object HardwareTarget {
  val RequiresRegs = "RequiresRegs"
  val LatencyOf = "LatencyOf"
  val LatencyInReduce = "LatencyInReduce"
  val RequiresInReduce = "RequiresInReduce"
  val BuiltInLatency = "BuiltInLatency"
}