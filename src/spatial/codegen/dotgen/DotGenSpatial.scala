package spatial.codegen.dotgen

import argon._
import spatial.metadata.memory._
import spatial.lang._
import spatial.node._
import spatial.util.spatialConfig

case class DotGenSpatial(IR: State) extends argon.codegen.DotCodegen {

  override def emitEdge(from:Sym[_], to:Sym[_]) = (from.op, to.op) match {
    case (Some(_:SRAMNew[_,_]), Some(_:SRAMBankedWrite[_,_])) => super.emitEdge(to, from)
    case (Some(_:StreamOutNew[_]), Some(_:StreamOutBankedWrite[_])) => super.emitEdge(to, from)
    case (Some(_:RegNew[_]), Some(_:RegWrite[_])) => super.emitEdge(to, from)
    case (Some(_:DRAMNew[_,_]), Some(_:SetMem[_,_])) => super.emitEdge(to, from)
    case (_, _) => super.emitEdge(from, to)
  }
}
