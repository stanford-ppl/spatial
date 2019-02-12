package spatial.targets
package generic

import argon._
import forge.tags._
import models._

class GenericAreaModel(target: HardwareTarget) extends AreaModel(target) {
  @stateful override def summarize(area: Area): (Area, String) = {
    (area,"")
  }

}

