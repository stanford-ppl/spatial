package spatial.codegen.chiselgen

import argon._
import argon.node._
import spatial.lang.Vec
import spatial.node._

trait ChiselGenVec extends ChiselGenCommon {

  override def remap(tp: Type[_]): String = tp match {
    case _: Vec[_] => s"UInt(${bitWidth(tp)}.W)"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    // Treat them as just wires of UInt anyways since chisel doesn't allow assigning to Vecs.
    case VecAlloc(elems) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))
      emit(src"$lhs.r := VecInit(Seq($elems)).asUInt")

    case VecSlice(vec, start, stop) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))
      val elemSize = vec.A.nbits
      val startOffset = start * elemSize
      // Chisel UInt slice is inclusive.
      val stopOffset = stop * elemSize - 1
      emit(src"$lhs.r := $vec($stopOffset, $startOffset)")
//      emit(src"$lhs.zipWithIndex.foreach{case(w, i) => w := $vec(i+$stop)}")

    case VecConcat(list) => 
      emit(createWire(quote(lhs),remap(lhs.tp)))
      val raws = list.map{x => src"$x"}.mkString(" ## ")
      emit(src"$lhs.r := ($raws)")
//      emit(src"${lhs}.zip($raws).foreach{case (a,b) => a := b}")

    case VecApply(vec, id) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))
      val elemSize = vec.A.nbits
      val startOffset = elemSize * id
      val endOffset = elemSize * (id + 1) - 1
      emit(src"$lhs.r := $vec($endOffset, $startOffset)")

    case ShuffleCompressVec(in) =>
      // TODO: Figure out what to do with this
      emit(src"val $lhs = Wire(${lhs.tp})")
      val (datamsb, datalsb) = getField(in.head.tp, "_1")
      val (maskmsb, masklsb) = getField(in.head.tp, "_2")
      val data = in.map{ quote(_) + s".r($datamsb, $datalsb)" }.mkString(src"List[UInt](", ",", ")")
      val mask = in.map{ quote(_) + s".r($maskmsb)" }.mkString(src"List[Bool](", ",", ")")
      emit(src"val (${lhs}_data, ${lhs}_mask) = Shuffle.compress(Vec($data), Vec($mask))")
      emit(src"${lhs}.zipWithIndex.foreach { case (a, i) => a.r := Cat(${lhs}_mask(i).r, ${lhs}_data(i).r) }")

    case BitsPopcount(data) =>
      emit(createWire(quote(lhs),remap(lhs.tp)))    
      emit(src"$lhs.r := PopCount($data.asBools)")


    case _ => super.gen(lhs, rhs)
  }

}
