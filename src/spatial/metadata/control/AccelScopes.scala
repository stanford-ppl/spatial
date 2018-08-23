package spatial.metadata.control

import argon._
import forge.tags._

/** A list of all Accel scopes in the program
  *
  * Getter: AccelScopes.all
  * Default: Nil
  */
case class AccelScopes(scopes: Seq[Ctrl.Node]) extends Data[AccelScopes](GlobalData.Flow)
@data object AccelScopes {
  def all: Seq[Ctrl.Node] = globals[AccelScopes].map(_.scopes).getOrElse(Nil)
}

