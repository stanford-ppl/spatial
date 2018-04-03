package spatial.codegen.chiselgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.internal.{spatialConfig => cfg}


trait ChiselGenReg extends ChiselGenCommon {

  // var outMuxMap: Map[Sym[Reg[_]], Int] = Map()
  private var nbufs: List[(Sym[Reg[_]], Int)]  = List()

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
	case ArgInNew(init)  => 
      argIns += (lhs -> (argIns.toList.length + drams.toList.length))
    case ArgOutNew(init) => 
      // emitGlobalWireMap(src"${lhs}_data_options", src"Wire(Vec(${scala.math.max(1,writersOf(lhs).size)}, UInt(64.W)))", forceful=true)
      // emitGlobalWireMap(src"${lhs}_en_options", src"Wire(Vec(${scala.math.max(1,writersOf(lhs).size)}, Bool()))", forceful=true)
      // emitt(src"""io.argOuts(${argMapping(lhs).argOutId}).bits := chisel3.util.Mux1H(${swap(lhs, EnOptions)}, ${swap(lhs, DataOptions)}) // ${lhs.name.getOrElse("")}""", forceful=true)
      // emitt(src"""io.argOuts(${argMapping(lhs).argOutId}).valid := ${swap(lhs, EnOptions)}.reduce{_|_}""", forceful=true)
      argOuts += (lhs -> (argOuts.toList.length + argOuts.toList.length))

    case GetArgOut(reg) => 
      argOutLoopbacks.getOrElseUpdate(argOuts(reg), argOutLoopbacks.toList.length)
      // emitGlobalWireMap(src"""${lhs}""",src"Wire(${newWire(reg.tp.typeArguments.head)})")
      emitt(src"""${lhs}.r := io.argOutLoopbacks(${argOutLoopbacks(argOuts(reg))})""")

    // case HostIONew(init) =>
    //   emitGlobalWireMap(src"${lhs}_data_options", src"Wire(Vec(${scala.math.max(1,writersOf(lhs).size)}, UInt(64.W)))", forceful = true)
    //   emitGlobalWireMap(src"${lhs}_en_options", src"Wire(Vec(${scala.math.max(1,writersOf(lhs).size)}, Bool()))", forceful = true)
    //   emitt(src"""io.argOuts(${argMapping(lhs).argOutId}).bits := chisel3.util.Mux1H(${swap(lhs, EnOptions)}, ${swap(lhs, DataOptions)}) // ${lhs.name.getOrElse("")}""", forceful = true)
    //   emitt(src"""io.argOuts(${argMapping(lhs).argOutId}).valid := ${swap(lhs, EnOptions)}.reduce{_|_}""", forceful = true)
    //   argIOs = argIOs :+ lhs.asInstanceOf[Sym[Reg[_]]]

