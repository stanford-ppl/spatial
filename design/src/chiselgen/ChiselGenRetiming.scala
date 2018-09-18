package spatial.codegen.chiselgen

import argon.core._
import argon.nodes._
import spatial.aliases._
import spatial.metadata._
import spatial.nodes._

trait ChiselGenRetiming extends ChiselGenSRAM {

  override protected def name(s: Dyn[_]): String = s match {
    case Def(DelayLine(size, data)) => data match {
      case Const(_) => src"$data"
      case _ => wireMap(s"${quote(data)}_D$size" + alphaconv.getOrElse(s"${quote(data)}_D$size", ""))
    }
    case _ => super.name(s)
  }

  override protected def quote(e: Exp[_]): String = e match {
    case Def(DelayLine(_, data)) if !spatialConfig.enableNaming => data match {case Const(_) => quote(data); case _ => wireMap(super.quote(e) + alphaconv.getOrElse(super.quote(e), ""))}
    case _ => super.quote(e) // All others
  }

  def quoteOperand2(s: Exp[_]): String = s match { // TODO: Unify this with the one in math
    case ss:Sym[_] => s"x${ss.id}"
    case Const(xx:Exp[_]) => s"${boundOf(xx).toInt}" // FIXME: This will never match
    case _ => "unk"
  }


  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case DelayLine(size, data) =>
      // emit(src"""val $lhs = Utils.delay($data, $size)""")
      data match {
        case Const(_) => 
        case _ =>
          alphaconv_register(src"$lhs")
          lhs.tp match {
            case a:VectorType[_] =>
              logRetime(lhs, src"$data", size, true, a.width, newWire(lhs.tp), false/*although what about vec of bools?*/)
            case BooleanType() =>
              logRetime(lhs, src"$data", size, false, 0, newWire(lhs.tp), true)
            case _ =>
              logRetime(lhs, src"$data", size, false, 0, newWire(lhs.tp), false)
          }
      }

    /*case ShiftRegNew(size, init) =>
      emitGlobalRetiming(src"val $lhs = Module(new Retimer($size, ${bitWidth(lhs.tp.typeArguments.head)}))")
      init match {
        case Def(ListVector(_)) => 
          emitGlobalRetiming(src"// ${lhs} init is ${init}, must emit in ${controllerStack.head}")
          emit(src"${lhs}.io.input.init := ${init}.r")
        case _ => 
          emitGlobalRetiming(src"${lhs}.io.input.init := ${init}.raw")
      }

    case ShiftRegRead(shiftReg) => 
      emit(src"val $lhs = Wire(${newWire(lhs.tp)})")
      lhs.tp match {
        case a:VectorType[_] =>
          emit(src"(0 until ${a.width}).foreach{i => ${lhs}(i).raw := ${shiftReg}.io.output.data(${bitWidth(lhs.tp)/a.width}*(i+1)-1, ${bitWidth(lhs.tp)/a.width}*i)}")
        case _ =>
          emit(src"$lhs.raw := ${shiftReg}.io.output.data")
      }
      

    case ShiftRegWrite(shiftReg, data, en) => 
      val parent = parentOf(lhs).get
      emit(src"${shiftReg}.io.input.data := ${data}.raw")
      emit(src"${shiftReg}.io.input.en := true.B //$en & ${parent}_datapath_en")*/

    case _ =>
      super.emitNode(lhs, rhs)
  }

  override protected def emitFileFooter() {
    emitGlobalWire(s"val max_retime = $maxretime")
    super.emitFileFooter()
  }

}