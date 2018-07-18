package spatial.metadata.modeling

import argon._
import models._

case class SavedArea(area: Area) extends Data[SavedArea](Transfer.Mirror)

