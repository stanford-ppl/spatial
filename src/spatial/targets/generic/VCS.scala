package spatial.targets
package generic

import models._
import argon._
import forge.tags._

object VCS extends GenericDevice  {
  import GenericDevice._

  val name = "VCS"
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
