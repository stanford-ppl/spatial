package spatial.targets

import forge.tags._
import models._

abstract class MemoryResource(val name: String) {
  @stateful def area(width: Int, depth: Int): Area
  @stateful def summary(area: Area): Double
  def minDepth: Int = 0
}