    // case RegNew(init)    => 
    //   // Console.println(src" working on reg $lhs")
    //   val width = bitWidth(init.tp)
    //   emitGlobalWire(src"val ${lhs}_initval = ${init}")
    //   resettersOf(lhs).indices.foreach{ i => emitGlobalWire(src"""val ${lhs}_manual_reset_$i = Wire(Bool())""")}
    //   if (resettersOf(lhs).nonEmpty) emitGlobalWire(src"""val ${lhs}_manual_reset = ${resettersOf(lhs).indices.map{i => src"${lhs}_manual_reset_$i"}.mkString(" | ")}""")
    //   val duplicates = duplicatesOf(lhs)
    //   duplicates.zipWithIndex.foreach{ case (d, i) => 
    //     val numBroadcasters = if (writersOf(lhs).isEmpty) 0 else writersOf(lhs).count{write => portsOf(write, lhs, i).toList.length > 1}
    //     val numWriters = writersOf(lhs)
    //       .filter{write => dispatchOf(write, lhs) contains i}
    //       .count{w => portsOf(w, lhs, i).toList.length == 1}.max(1)
    //     reduceType(lhs) match {
    //       case Some(fps: ReduceFunction) => 
    //         fps match {
    //           case FixPtSum => 
    //             if (d.isAccum) {
    //               if (!spatialNeedsFPType(lhs.tp.typeArguments.head)) {
    //                 emitGlobalModule(src"""val ${lhs}_${i} = Module(new SpecialAccum(1,"add","UInt", List(${width}))) """)  
    //               } else {
    //                 lhs.tp.typeArguments.head match {
    //                   case FixPtType(s,d,f) => emitGlobalModule(src"""val ${lhs}_${i} = Module(new SpecialAccum(1,"add","FixedPoint", List(${if (s) 1 else 0},$d,$f)))""")  
    //                   case _ => emitGlobalModule(src"""val ${lhs}_${i} = Module(new SpecialAccum(1,"add","UInt", List(${width}))) // TODO: No match""")  
    //                 }                  
    //               }
    //               // Figure out if we need to tie down direct ports
    //               val direct_tiedown = writersOf(lhs).forall{w => reduceType(w.node).isDefined }
    //               if (direct_tiedown) {
    //                 emitGlobalModule(src"""${lhs}_${i}.io.input.direct_enable := false.B""")
    //               }
    //             } else {
    //               if (d.depth > 1) {
    //                 nbufs = nbufs :+ (lhs.asInstanceOf[Sym[Reg[_]]], i)
    //                 if (numWriters > 1) warn(s"You have multiple writers to an NBufFF ( ${lhs.name.getOrElse("")} = ${numWriters} writes ).  Have you considered the loop-carry dependency issues?")
    //                 emitGlobalModuleMap(src"${lhs}_${i}", src"Module(new NBufFF(${d.depth}, ${width}, numWriters = ${numWriters}))")
    //                 if (numBroadcasters == 0){
    //                   emitt(src"${swap(src"${lhs}_${i}", Blank)}.io.broadcast.enable := false.B")
    //                 }
    //               } else {
    //                 emitGlobalModuleMap(src"${lhs}_${i}",src"Module(new templates.FF(${width}, ${numWriters}))")
    //               }              
    //             }
    //           case _ => 
    //             if (d.depth > 1) {
    //               nbufs = nbufs :+ (lhs.asInstanceOf[Sym[Reg[_]]], i)
    //               if (numWriters > 1) warn(s"You have multiple writers to an NBufFF ( ${lhs.name.getOrElse("")} = ${numWriters} writes ).  Have you considered the loop-carry dependency issues?")
    //               emitGlobalModuleMap(src"${lhs}_${i}", src"Module(new NBufFF(${d.depth}, ${width}, numWriters = ${numWriters}))")
    //               if (numBroadcasters == 0){
    //                 emitt(src"${swap(src"${lhs}_${i}", Blank)}.io.broadcast.enable := false.B")
    //               }
    //             } else {
    //               emitGlobalModuleMap(src"${lhs}_${i}", src"Module(new templates.FF(${width}, ${numWriters}))")
    //             }
    //         }
    //       case _ =>
    //         if (d.depth > 1) {
    //           nbufs = nbufs :+ (lhs.asInstanceOf[Sym[Reg[_]]], i)
    //           if (numWriters > 1) warn(s"You have multiple writers to an NBufFF ( ${lhs.name.getOrElse("")} = ${numWriters} writes ).  Have you considered the loop-carry dependency issues?")
    //           emitGlobalModuleMap(src"${lhs}_${i}", src"Module(new NBufFF(${d.depth}, ${width}, numWriters = ${numWriters}))")
    //           if (numBroadcasters == 0){
    //             emitt(src"${swap(src"${lhs}_${i}", Blank)}.io.broadcast.enable := false.B")
    //           }
    //         } else {
    //           emitGlobalModuleMap(src"${lhs}_${i}",src"Module(new templates.FF(${width}, ${numWriters}))")
    //         }
    //     } // TODO: Figure out which reg is really the accum
    //   }
    // case RegRead(reg)    => 
    //   val lhs_sym = quote(lhs)
    //   if (isArgIn(reg) | isHostIO(reg)) {
    //     emitGlobalWireMap(src"""${lhs}""",src"Wire(${newWire(reg.tp.typeArguments.head)})")
    //     emitGlobalWire(src"""${lhs}.r := io.argIns(${argMapping(reg).argInId})""")
    //   } else if (isArgOut(reg)) {
    //     argOutLoopbacks.getOrElseUpdate(argMapping(reg).argOutId, argOutLoopbacks.toList.length)
    //     emitGlobalWireMap(src"""${lhs}""",src"Wire(${newWire(reg.tp.typeArguments.head)})")
    //     emitt(src"""${lhs}.r := io.argOutLoopbacks(${argOutLoopbacks(argMapping(reg).argOutId)})""")
    //   } else {
    //     emitGlobalWireMap(src"""$lhs""", src"""Wire(${newWire(lhs.tp)})""") 
    //     if (dispatchOf(lhs, reg).isEmpty) {
    //       throw new spatial.EmptyDispatchException(lhs)
    //     }
    //     val inst = dispatchOf(lhs, reg).head // Reads should only have one index
    //     val port = portsOf(lhs, reg, inst)
    //     val duplicates = duplicatesOf(reg)
    //     // Console.println(s"working on $lhs $reg $inst $duplicates")
    //     if (duplicates(inst).isAccum) {
    //       reduceType(lhs) match {
    //         case Some(fps: ReduceFunction) => 
    //           fps match {
    //             case FixPtSum =>
    //               if (spatialNeedsFPType(reg.tp.typeArguments.head)) {
    //                 reg.tp.typeArguments.head match {
    //                   case FixPtType(s,d,f) => emitt(src"""${lhs}.r := Utils.FixedPoint(${s}, $d, $f, ${reg}_initval).r // get reset value that was created by reduce controller""")                    
    //                 }
    //               } else {
    //                 emitt(src"""${lhs}.r := ${reg}_initval // get reset value that was created by reduce controller""")                    
    //               }
    //             case _ =>  
    //               lhs.tp match { 
    //                 case FixPtType(s,d,f) => 
    //                   emitt(src"""${lhs}.r := ${swap(src"${reg}_${inst}", Blank)}.read(${port.head})""")
    //                 case BooleanType() => emitt(src"""${lhs}.r := ${swap(src"${reg}_${inst}", Blank)}.read(${port.head}) === 1.U(1.W)""")
    //                 case _ => emitt(src"""${lhs}.r := ${swap(src"${reg}_${inst}", Blank)}.read(${port.head})""")
    //               }
    //           }
    //         case _ =>
    //           lhs.tp match { 
    //             case FixPtType(s,d,f) => 
    //               emitt(src"""${lhs}.r := ${swap(src"${reg}_${inst}", Blank)}.read(${port.head})""")
    //             case BooleanType() => emitt(src"""${lhs}.r := ${swap(src"${reg}_${inst}", Blank)}.read(${port.head}) === 1.U(1.W)""")
    //             case _ => emitt(src"""${lhs}.r := ${swap(src"${reg}_${inst}", Blank)}.read(${port.head})""")
    //           }
    //       }
    //     } else {
    //       lhs.tp match { 
    //         case FixPtType(s,d,f) => 
    //           emitt(src"""${lhs}.r := ${swap(src"${reg}_${inst}", Blank)}.read(${port.head})""")
    //         case BooleanType() => emitt(src"""${lhs}.r := ${swap(src"${reg}_${inst}", Blank)}.read(${port.head}) === 1.U(1.W)""")
    //         case _ => emitt(src"""${lhs}.r := ${swap(src"${reg}_${inst}", Blank)}.read(${port.head})""")
    //       }
    //     }
    //   }


