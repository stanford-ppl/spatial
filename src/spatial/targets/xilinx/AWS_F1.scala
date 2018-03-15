package spatial.targets
package xilinx

import models._

object AWS_F1 extends XilinxDevice {
  import XilinxDevice._
  val name = "AWS_F1"
  def burstSize = 512


  // TODO
  override def capacity: Area = Area(
    SLICEL -> 2586150,  // Can use any LUT
    SLICEM -> 2586150,  // TODO
    Slices -> 2586150,  // TODO
    Regs   -> 2364480,
    BRAM   -> 2160,     // 1 RAM36 or 2 RAM18s
    URAM   -> 960,
    DSPs   -> 6840
  )
}

