package spatial.lang.api

import spatial.lang.control.NamedClass

trait ControlAPI {

  implicit class SymbolOps(x: Symbol) extends NamedClass(x.name)

}
