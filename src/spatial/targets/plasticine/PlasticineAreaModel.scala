package spatial.targets
package plasticine

import argon._
import forge.tags._
import models._

class PlasticineAreaModel(target: HardwareTarget, mlModel: AreaEstimator) extends AreaModel(target, mlModel) {

  @stateful override def summarize(area: Area): (Area, String) = {
    (area,"")
  }
}

