package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._

trait ScalaGenStream extends ScalaGenMemories with ScalaGenControl {
  var streamIns: List[Sym[_]] = Nil
  var streamOuts: List[Sym[_]] = Nil
  var bufferedOuts: List[Sym[_]] = Nil

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: StreamIn[_]  => src"scala.collection.mutable.Queue[${tp.A}]"
    case tp: StreamOut[_] => src"scala.collection.mutable.Queue[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def emitControlDone(ctrl: Sym[_]): Unit = {
    super.emitControlDone(ctrl)

    /*val written = localMems.filter{mem => writersOf(mem).exists{wr => topControllerOf(wr.node,mem,0).exists(_.node == ctrl) } }
    val bufferedOuts = written.filter(isBufferedOut)
    if (bufferedOuts.nonEmpty) {
      emit("/** Dump BufferedOuts **/")
      bufferedOuts.foreach{buff => emit(src"dump_$buff()") }
      emit("/***********************/")
    }*/
  }

  // HACK
  def bitsFromString(lhs: String, line: String, tp: ExpType[_,_]): Unit = tp match {
    case FixPtType(s,i,f) => emit(s"val $lhs = FixedPoint($line, FixFormat($s,$i,$f))")
    case FltPtType(g,e)   => emit(s"val $lhs = FixedPoint($line, FltFormat(${g-1},$e))")
    case _:Bit            => emit(s"val $lhs = Bool($line.toBoolean, true)")
    case tp: Vec[_] =>
      open(s"""val $lhs = $line.split(",").map(_.trim).map{elem => """)
        bitsFromString("out", "elem", tp.A)
        emit("out")
      close("}.toArray")
    case tp: Struct[_] =>
      emit(s"""val tokens = $line.split(";").map(_.trim)""")
      tp.fields.zipWithIndex.foreach{case (field,i) =>
        bitsFromString(s"field$i", s"tokens($i)", field._2)
      }
      emit(src"val $lhs = $tp(" + List.tabulate(tp.fields.length){i => s"field$i"}.mkString(", ") + ")")

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
        emit(s"val field$i = $elem.${field._1}")
        bitsToString(s"fieldStr$i", s"field$i", field._2)
      }
      emit(s"val $lhs = List(" + List.tabulate(tp.fields.length){i => s"fieldStr$i"}.mkString(", ") + s""").mkString("; ")""")
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@StreamInNew(bus)  =>
      streamIns :+= lhs
      emitMem(lhs, src"$lhs = new scala.collection.mutable.Queue[${op.A}]")

      if (!bus.isInstanceOf[DRAMBus[_]]) {
        val name = lhs.name.map(_ + " (" +lhs.ctx + ")").getOrElse("defined at " + lhs.ctx)
        open(src"def populate_$lhs() = {")
          emit(src"""print("Enter name of file to use for StreamIn $name: ")""")
          emit(src"val filename = Console.readLine()")
          open(src"try {")
            emit(src"val source = scala.io.Source.fromFile(filename)")
            open(src"source.getLines.foreach{line => ")
              open(src"if (line.exists(_.isDigit)) {")
                bitsFromString("elem", "line", op.A)
                emit(src"$lhs.enqueue(elem)")
              close("}")
            close("}")
          close("}")
          open(src"catch {case e: Throwable => ")
            emit(src"""println("There was a problem while opening the specified file for reading.")""")
            emit(src"""println(e.getMessage)""")
            emit(src"""e.printStackTrace()""")
            emit(src"sys.exit(1)")
          close("}")
        close("}")
        emit(src"populate_$lhs()")
      }

    case op@StreamOutNew(bus) =>
      streamOuts :+= lhs
      emitMem(lhs, src"$lhs = new scala.collection.mutable.Queue[${op.A}]")

      if (!bus.isInstanceOf[DRAMBus[_]]) {
        val name = lhs.name.map(_ + " (" +lhs.ctx + ")").getOrElse("defined at " + lhs.ctx)

        emit(src"""print("Enter name of file to use for StreamOut $name: ")""")
        emit(src"var ${lhs}_writer: java.io.PrintWriter = null")
        open(src"try {")
          emit(src"val filename = Console.readLine()")
          emit(src"${lhs}_writer = new java.io.PrintWriter(new java.io.File(filename))")
        close("}")
        open("catch{ case e: Throwable => ")
          emit(src"""println("There was a problem while opening the specified file for writing.")""")
          emit(src"""println(e.getMessage)""")
          emit(src"""e.printStackTrace()""")
          emit(src"sys.exit(1)")
        close("}")

        open(src"def print_$lhs(): Unit = {")
          open(src"$lhs.foreach{elem => ")
            bitsToString("line", "elem", op.A)
            emit(src"${lhs}_writer.println(line)")
          close("}")
          emit(src"${lhs}_writer.close()")
        close("}")
      }

    case op@StreamInBankedRead(strm, enss) =>
      open(src"val $lhs = {")
      enss.zipWithIndex.foreach{case (en,i) =>
        emit(src"val a$i = if ($en && $strm.nonEmpty) $strm.dequeue() else ${invalid(op.A)}")
      }
      emit(src"Array[${op.A}](" + enss.indices.map{i => src"a$i"}.mkString(", ") + ")")
      close("}")

    case StreamOutBankedWrite(strm, data, enss) =>
      open(src"val $lhs = {")
      enss.zipWithIndex.foreach{case (en,i) =>
        emit(src"if ($en) $strm.enqueue(${data(i)})")
      }
      close("}")

    case _ => super.gen(lhs, rhs)
  }

}
