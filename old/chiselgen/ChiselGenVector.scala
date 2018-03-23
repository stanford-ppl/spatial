package spatial.codegen.chiselgen

import argon.core._
import argon.nodes._
import spatial.aliases._
import spatial.nodes._


trait ChiselGenVector extends ChiselGenSRAM {

  override protected def name(s: Dyn[_]): String = s match {
    case Def(ListVector(_))            => s"${s}_vecified"
    case Def(VectorApply(_,i))         => s"${s}_elem$i"
    case Def(VectorSlice(_,start,end)) => s"${s}_slice${start}to$end"
    case _ => super.name(s)
  } 

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: VectorType[_] => src"Array[${tp.child}]"
    case _ => super.remap(tp)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case ListVector(elems)      => emit(src"val $lhs = Array($elems)")
    case VectorApply(vector, i) => emitGlobalWireMap(src"""$lhs""", src"""Wire(${newWire(lhs.tp)})"""); emit(src"$lhs := $vector.apply($i)")
    case VectorSlice(vector, start, end) => emit(src"val $lhs = $vector($start, $end)")

    // TODO: Use memcpy for these data <-> bits operations
    /*
      uint32_t dword = 0x4D91A2B4;
      float f;
      memcpy(&f, &dw, 4);
    */
    case e@DataAsBits(a) => e.mT match {
      case FltPtType(_,_)   => throw new Exception("Bit-wise operations not supported on floating point values yet")
      case FixPtType(s,d,f) => emit(src"val $lhs = ${a}.r")
      case BooleanType()    => emit(src"val $lhs = ${a}.r")
    }

    case BitsAsData(v,mT) => mT match {
      case FltPtType(_,_)   => throw new Exception("Bit-wise operations not supported on floating point values yet")
      case FixPtType(s,i,f) => emitGlobalWireMap(src"$lhs",src"Wire(${newWire(lhs.tp)})"); emit(src"${lhs}.r := ${v}.r")
      case BooleanType()    => emit(src"val $lhs = $v // TODO: Need to do something fancy here?")
    }

    case _ => super.emitNode(lhs, rhs)
  }
}