    // case RegReset(reg,en) => 
    //   val parent = parentOf(lhs).get
    //   val id = resettersOf(reg).map{_._1}.indexOf(lhs)
    //   emitt(src"${reg}_manual_reset_$id := $en & ${DL(swap(parent, DatapathEn), src"${enableRetimeMatch(en, lhs)}.toInt")} ")

    // case RegWrite(reg,v,en) => 
    //   val fully_unrolled_accum = !writersOf(reg).exists{w => readersOf(reg).exists{ r => w.node.dependsOn(r.node) }}
    //   val manualReset = if (resettersOf(reg).length > 0) {s"| ${quote(reg)}_manual_reset"} else ""
    //   val parent = writersOf(reg).find{_.node == lhs}.get.ctrlNode
    //   if (isArgOut(reg) | isHostIO(reg)) {
    //     val id = argMapping(reg).argOutId
    //       emitt(src"val ${lhs}_wId = getArgOutLane($id)")
    //       v.tp match {
    //         case FixPtType(s,d,f) => 
    //           if (s) {
    //             val pad = 64 - d - f
    //             if (pad > 0) {
    //               emitt(src"""${swap(reg, DataOptions)}(${lhs}_wId) := util.Cat(util.Fill($pad, ${v}.msb), ${v}.r)""")  
    //             } else {
    //               emitt(src"""${swap(reg, DataOptions)}(${lhs}_wId) := ${v}.r""")                  
    //             }
    //           } else {
    //             emitt(src"""${swap(reg, DataOptions)}(${lhs}_wId) := ${v}.r""")                  
    //           }
    //         case _ => 
    //           emitt(src"""${swap(reg, DataOptions)}(${lhs}_wId) := ${v}.r""")                  
    //         }
    //       emitt(src"""${swap(reg, EnOptions)}(${lhs}_wId) := $en & ${DL(swap(parent, DatapathEn), src"${enableRetimeMatch(en, lhs)}.toInt")}""")
    //   } else {
    //     reduceType(lhs) match {
    //       case Some(fps: ReduceFunction) => // is an accumulator
    //         // Make sure this was not stripped of its accumulation from full unroll
    //         if (fully_unrolled_accum) {
    //           emitGlobalWireMap(src"""${reg}_wren""", "Wire(Bool())");emitt(src"${swap(reg, Wren)} := ${swap(parentOf(lhs).get, DatapathEn)}")
    //           emitGlobalWireMap(src"""${reg}_resetter""", "Wire(Bool())");emitt(src"""${swap(reg, Resetter)} := ${DL(swap(parentOf(lhs).get, RstEn), src"${enableRetimeMatch(en, lhs)}.toInt", true)} // Delay was added on 12/5/2017, not sure why it wasn't there before""")
    //         }
    //         emitGlobalWireMap(src"""${lhs}""", src"""Wire(${newWire(reg.tp.typeArguments.head)})""")
    //         duplicatesOf(reg).zipWithIndex.foreach { case (dup, ii) =>
    //           fps match {
    //             case FixPtSum =>
    //               if (dup.isAccum) {
    //                 emitt(src"""${swap(src"${reg}_${ii}", Blank)}.io.input.next := ${v}.number""")
    //                 emitt(src"""${swap(src"${reg}_${ii}", Blank)}.io.input.enable := ${DL(swap(reg, Wren), src"${enableRetimeMatch(en, lhs)}.toInt", true)}""")
    //                 emitt(src"""${swap(src"${reg}_${ii}", Blank)}.io.input.init := ${reg}_initval.number""")
    //                 emitt(src"""${swap(src"${reg}_${ii}", Blank)}.io.input.reset := accelReset | ${DL(src"${swap(reg, Resetter)} ${manualReset}", src"${enableRetimeMatch(en, lhs)}.toInt", true)}""")
    //                 emitt(src"""${lhs} := ${swap(src"${reg}_${ii}", Blank)}.io.output""")
    //               } else {
    //                 val ports = portsOf(lhs, reg, ii) // Port only makes sense if it is not the accumulating duplicate
    //                 val data_string = if (fully_unrolled_accum) src"$v" else src"$lhs"
    //                 emitt(src"""${swap(src"${reg}_${ii}", Blank)}.write(${data_string}, $en & ${DL(src"${swap(reg, Wren)} & ${swap(parent, IIDone)}", src"${enableRetimeMatch(en, lhs)}.toInt+1", true)}, accelReset ${manualReset}, List($ports), ${reg}_initval.number, accumulating = ${isAccum(lhs)})""")
    //               }
    //             case _ =>
    //               val ports = portsOf(lhs, reg, ii) // Port only makes sense if it is not the accumulating duplicate
    //               val dlay = if (accumsWithIIDlay.contains(reg)) {src"${reg}_II_dlay"} else "0" // Ultra hacky
    //               if (dup.isAccum) {
    //                 emitt(src"""${swap(src"${reg}_${ii}", Blank)}.write($v, $en & ${DL(src"${swap(reg, Wren)} & ${DL(swap(parent, IIDone), dlay, true)}", src"${enableRetimeMatch(en, lhs)}.toInt", true)}, accelReset | ${DL(swap(reg, Resetter), src"${enableRetimeMatch(en, lhs)}.toInt", true)} ${manualReset}, List($ports), ${reg}_initval.number, accumulating = ${isAccum(lhs)})""")
    //               } else {
    //                 emitt(src"""${swap(src"${reg}_${ii}", Blank)}.write($v, $en & ${DL(src"${swap(reg, Wren)} & ${DL(swap(parent, IIDone), dlay, true)}", src"${enableRetimeMatch(en, lhs)}.toInt", true)}, accelReset ${manualReset}, List($ports), ${reg}_initval.number, accumulating = ${isAccum(lhs)})""")
    //               }
                  
