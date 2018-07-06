package spatial.codegen.chiselgen

import argon._
import spatial.lang._
import spatial.node._

trait ChiselGenDelay extends ChiselGenCommon {

  // var outMuxMap: Map[Sym[Reg[_]], Int] = Map()
  private var nbufs: List[(Sym[Reg[_]], Int)]  = List()

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {

    case DelayLine(delay, data) =>
      if (delay > maxretime) maxretime = delay
      // emit(src"""val $lhs = Utils.delay($data, $size)""")

      data.rhs match {
        case Def.Const(_) => 
        case Def.Param(_,_) => 
        case _ =>
          alphaconv_register(src"$lhs")
          emitGlobalWireMap(src"$lhs", src"Wire(${lhs.tp})")
          lhs.tp match {
            case a:Vec[_] => emit(src"(0 until ${a.width}).foreach{i => ${lhs}(i).r := ${DL(src"${data}(i).r", delay)}}")
            case _ =>        emit(src"""${lhs}.r := ${DL(src"${data}.r", delay, false)}""")
          }
      }

	case _ => super.gen(lhs, rhs)
  }


}