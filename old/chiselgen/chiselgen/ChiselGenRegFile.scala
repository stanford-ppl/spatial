package spatial.codegen.chiselgen

import argon.core._
import argon.nodes._
import spatial.aliases._
import spatial.metadata._
import spatial.nodes._
import spatial.utils._


trait ChiselGenRegFile extends ChiselGenSRAM {
  private var nbufs: List[(Sym[SRAM[_]], Int)]  = List()

  override protected def name(s: Dyn[_]): String = s match {
    case Def(_: RegFileNew[_,_]) => s"""${s}_${s.name.getOrElse("regfile")}"""
    case Def(_: LUTNew[_,_])     => s"""${s}_${s.name.getOrElse("lut")}"""
    case _ => super.name(s)
  } 

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: RegFileType[_] => src"Array[${tp.child}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@RegFileNew(dims, inits) =>
      val initVals = if (inits.isDefined) {
        getConstValues(inits.get).toList.map{a => src"${a}d"}.mkString(",")
      } else { "None"}
      
      val initString = if (inits.isDefined) src"Some(List(${initVals}))" else "None"
      val f = lhs.tp.typeArguments.head match {
        case a: FixPtType[_,_,_] => a.fracBits
        case _ => 0
      }
      val width = bitWidth(lhs.tp.typeArguments.head)
      duplicatesOf(lhs).zipWithIndex.foreach{ case (mem, i) => 
        val writerInfo = writersOf(lhs).toSeq.map{w =>
          val port = portsOf(w, lhs, i).head
          w.node match {
            case Def(_:RegFileStore[_]) => (port, 1, 1)
            case Def(_:RegFileShiftIn[_]) => (port, 1, 1)
            case Def(_@ParRegFileShiftIn(_,inds,d,data,en)) => (port, inds.length, data.tp.asInstanceOf[VectorType[_]].width) // Get stride
            case Def(_@ParRegFileStore(_,_,_,en)) => (port, en.length, 1)
          }
        }
        if (writerInfo.isEmpty) {warn(s"RegFile $lhs has no writers!")}
        val parInfo = writerInfo.groupBy(_._1).map{case (k,v) => src"($k -> ${v.map{_._2}.sum})"}
        val stride = if (writerInfo.isEmpty) 1 else writerInfo.map(_._3).max
        val depth = mem match {
          case BankedMemory(dims, d, isAccum) => d
          case _ => 1
        }
        if (depth == 1) {
          emitGlobalModule(src"""val ${lhs}_$i = Module(new templates.ShiftRegFile(List(${getConstValues(dims)}), $initString, $stride, ${if (writerInfo.length == 0) 1 else writerInfo.map{_._2}.reduce{_+_}}, false, $width, $f))""")
          emitGlobalModule(src"${lhs}_$i.io.dump_en := false.B")
        } else {
          appPropertyStats += HasNBufRegFile
          nbufs = nbufs :+ (lhs.asInstanceOf[Sym[SRAM[_]]], i)
          emitGlobalModule(src"""val ${lhs}_$i = Module(new NBufShiftRegFile(List(${getConstValues(dims)}), $initString, $stride, $depth, Map(${parInfo.mkString(",")}), $width, $f))""")
        }
        resettersOf(lhs).indices.foreach{ ii => emitGlobalWire(src"""val ${lhs}_${i}_manual_reset_$ii = Wire(Bool())""")}
        if (resettersOf(lhs).nonEmpty) {
          emitGlobalModule(src"""val ${lhs}_${i}_manual_reset = ${resettersOf(lhs).indices.map{ii => src"${lhs}_${i}_manual_reset_$ii"}.mkString(" | ")}""")
          emitGlobalModule(src"""${lhs}_$i.io.reset := ${lhs}_${i}_manual_reset | accelReset""")
        } else {emitGlobalModule(src"${lhs}_$i.io.reset := accelReset")}

      }

    case RegFileReset(rf,en) => 
      val parent = parentOf(lhs).get
      val id = resettersOf(rf).map{_._1}.indexOf(lhs)
      duplicatesOf(rf).indices.foreach{i => emit(src"${rf}_${i}_manual_reset_$id := $en & ${DL(swap(parent, DatapathEn), enableRetimeMatch(en, lhs), true)} ")}
      
    case op@RegFileLoad(rf,inds,en) =>
      val dispatch = dispatchOf(lhs, rf).toList.head
      val port = portsOf(lhs, rf, dispatch).toList.head
      val addr = inds.map{i => src"${i}.r"}.mkString(",")
      emitGlobalWireMap(src"""${lhs}""",src"""Wire(${newWire(lhs.tp)})""")
      emit(src"""${lhs}.r := ${rf}_${dispatch}.readValue(List($addr), $port)""")

