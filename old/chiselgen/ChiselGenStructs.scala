package spatial.codegen.chiselgen

import argon.core._
import argon.nodes._
import spatial.aliases._
import spatial.nodes._


trait ChiselGenStructs extends ChiselGenSRAM {

  override protected def spatialNeedsFPType(tp: Type[_]): Boolean = tp match { // FIXME: Why doesn't overriding needsFPType work here?!?!
    case FixPtType(s,d,f) => if (s) true else if (f == 0) false else true
    case IntType()  => false
    case LongType() => false
    case HalfType() => true
    case FloatType() => true
    case DoubleType() => true
    case _ => super.needsFPType(tp)
  }

  protected def tupCoordinates(tp: Type[_],field: String): (Int,Int) = tp match {
    case x: Tuple2Type[_,_] => field match {
      // A little convoluted because we .reverse simplestructs
      case "_1" => 
        val s = 0
        val width = bitWidth(x.m1)
        (s+width-1, s)
      case "_2" => 
        val s = bitWidth(x.m1)
        val width = bitWidth(x.m2)
        (s+width-1, s)
      }
    case x: StructType[_] =>
      val idx = x.fields.indexWhere(_._1 == field)
      val width = bitWidth(x.fields(idx)._2)
      val prec = x.fields.take(idx)
      val precBits = prec.map{case (_,bt) => bitWidth(bt)}.sum
      (precBits+width-1, precBits)
  }

  override protected def bitWidth(tp: Type[_]): Int = tp match {
      case e: Tuple2Type[_,_]  => super.bitWidth(e.typeArguments(0)) + super.bitWidth(e.typeArguments(1))
      case _ => super.bitWidth(tp)
  }


  override protected def name(s: Dyn[_]): String = s match {
    case Def(_: SimpleStruct[_]) => s"${s}_tuple"
    case Def(_: FieldApply[_,_]) => s"${s}_apply"
    case _ => super.name(s)
  } 

  override protected def remap(tp: Type[_]): String = tp match {
    // case tp: DRAMType[_] => src"Array[${tp.child}]"
    case _ => super.remap(tp)
  }

  override protected def quoteConst(c: Const[_]): String = (c.tp, c) match {
    case (st: StructType[_], e@Const(elems)) =>
      val tuples = elems.asInstanceOf[Seq[(_, Exp[_])]]
      val rand_string = (0 until 5).map{_ => scala.util.Random.alphanumeric.filter(_.isLetter).head}.mkString("") // Random letter since quoteConst has no lhs handle
      val items = tuples.zipWithIndex.map{ case(t,i) => 
        val width = bitWidth(t._2.tp)
        emitGlobalWireMap(src"${rand_string}_item${i}",src"Wire(UInt(${width}.W))")
        if (width > 1 & !spatialNeedsFPType(t._2.tp)) { emit(src"${swap(src"${rand_string}_item${i}", Blank)} := ${t._2}(${width-1},0)") } else {emit(src"${swap(src"${rand_string}_item${i}", Blank)} := ${t._2}.r")} // FIXME: This is a hacky way to fix chisel/verilog auto-upcasting from multiplies
        src"${swap(src"${rand_string}_item${i}", Blank)}"
      }.reverse.mkString(",")
      val totalWidth = tuples.map{ t => 
          bitWidth(t._2.tp)  
      }.reduce{_+_}
      src"chisel3.util.Cat($items)"

    case _ => super.quoteConst(c)
  }
  

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case SimpleStruct(tuples)  =>
      val items = tuples.zipWithIndex.map{ case(t,i) => 
        val width = bitWidth(t._2.tp)
        emitGlobalWireMap(src"${lhs}_item${i}",src"Wire(UInt(${width}.W))")
        if (width > 1 & !spatialNeedsFPType(t._2.tp)) { emit(src"${swap(src"${lhs}_item${i}", Blank)} := ${t._2}(${width-1},0)") } else {emit(src"${swap(src"${lhs}_item${i}", Blank)} := ${t._2}.r")} // FIXME: This is a hacky way to fix chisel/verilog auto-upcasting from multiplies
        src"${swap(src"${lhs}_item${i}", Blank)}"
      }.reverse.mkString(",")
      val totalWidth = tuples.map{ t => 
          bitWidth(t._2.tp)  
      }.reduce{_+_}
      emitGlobalWireMap(src"$lhs", src"Wire(UInt(${totalWidth}.W))")
      emit(src"$lhs := chisel3.util.Cat($items)")
    case VectorConcat(items) =>
      val items_string = items.map{a => src"${a}.r"}.mkString(",")
      emit(src"val $lhs = chisel3.util.Cat(${items_string})")
    case FieldApply(struct, field) =>
      val (msb, lsb) = tupCoordinates(struct.tp, field)      
      if (spatialNeedsFPType(lhs.tp)) {
        lhs.tp match {
          case FixPtType(s,d,f) => 
            emitGlobalWireMap(src"""${lhs}""", src"""Wire(${newWire(lhs.tp)})""")
            emit(src"""${lhs}.r := ${struct}($msb, $lsb)""")
          case _ => emit(src"val $lhs = ${struct}($msb, $lsb)")
        }
      } else {
        emit(src"val $lhs = ${struct}($msb, $lsb)")
      }

    case _ => super.gen(lhs, rhs)
  }
}
