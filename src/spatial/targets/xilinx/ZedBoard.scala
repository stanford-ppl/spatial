package spatial.targets
package xilinx

import models._

object ZedBoard extends XilinxDevice {
  import XilinxDevice._
  val name = "ZedBoard"
  def burstSize = 512

  override def capacity: Area = Area(
    SLICEL -> 54650,  // Can use any LUT
    SLICEM -> 17600,  // Can only use specialized LUTs
    Slices -> 54650,  // SLICEL + SLICEM
    Regs   -> 437200,
    BRAM   -> 545,    // 1 RAM36 or 2 RAM18s
    DSPs   -> 900
  )
}