    case op@RegFileStore(rf,inds,data,en) =>
      val width = bitWidth(rf.tp.typeArguments.head)
      val parent = writersOf(rf).find{_.node == lhs}.get.ctrlNode
      val enable = src"""${swap(parent, DatapathEn)} & ~${swap(parent, Inhibitor)} & ${swap(parent, IIDone)}"""
      emit(s"""// Assemble multidimW vector""")
      emit(src"""val ${lhs}_wVec = Wire(Vec(1, new multidimRegW(${inds.length}, List(${constDimsOf(rf)}), ${width}))) """)
      emit(src"""${lhs}_wVec(0).data := ${data}.r""")
      emit(src"""${lhs}_wVec(0).en := ${en} & ${DL(enable, enableRetimeMatch(en, lhs), true)}""")
      inds.zipWithIndex.foreach{ case(ind,j) => 
        emit(src"""${lhs}_wVec(0).addr($j) := ${ind}.r // Assume always an int""")
      }
      emit(src"""${lhs}_wVec(0).shiftEn := false.B""")
      duplicatesOf(rf).zipWithIndex.foreach{ case (mem, i) =>
        emit(src"""${rf}_$i.connectWPort(${lhs}_wVec, List(${portsOf(lhs, rf, i)})) """)
      }

    case ParRegFileLoad(rf, inds, ens) => //FIXME: Not correct for more than par=1
      val dispatch = dispatchOf(lhs, rf).toList.head
      val port = portsOf(lhs, rf, dispatch).toList.head
      emitGlobalWire(s"""val ${quote(lhs)} = Wire(Vec(${ens.length}, ${newWire(lhs.tp.typeArguments.head)}))""")
      ens.zipWithIndex.foreach { case (en, i) =>
        val addr = inds(i).map{id => src"${id}.r"}.mkString(",") 
        emit(src"""val ${lhs}_$i = Wire(${newWire(lhs.tp.typeArguments.head)})""")
        emit(src"""${lhs}(${i}).r := ${rf}_${dispatch}.readValue(List(${addr}), $port)""")
      }
      // emit(s"""${quote(lhs)} := Vec(${(0 until ens.length).map{i => src"${lhs}_$i"}.mkString(",")})""")


    case ParRegFileStore(rf, inds, data, ens) => //FIXME: Not correct for more than par=1
      val width = bitWidth(rf.tp.typeArguments.head)
      val parent = writersOf(rf).find{_.node == lhs}.get.ctrlNode
      val enable = src"""${swap(parent, DatapathEn)} & ~${swap(parent, Inhibitor)} && ${swap(parent, IIDone)}"""
      emit(s"""// Assemble multidimW vector""")
      emitGlobalWireMap(src"""${lhs}_wVec""", src"""Wire(Vec(${ens.length}, new multidimRegW(${inds.head.length}, List(${constDimsOf(rf)}), ${width})))""")
      ens.indices.foreach{ k =>
        emit(src"""${swap(lhs, WVec)}($k).data := ${data(k)}.r""")
        emit(src"""${swap(lhs, WVec)}($k).en := ${ens(k)} & ${DL(enable, enableRetimeMatch(ens.head, lhs), true)}""")
        inds(k).zipWithIndex.foreach{ case(ind,j) => 
          emit(src"""${swap(lhs, WVec)}($k).addr($j) := ${ind}.r // Assume always an int""")
        }
        emit(src"""${swap(lhs, WVec)}($k).shiftEn := false.B""")
      }
      duplicatesOf(rf).zipWithIndex.foreach{ case (mem, i) => 
        val p = portsOf(lhs, rf, i).mkString(",")
        emit(src"""${rf}_$i.connectWPort(${swap(lhs, WVec)}, List(${p})) """)
      }

    case RegFileShiftIn(rf,inds,d,data,en)    => 
      val width = bitWidth(rf.tp.typeArguments.head)
      val parent = writersOf(rf).find{_.node == lhs}.get.ctrlNode
      val enable = src"""${swap(parent, DatapathEn)} & ~${swap(parent, Inhibitor)}"""
      emit(s"""// Assemble multidimW vector""")
      emit(src"""val ${lhs}_wVec = Wire(Vec(1, new multidimRegW(${inds.length}, List(${constDimsOf(rf)}), ${width}))) """)
      emit(src"""${lhs}_wVec(0).data := ${data}.r""")
      emit(src"""${lhs}_wVec(0).shiftEn := ${en} & ${DL(enable, enableRetimeMatch(en, lhs), true)}""")
      inds.zipWithIndex.foreach{ case(ind,j) => 
        emit(src"""${lhs}_wVec(0).addr($j) := ${ind}.r // Assume always an int""")
      }
      emit(src"""${lhs}_wVec(0).en := false.B""")
      duplicatesOf(rf).zipWithIndex.foreach{ case (mem, i) =>
        emit(src"""${rf}_$i.connectShiftPort(${lhs}_wVec, List(${portsOf(lhs, rf, i)})) """)
      }

