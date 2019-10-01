package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.bounds.Expect
import spatial.metadata.memory._
import spatial.metadata.control._

trait PIRGenMergeBuffers extends PIRCodegen {

  private def parOf(mem:Sym[_]):Int = {
    val Op(MergeBufferNew(_, Expect(par))) = mem
    par
  }

  def withinCtrl(mem:Sym[_])(block: => Unit) = {
    val lca = LCA(mem.accesses.map { _.parent }).s.get
    emit(src"beginState(${lca}.getCtrl)")
    block
    emit(src"endState[Ctrl]")
  }

  override protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@MergeBufferNew(Expect(ways), Expect(par)) => 
      state(lhs)(src"MergeBuffer($ways, $par).tp(${op.A})")
    case op@MergeBufferBankedEnq(mem,way,data,ens) => 
      state(Lhs(lhs,"inputFIFO")) { src"FIFO().banks(List(${parOf(mem)})).tp(${mem.A})" }
      state(lhs) { 
        src"MemWrite().setMem(${Lhs(lhs,"inputFIFO")}).en(${assertOne(ens)}).data(${assertOne(data)})"
      }
      withinCtrl(mem) {
        state(Lhs(lhs,"inputRead")) {
          src"MemRead().setMem(${Lhs(lhs,"inputFIFO")}).out(${mem}.inputs($way))"
        }
      }
    case op@MergeBufferBankedDeq(mem,ens) => 
      state(Lhs(lhs,"outputFIFO")) { src"FIFO().banks(List(${parOf(mem)})).tp(${mem.A})" }
      withinCtrl(mem) {
        state(Lhs(lhs,"write")) { 
          src"MemWrite().setMem(${Lhs(lhs,"outputFIFO")}).data(${mem}.out)"
        }
      }
      state(lhs) {
        src"MemRead().setMem(${Lhs(lhs,"outputFIFO")}).en(${assertOne(ens)})"
      }
      val lca = LCA(mem.accesses.map { _.parent }).s.get
      emit(src"$mem.ctrl(${lca}.getCtrl, true)")
    case op@MergeBufferBound(mem, way, data, ens) =>
      state(Lhs(lhs,"boundFIFO")) { src"FIFO().banks(List(1)).tp(${Bits[I32]})" }
      state(lhs) { 
        src"MemWrite().setMem(${Lhs(lhs,"boundFIFO")}).en(${assertOne(ens)}).data($data)"
      }
      withinCtrl(mem) {
        state(Lhs(lhs,"boundRead")) {
          src"MemRead().setMem(${Lhs(lhs,"boundFIFO")}).out(${mem}.bounds($way))"
        }
      }
    case op@MergeBufferInit(mem, data, ens) =>
      state(Lhs(lhs,"initFIFO")) { s"FIFO().banks(List(1)).tp(Bool)" }
      state(lhs) { 
        src"MemWrite().setMem(${Lhs(lhs,"initFIFO")}).en(${assertOne(ens)}).data($data)"
      }
      withinCtrl(mem) {
        state(Lhs(lhs,"initRead")) {
          src"MemRead().setMem(${Lhs(lhs,"initFIFO")}).out(${mem}.init)"
        }
      }
    case _ => super.genAccel(lhs, rhs)
  }

}

