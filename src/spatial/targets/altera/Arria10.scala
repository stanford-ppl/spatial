package spatial.targets
package altera

import models._

object Arria10 extends AlteraDevice {
  import AlteraDevice._
  val name = "Arria10"
  def burstSize = 512

  override def capacity: Area = Area(
    // Fill me in
  )
}
