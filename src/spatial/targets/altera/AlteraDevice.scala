package spatial.targets
package altera

import argon._
import forge.tags._
import models._

abstract class AlteraDevice extends HardwareTarget {
  import AlteraDevice._
  override val AFIELDS: Array[String] = Array(RAM18,RAM36,BRAM,URAM,RAM32X1S,RAM32X1D,RAM32M,RAM64M,RAM64X1S,RAM64X1D,RAM128X1S,RAM128X1D,RAM256X1S,SRLC32E,SRL16E,DSPs,Regs,Mregs,MUX7,MUX8,LUT1,LUT2,LUT3,LUT4,LUT5,LUT6,SLICEL,SLICEM,Slices)
  val DSP_CUTOFF = 16 // TODO[4]: Not sure if this is right

  case object URAM_RESOURCE extends MemoryResource(URAM) {
    @stateful def area(width: Int, depth: Int): Area = uramMemoryModel(width,depth)
    @stateful def summary(area: Area): Double = area(URAM)
    override def minDepth: Int = 512
  }
  case object BRAM_RESOURCE extends MemoryResource(BRAM) {
    @stateful def area(width: Int, depth: Int): Area = bramMemoryModel(width,depth)
    @stateful def summary(area: Area): Double = Math.ceil(area(RAM18) / 2) + area(RAM36)
    override def minDepth: Int = 32
  }
  object URAM_RESOURCE_OVERFLOW extends MemoryResource(URAM) {
    @stateful def area(width: Int, depth: Int): Area = uramMemoryModel(width,depth)
    @stateful def summary(area: Area): Double = area(URAM)
    override def minDepth: Int = 32
  }
  case object LUTs_RESOURCE extends MemoryResource("LUTs") {
    @stateful def area(width: Int, depth: Int): Area = distributedMemoryModel(width,depth)
    @stateful def summary(area: Area): Double = RAMs.map{l => area(l) * RAM_LUT_USAGE(l) }.sum
  }

  override val memoryResources: List[MemoryResource] = List(
    URAM_RESOURCE,
    BRAM_RESOURCE,
    URAM_RESOURCE_OVERFLOW,   // Used if BRAM runs out for medium sized memories
    LUTs_RESOURCE
  )
  val defaultResource: MemoryResource = BRAM_RESOURCE

  /** Returns the depth of a BRAM for a given width */
  def bramWordDepth(width: Int): Int = {
    if      (width == 1) 16384
    else if (width == 2) 8192
    else if (width <= 4) 4096
    else if (width <= 9) 2048
    else if (width <= 18) 1024 // Assume uses RAM18 TDP
    else 512                   // Assume uses RAM36 TDP
  }

  /** Returns the depth of a URAM for a given width */
  def uramWordDepth(width: Int): Int = 4096

  def uramMemoryModel(width: Int, depth: Int): Area = {
    val cols = Math.ceil(width / 72.0)
    val wordDepth = uramWordDepth(width)
    val nRows = Math.ceil(depth.toDouble / wordDepth).toInt
    val totalURAM = cols * nRows
    Area(URAM -> totalURAM)
  }

  @stateful def bramMemoryModel(width: Int, depth: Int): Area = {
    val cols = Math.ceil(width / 18.0)
    val nRAM36Cols = Math.floor(cols / 2).toInt
    val nRAM18Cols = if (cols % 2 != 0) 1 else 0
    val wordDepth = bramWordDepth(width)
    val nRows = Math.ceil(depth.toDouble / wordDepth).toInt
    val totalRAM18 = nRAM18Cols * nRows
    val totalRAM36 = nRAM36Cols * nRows

    log(s"# of RAM18 Cols:  $nRAM18Cols")
    log(s"# of RAM36 Cols:  $nRAM36Cols")
    log(s"# of rows:        $nRows")
    log(s"Elements / Mem:   $wordDepth")
    log(s"Memories / Bank:  $totalRAM18, $totalRAM36")
    Area(RAM18 -> totalRAM18, RAM36 -> totalRAM36)
  }

  def distributedMemoryModel(width: Int, depth: Int): Area = {
    val N = Math.ceil(width.toDouble / 2)
    if (depth <= 32) Area(RAM32M->N)
    else             Area(RAM64M->N) * Math.ceil(depth/64.0)
  }

  protected def makeAreaModel: AreaModel = new AlteraAreaModel(this)
  protected def makeLatencyModel: LatencyModel = new LatencyModel(this)

}
object AlteraDevice {
  // Fill me in correctly please
  val RAM18    = "RAM18"
  val RAM36    = "RAM36"
  val BRAM     = "BRAM"
  val URAM     = "URAM"
  val RAM32X1S = "RAM32X1S"
  val RAM32X1D = "RAM32X1D"
  val RAM32M   = "RAM32M"
  val RAM64M   = "RAM64M"
  val RAM64X1S = "RAM64X1S"
  val RAM64X1D = "RAM64X1D"
  val RAM128X1S = "RAM128X1S"
  val RAM128X1D = "RAM128X1D"
  val RAM256X1S = "RAM256X1S"
  val SRLC32E  = "SRLC32E"
  val SRL16E   = "SRL16E"
  val DSPs     = "DSPs"
  val Regs     = "Regs"
  val Mregs    = "Mregs"
  val MUX7     = "MUX7"
  val MUX8     = "MUX8"
  val LUT1     = "LUT1"
  val LUT2     = "LUT2"
  val LUT3     = "LUT3"
  val LUT4     = "LUT4"
  val LUT5     = "LUT5"
  val LUT6     = "LUT6"
  val SLICEL   = "SLICEL"
  val SLICEM   = "SLICEM"
  val Slices   = "Slices"

  val LUTs = List(LUT1,LUT2,LUT3,LUT4,LUT5,LUT6)
  val RAMs = List(SRL16E, SRLC32E, RAM32X1S,RAM32X1D,RAM32M,RAM64M,RAM64X1S,RAM64X1D,RAM128X1S,RAM128X1D,RAM256X1S)

  val RAM_LUT_USAGE: Map[String,Int] = Map(
    SRL16E -> 1,
    SRLC32E -> 1,
    RAM32X1S -> 1,
    RAM32X1D -> 2,
    RAM32M -> 4,
    RAM64X1S -> 1,
    RAM64X1D -> 2,
    RAM64M -> 4,
    RAM128X1S -> 2,
    RAM128X1D -> 4,
    RAM256X1S -> 4
  )
}
