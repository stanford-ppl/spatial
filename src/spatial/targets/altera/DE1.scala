package spatial.targets
package altera

import models._

object DE1 extends AlteraDevice {
  import AlteraDevice._
  val name = "DE1"
  def burstSize = 512

  override def capacity: Area = Area(
    // Fill me in
  )
}
