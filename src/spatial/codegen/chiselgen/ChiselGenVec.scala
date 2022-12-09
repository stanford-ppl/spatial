package spatial.codegen.chiselgen

import argon._
import argon.node._
import spatial.node._

trait ChiselGenVec extends ChiselGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case VecAlloc(elems) => emit(src"val $lhs = VecInit($elems)")

    case VecSlice(vec, start, stop) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.zipWithIndex.foreach{case(w, i) => w := $vec(i+$stop)}")

    case VecConcat(list) => 
      emit(createWire(quote(lhs),remap(lhs.tp)))
      val raws = list.map{x => src"$x"}.mkString(" ++ ")
      emit(src"${lhs}.zip($raws).foreach{case (a,b) => a.r := b.r}")

    case VecApply(vec, id) =>
      emit(s"// $lhs = $rhs")
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := $vec($id).r")

    case ShuffleCompressVec(in) =>
      emit(src"val $lhs = Wire(${lhs.tp})")
      val (datamsb, datalsb) = getField(in.head.tp, "_1")
      val (maskmsb, masklsb) = getField(in.head.tp, "_2")
      val data = in.map{ quote(_) + s".r($datamsb, $datalsb)" }.mkString(src"List[UInt](", ",", ")")
      val mask = in.map{ quote(_) + s".r($maskmsb)" }.mkString(src"List[Bool](", ",", ")")
      emit(src"val (${lhs}_data, ${lhs}_mask) = Shuffle.compress(Vec($data), Vec($mask))")
      emit(src"${lhs}.zipWithIndex.foreach { case (a, i) => a.r := Cat(${lhs}_mask(i).r, ${lhs}_data(i).r) }")

    case BitsPopcount(data) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))    
      emit(src"$lhs.r := PopCount(Seq($data))") 


    case _ => super.gen(lhs, rhs)
  }

}
