package spatial.targets
package xilinx

import models._

object KCU1500 extends XilinxDevice {
  import XilinxDevice._
  val name = "KCU1500"
  def burstSize = 512

  override def capacity: Area = Area(
    SLICEL -> 34260,  // Can use any LUT
    SLICEM -> 17600,  // Can only use specialized LUTs
    Slices -> 34260,  // SLICEL + SLICEM
    Regs   -> 548160,
    BRAM   -> 912,    // 1 RAM36 or 2 RAM18s
    DSPs   -> 2520
  )
}
