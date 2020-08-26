package spatial.targets
package plasticine

import argon._
import forge.tags._
import models._

object Plasticine extends HardwareTarget {
  def name = "Plasticine"
  def burstSize = 512 // bit
  override val AFIELDS: Array[String] = Array()
  val DSP_CUTOFF = 0
  clockRate = 1000.0f     // Frequency in MHz

  override def capacity: Area = Area()

  protected def makeAreaModel(mlModel: AreaEstimator): AreaModel = new PlasticineAreaModel(this, mlModel)
  protected def makeLatencyModel: LatencyModel = new LatencyModel(this)

  case object SRAM_RESOURCE extends MemoryResource("SRAM") {
    @stateful def area(width: Int, depth: Int): Area = Area()
    @stateful def summary(area: Area): Double = 0.0
  }

  val defaultResource: MemoryResource = SRAM_RESOURCE
  val memoryResources: List[MemoryResource] = List(defaultResource)

}
