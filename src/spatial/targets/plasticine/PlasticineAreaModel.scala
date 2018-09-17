package spatial.targets
package plasticine

import argon._
import forge.tags._
import models._

class PlasticineAreaModel(target: HardwareTarget) extends AreaModel(target) {

  @stateful override def summarize(area: Area): (Area, String) = {
    (area,"")
  }
}

