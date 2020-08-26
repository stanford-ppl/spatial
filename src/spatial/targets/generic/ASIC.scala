package spatial.targets
package generic

import models._
import argon._
import forge.tags._

object ASIC extends HardwareTarget {
  override val AFIELDS: Array[String] = Array()
  val DSP_CUTOFF = 0
  protected def makeAreaModel(mlModel: AreaEstimator): AreaModel = new GenericAreaModel(this, mlModel)
  protected def makeLatencyModel: LatencyModel = new LatencyModel(xilinx.Zynq)

  case object SRAM_RESOURCE extends MemoryResource("SRAM") {
    @stateful def area(width: Int, depth: Int): Area = Area()
    @stateful def summary(area: Area): Double = 0.0
  }

  val defaultResource: MemoryResource = SRAM_RESOURCE
  val memoryResources: List[MemoryResource] = List(defaultResource)


  val name = "ASIC"
  def burstSize = 512

  override def capacity: Area = Area(
    // Fill me in
  )
}