    case ParRegFileShiftIn(rf,inds,d,data,en) => 
      val width = bitWidth(rf.tp.typeArguments.head)
      val parent = writersOf(rf).find{_.node == lhs}.get.ctrlNode
      val enable = src"""${swap(parent, DatapathEn)} & ~${swap(parent, Inhibitor)}"""
      emit(s"""// Assemble multidimW vectors""")
      emit(src"""val ${lhs}_wVec = Wire(Vec(${inds.length}, new multidimRegW(${inds.length}, List(${constDimsOf(rf)}), ${width}))) """)
      open(src"""for (${lhs}_i <- 0 until ${data}.length) {""")
        emit(src"""${lhs}_wVec(${lhs}_i).data := ${data}(${lhs}_i).r""")
        emit(src"""${lhs}_wVec(${lhs}_i).shiftEn := ${en} & ${DL(enable, enableRetimeMatch(en, lhs), true)}""")
        inds.zipWithIndex.foreach{ case(ind,j) => 
          emit(src"""${lhs}_wVec(${lhs}_i).addr($j) := ${ind}.r // Assume always an int""")
        }
        emit(src"""${lhs}_wVec(${lhs}_i).en := false.B""")
      close(src"}")
      duplicatesOf(rf).zipWithIndex.foreach{ case (mem, i) =>
        emit(src"""${rf}_$i.connectShiftPort(${lhs}_wVec, List(${portsOf(lhs, rf, i)})) """)
      }

    case op@LUTNew(dims, init) =>
      appPropertyStats += HasLUT
      val width = bitWidth(lhs.tp.typeArguments.head)
      val f = lhs.tp.typeArguments.head match {
        case a: FixPtType[_,_,_] => a.fracBits
        case _ => 0
      }
      val lut_consts = if (width == 1) {
        getConstValues(init).toList.map{a => if (a == true) "1.0" else "0.0"}.mkString(",")
      } else {
        getConstValues(init).toList.map{a => src"${a}d"}.mkString(",")
      }
      duplicatesOf(lhs).zipWithIndex.foreach{ case (mem, i) => 
        val numReaders = readersOf(lhs).filter{read => dispatchOf(read, lhs) contains i}.length
        emitGlobalModule(src"""val ${lhs}_$i = Module(new LUT(List($dims), List(${lut_consts}), ${numReaders}, $width, $f))""")
      }
        // } else {
        //   nbufs = nbufs :+ (lhs.asInstanceOf[Sym[SRAM[_]]], i)
        //   emitGlobalModule(s"val ${quote(lhs)}_$i = Module(new templates.NBufShiftRegFile(${dims(0)}, ${dims(1)}, 1, $depth, ${par}/${dims(0)}, $width))")
        //   emitGlobalModule(s"${quote(lhs)}_$i.io.reset := reset")          
        // }

      
    case op@LUTLoad(lut,inds,en) =>
      val dispatch = dispatchOf(lhs, lut).toList.head
      val idquote = src"${lhs}_id"
      emitGlobalWireMap(src"""${lhs}""",src"""Wire(${newWire(lhs.tp)})""")
      val parent = parentOf(lhs).get
      emit(src"""val ${idquote} = ${lut}_${dispatch}.connectRPort(List(${inds.map{a => src"${a}.r"}}), $en & ${DL(swap(parent, DatapathEn), enableRetimeMatch(en, lhs), true)})""")
      emit(src"""${lhs}.raw := ${lut}_${dispatch}.io.data_out(${idquote}).raw""")

    case op@VarRegNew(init)    => 
    case VarRegRead(reg)       => 
    case VarRegWrite(reg,v,en) => 
    case Print(x)   => 
    case Println(x) => 
    case PrintIf(_,_) =>
    case PrintlnIf(_,_) =>

    case _ => super.gen(lhs, rhs)
  }


  override protected def emitFileFooter() {
    withStream(getStream("BufferControlCxns")) {
      nbufs.foreach{ case (mem, i) => 
        val info = bufferControlInfo(mem, i)
        info.zipWithIndex.foreach{ case (inf, port) => 
          emit(src"""${mem}_${i}.connectStageCtrl(${DL(swap(quote(inf._1), Done), 1, true)}, ${swap(quote(inf._1), BaseEn)}, List(${port})) ${inf._2}""")
        }

      }
    }

    super.emitFileFooter()
  }

}
