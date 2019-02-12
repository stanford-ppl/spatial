package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._

trait ScalaGenStream extends ScalaGenMemories with ScalaGenControl {
  var streamIns: Set[Sym[_]] = Set.empty
  var streamOuts: Set[Sym[_]] = Set.empty
  var bufferedOuts: Set[Sym[_]] = Set.empty

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: StreamIn[_]  => src"scala.collection.mutable.Queue[${tp.A}]"
    case tp: StreamOut[_] => src"scala.collection.mutable.Queue[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def emitControlDone(ctrl: Sym[_]): Unit = {
    super.emitControlDone(ctrl)
  }

  // HACK
  def bitsFromString(lhs: String, line: String, tp: ExpType[_,_]): Unit = tp match {
    case FixPtType(s,i,f) => emit(s"val $lhs = FixedPoint($line, FixFormat($s,$i,$f))")
    case FltPtType(g,e)   => emit(s"val $lhs = FloatPoint($line, FltFormat(${g-1},$e))")
    case _:Bit            => emit(s"val $lhs = Bool($line.toBoolean, true)")
    case tp: Vec[_] =>
      open(s"""val $lhs = $line.split(",").map(_.trim).map{elem => """)
        bitsFromString("out", "elem", tp.A)
        emit("out")
      close("}.toArray")
    case tp: Struct[_] =>
      emit(s"""val ${lhs}_tokens = $line.split(";").map(_.trim)""")
      tp.fields.zipWithIndex.foreach{case (field,i) =>
        bitsFromString(s"${lhs}_field$i", s"${lhs}_tokens($i)", field._2)
      }
      emit(src"val $lhs = $tp(" + List.tabulate(tp.fields.length){i => s"${lhs}_field$i"}.mkString(", ") + ")")

    case _ => throw new Exception(s"Cannot create Stream with type $tp")
  }

  def bitsToString(lhs: String, elem: String, tp: ExpType[_,_]): Unit = tp match {
    case FixPtType(s,i,f) => emit(s"val $lhs = $elem.toString")
    case FltPtType(g,e)   => emit(s"val $lhs = $elem.toString")
    case _: Bit           => emit(s"val $lhs = $elem.toString")
    case tp: Vec[_] =>
      open(s"""val $lhs = $elem.map{elem => """)
        bitsToString("out", "elem", tp.A)
        emit("out")
      close("""}.mkString(", ")""")
    case tp: Struct[_] =>
      tp.fields.zipWithIndex.foreach{case (field,i) =>
        emit(s"val ${elem}_field$i = $elem.${field._1}")
        bitsToString(s"${elem}_fieldStr$i", s"${elem}_field$i", field._2)
      }
      emit(s"val $lhs = List(" + List.tabulate(tp.fields.length){i => s"${elem}_fieldStr$i"}.mkString(", ") + s""").mkString("; ")""")
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@StreamInNew(bus)  =>
      val name = lhs.name.map(_ + " (" + lhs.ctx + ")").getOrElse("defined at " + lhs.ctx)
      streamIns += lhs

      emitMemObject(lhs){
        open(src"""object $lhs extends StreamIn[${op.A}]("$name", {str => """)
        bitsFromString("x", "str", op.A)
        emit(src"x")
        close(src"})")
      }
      if (!bus.isInstanceOf[DRAMBus[_]]) emit(src"$lhs.initMem()")

    case op@StreamOutNew(bus) =>
      val name = lhs.name.map(_ + " (" +lhs.ctx + ")").getOrElse("defined at " + lhs.ctx)
      streamOuts += lhs

      emitMemObject(lhs){
        open(src"""object $lhs extends StreamOut[${op.A}]("$name", {elem => """)
        bitsToString("x", "elem", op.A)
        emit(src"x")
        close("})")
      }
      if (!bus.isInstanceOf[DRAMBus[_]]) emit(src"$lhs.initMem()")

    case op@StreamInBankedRead(strm, ens) =>
      open(src"val $lhs = {")
      ens.zipWithIndex.foreach{case (en,i) =>
        emit(src"val a$i = if (${and(en)} && $strm.nonEmpty) $strm.dequeue() else ${invalid(op.A)}")
      }
      emit(src"Array[${op.A}](" + ens.indices.map{i => src"a$i"}.mkString(", ") + ")")
      close("}")

    case StreamOutBankedWrite(strm, data, ens) =>
      open(src"val $lhs = {")
      ens.zipWithIndex.foreach{case (en,i) =>
        emit(src"if (${and(en)}) $strm.enqueue(${data(i)})")
      }
      close("}")

    case _ => super.gen(lhs, rhs)
  }

}
