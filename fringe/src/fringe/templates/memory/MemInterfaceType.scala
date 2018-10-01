package fringe.templates.memory

import chisel3._
import fringe.utils.HVec
import fringe.utils.DMap._
import fringe.utils.XMap._
import fringe.utils.implicits._

sealed trait MemInterfaceType

sealed abstract class MemInterface(p: MemParams) extends Bundle {
  var xBarW = HVec(Array.tabulate(1 max p.numXBarWPorts){i => Input(new W_XBar(p.xBarWMux.accessPars.getOr1(i), p.ofsWidth, p.banksWidths, p.bitWidth))})
  var xBarR = HVec(Array.tabulate(1 max p.numXBarRPorts){i => Input(new R_XBar(p.xBarRMux.accessPars.getOr1(i), p.ofsWidth, p.banksWidths))})
  var directW = HVec(Array.tabulate(1 max p.numDirectWPorts){i =>
    Input(new W_Direct(p.directWMux.accessPars.getOr1(i), p.ofsWidth, if (p.hasDirectW) p.directWMux.sortByMuxPortAndOfs.values.flatMap(_._1).flatten.toList.grouped(p.banks.length).toList else p.defaultDirect, p.bitWidth))
  })
  var directR = HVec(Array.tabulate(1 max p.numDirectRPorts){i =>
    Input(new R_Direct(p.directRMux.accessPars.getOr1(i), p.ofsWidth, if (p.hasDirectR) p.directRMux.sortByMuxPortAndOfs.values.flatMap(_._1).flatten.toList.grouped(p.banks.length).toList else p.defaultDirect))
  })
  var output = new Bundle {
    var data  = Vec(1 max p.totalOutputs, Output(UInt(p.bitWidth.W)))
  }
  var reset = Input(Bool())
}

class StandardInterface(p: MemParams) extends MemInterface(p) {}
object StandardInterface extends MemInterfaceType


class ShiftRegFileInterface(p: MemParams) extends MemInterface(p) {
  var dump_out = Vec(p.depth, Output(UInt(p.bitWidth.W)))
  var dump_in = Vec(p.depth, Input(UInt(p.bitWidth.W)))
  var dump_en = Input(Bool())
}
object ShiftRegFileInterface extends MemInterfaceType


class FIFOInterface(p: MemParams) extends MemInterface(p) {
  var full = Output(Bool())
  var almostFull = Output(Bool())
  var empty = Output(Bool())
  var almostEmpty = Output(Bool())
  var numel = Output(UInt(32.W))
}
object FIFOInterface extends MemInterfaceType