    //           }
    //         }
    //       case _ => // Not an accum
    //         duplicatesOf(reg).zipWithIndex.foreach { case (dup, ii) =>
    //           val ports = portsOf(lhs, reg, ii) // Port only makes sense if it is not the accumulating duplicate
    //           emitt(src"""${swap(src"${reg}_${ii}", Blank)}.write($v, $en & ${DL(src"${swap(parent, DatapathEn)} & ${swap(parent, IIDone)}", src"${enableRetimeMatch(en, lhs)}.toInt", true)}, accelReset ${manualReset}, List($ports), ${reg}_initval.number, accumulating = ${isAccum(lhs)})""")
    //         }
    //     }
    //   }
	case _ => super.gen(lhs, rhs)
  }

  override def emitFooter(): Unit = {
    // inGenn(out, "BufferControlCxns", ext)) {
    //   nbufs.foreach{ case (mem, i) => 
    //     val info = bufferControlInfo(mem, i)
    //     info.zipWithIndex.foreach{ case (inf, port) => 
    //       emitt(src"""${swap(src"${mem}_${i}", Blank)}.connectStageCtrl(${DL(swap(quote(inf._1), Done), 1, true)}, ${swap(quote(inf._1), BaseEn)}, List(${port})) ${inf._2}""")
    //     }
    //   }
    // }

    inGen(out, "Instantiator.scala") {
      emit("")
      emit("// Scalars")
      emit(s"val numArgIns_reg = ${argIns.toList.length}")
      emit(s"val numArgOuts_reg = ${argOuts.toList.length}")
      emit(s"val numArgIOs_reg = ${argIOs.toList.length}")
      argIns.zipWithIndex.foreach { case(p,i) => emit(s"""//${quote(p._1)} = argIns($i) ( ${p._1.name.getOrElse("")} )""") }
      argOuts.zipWithIndex.foreach { case(p,i) => emit(s"""//${quote(p._1)} = argOuts($i) ( ${p._1.name.getOrElse("")} )""") }
      argIOs.zipWithIndex.foreach { case(p,i) => emit(s"""//${quote(p._1)} = argIOs($i) ( ${p._1.name.getOrElse("")} )""") }
      emit(s"val io_argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int] = ${argOutLoopbacks}")
    }

    inGenn(out, "IOModule", ext) {
      emitt("// Scalars")
      emitt(s"val io_numArgIns_reg = ${argIns.toList.length}")
      emitt(s"val io_numArgOuts_reg = ${argOuts.toList.length}")
      emitt(s"val io_numArgIOs_reg = ${argIOs.toList.length}")
      emitt(s"val io_argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int] = ${argOutLoopbacks}")

    }

    super.emitFooter()
  }

}