package spatial.codegen.cppgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.util.spatialConfig

trait CppGenInterface extends CppGenCommon {

  // override protected def remap(tp: Type[_]): String = tp match {
  //   case tp: RegType[_] => src"${tp.typeArguments.head}"
  //   case _ => super.remap(tp)
  // }

  def isHostIO(x: Sym[_]) = "false"

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case ArgInNew(init)  => 
      argIns += lhs
      emit(src"${lhs.tp} $lhs = $init;")
    case HostIONew(init)  => 
      argIOs += lhs
      emit(src"${lhs.tp} $lhs = $init;")
    case ArgOutNew(init) => 
      argOuts += lhs
      emit(src"//${lhs.tp}* $lhs = new int32_t {0}; // Initialize cpp argout ???")
    // case HostIONew(init) => 
    //   argIOs += lhs.asInstanceOf[Sym[Reg[_]]]
    //   emit(src"${lhs.tp} $lhs = $init;")
    case RegRead(reg)    => 
      emit(src"${lhs.tp} $lhs = $reg;")
    case RegWrite(reg,v,en) => 
      emit(src"// $lhs $reg $v $en reg write")
    case DRAMHostNew(dims, _) =>
      drams += lhs
      emit(src"""uint64_t $lhs = c1->malloc(sizeof(${lhs.tp.typeArgs.head}) * ${dims.map(quote).mkString("*")});""")
      emit(src"c1->setArg(${argHandle(lhs)}_ptr, $lhs, false);")
      emit(src"""printf("Allocate mem of size ${dims.map(quote).mkString("*")} at %p\n", (void*)$lhs);""")

    case SetReg(reg, v) =>
      reg.tp.typeArgs.head match {
        case FixPtType(s,d,f) => 
          if (f != 0) {
            emit(src"c1->setArg(${argHandle(reg)}_arg, (int64_t)($v * ((int64_t)1 << $f)), ${reg.isHostIO}); // $reg")
            emit(src"$reg = $v;")
          } else {
            emit(src"c1->setArg(${argHandle(reg)}_arg, $v, ${reg.isHostIO});")
            emit(src"$reg = $v;")
          }
        case FltPtType(g,e) =>         
          emit(src"int64_t ${v}_raw;")
          emit(src"memcpy(&${v}_raw, &$v, sizeof($v));")
          emit(src"${v}_raw = ${v}_raw & ((int64_t) 0 | (int64_t) pow(2,${g+e}) - 1);")
          emit(src"c1->setArg(${argHandle(reg)}_arg, ${v}_raw, ${reg.isHostIO}); // $reg")
          emit(src"$reg = $v;")
        case _ => 
            emit(src"c1->setArg(${argHandle(reg)}_arg, $v, ${reg.isHostIO}); // $reg")
            emit(src"$reg = $v;")
      }
    case _: CounterNew[_] => 
    case _: CounterChainNew => 
    case GetReg(reg)    =>
      val bigArg = if (bitWidth(lhs.tp) > 32 & bitWidth(lhs.tp) <= 64) "64" else ""
      val get_string = src"c1->getArg$bigArg(${argHandle(reg)}_arg, ${reg.isHostIO})"
    
      lhs.tp match {
        case FixPtType(s,d,f) => 
          emit(src"int64_t ${lhs}_tmp = $get_string;")
          emit(src"bool ${lhs}_sgned = $s & ((${lhs}_tmp & ((int64_t)1 << ${d+f-1})) > 0); // Determine sign")
          emit(src"if (${lhs}_sgned) ${lhs}_tmp = ${lhs}_tmp | ~(((int64_t)1 << ${d+f})-1); // Sign-extend if necessary")
          emit(src"${lhs.tp} $lhs = (${lhs.tp}) ${lhs}_tmp / ((int64_t)1 << $f);")
        case FltPtType(g,e) => 
          emit(src"int64_t ${lhs}_tmp = $get_string;")
          emit(src"${lhs.tp} $lhs;")
          emit(src"memcpy(&$lhs, &${lhs}_tmp, sizeof($lhs));")
        case _ => 
          emit(src"${lhs.tp} $lhs = (${lhs.tp}($get_string));")
      }

    case StreamInNew(stream) => 
    case StreamOutNew(stream) => 

    case SetMem(dram, data) =>
      val rawtp = asIntType(dram.tp.typeArgs.head)
      val width = bitWidth(data.tp.typeArgs.head)
      val f = fracBits(dram.tp.typeArgs.head)
      val ptr = if (f > 0 && width >= 8) {
        emit(src"vector<$rawtp>* ${lhs}_rawified = new vector<$rawtp>((*$data).size());")
        open(src"for (int ${lhs}_rawified_i = 0; ${lhs}_rawified_i < (*$data).size(); ${lhs}_rawified_i++) {")
        emit(src"(*${lhs}_rawified)[${lhs}_rawified_i] = ($rawtp) ((*$data)[${lhs}_rawified_i] * (($rawtp)1 << $f));")
        close("}")
        src"${lhs}_rawified"
      } else if (f == 0 && width < 8){
        emit(src"vector<uint8_t>* ${lhs}_rawified = new vector<uint8_t>((*$data).size() / ${8/width});")
        open(src"for (int ${lhs}_rawified_i = 0; ${lhs}_rawified_i < (*$data).size(); ${lhs}_rawified_i = ${lhs}_rawified_i + ${8/width}) {")
          open(src"for (int ${lhs}_rawified_j = 0; ${lhs}_rawified_j < ${8/width}; ${lhs}_rawified_j++) {")
            emit(src"(*${lhs}_rawified)[${lhs}_rawified_i/${8/width}] = (*${lhs}_rawified)[${lhs}_rawified_i/${8/width}] + (uint8_t) (((*$data)[${lhs}_rawified_i + ${lhs}_rawified_j] % ${scala.math.pow(2,width).toInt}) << (${lhs}_rawified_j * $width) );")
          close("}")
        close("}")
        src"${lhs}_rawified"
      } else if (f > 0 && width < 8) throw new Exception(s"Small data types (< 8 bits) with more than 0 fractional bits is currently not supported.  Please transfer using an integer type.")
      else {
        src"$data"
      }
      emit(src"c1->memcpy($dram, &(*$ptr)[0], (*$ptr).size() * sizeof($rawtp));")

    case GetMem(dram, data) =>
      val rawtp = asIntType(dram.tp.typeArgs.head)
      val width = bitWidth(data.tp.typeArgs.head)
      val f = fracBits(dram.tp.typeArgs.head)
      if (f > 0 && width >= 8) {
        emit(src"vector<$rawtp>* ${lhs}_rawified = new vector<$rawtp>((*$data).size());")
        emit(src"c1->memcpy(&(*${lhs}_rawified)[0], $dram, (*${lhs}_rawified).size() * sizeof($rawtp));")
        open(src"for (int ${data}_i = 0; ${data}_i < (*$data).size(); ${data}_i++) {")
        emit(src"$rawtp ${data}_tmp = (*${lhs}_rawified)[${data}_i];")
        emit(src"(*$data)[${data}_i] = (double) ${data}_tmp / (($rawtp)1 << $f);")
        close("}")
      } else if (f == 0 && width < 8) {
        emit(src"vector<uint8_t>* ${lhs}_rawified = new vector<uint8_t>((*$data).size() / ${8/width});")
        emit(src"c1->memcpy(&(*${lhs}_rawified)[0], $dram, (*${lhs}_rawified).size() * sizeof($rawtp));")
        open(src"for (int ${lhs}_rawified_i = 0; ${lhs}_rawified_i < (*$data).size(); ${lhs}_rawified_i = ${lhs}_rawified_i + ${8/width}) {")
          open(src"for (int ${lhs}_rawified_j = 0; ${lhs}_rawified_j < ${8/width}; ${lhs}_rawified_j++) {")
            emit(src"(*$data)[${lhs}_rawified_i + ${lhs}_rawified_j] = ((*${lhs}_rawified)[${lhs}_rawified_i/${8/width}] >> (${lhs}_rawified_j * $width)) % ${scala.math.pow(2,width).toInt};")
          close("}")
        close("}")
      } else if (f > 0 && width < 8) throw new Exception(s"Small data types (< 8 bits) with more than 0 fractional bits is currently not supported.  Please transfer using an integer type.")
      else {
        emit(src"c1->memcpy(&(*$data)[0], $dram, (*$data).size() * sizeof(${dram.tp.typeArgs.head}));")
      }

    case _ => super.gen(lhs, rhs)
  }



  override def emitFooter(): Unit = {
    inGen(out,"ArgAPI.hpp") {
      emit("\n// ArgIns")
      argIns.zipWithIndex.foreach{case (a, id) => emit(src"#define ${argHandle(a)}_arg $id")}
      emit("\n// DRAM Ptrs:")
      drams.zipWithIndex.foreach {case (d, id) => emit(src"#define ${argHandle(d)}_ptr ${id+argIns.length}")}
      emit("\n// ArgIOs")
      argIOs.zipWithIndex.foreach{case (a, id) => emit(src"#define ${argHandle(a)}_arg ${id+argIns.length+drams.length}")}
      emit("\n// ArgOuts")
      argOuts.zipWithIndex.foreach{case (a, id) => emit(src"#define ${argHandle(a)}_arg ${id+argIns.length+drams.length+argIOs.length}")}
      if (spatialConfig.enableInstrumentation) {
        emit("\n// Instrumentation Counters")
        instrumentCounters.foreach { case (s, _) =>
          val base = instrumentCounterIndex(s)
          emit(src"#define ${quote(s).toUpperCase}_cycles_arg ${argIns.length + drams.length + argIOs.length + argOuts.length + base}")
          emit(src"#define ${quote(s).toUpperCase}_iters_arg ${argIns.length + drams.length + argIOs.length + argOuts.length + base + 1}")
          if (hasBackPressure(s.toCtrl) || hasForwardPressure(s.toCtrl)) {
            emit(src"#define ${quote(s).toUpperCase}_stalled_arg ${argIns.length + drams.length + argIOs.length + argOuts.length + base + 2}")
            emit(src"#define ${quote(s).toUpperCase}_idle_arg ${argIns.length + drams.length + argIOs.length + argOuts.length + base + 3}")
          }
        }
      }
      earlyExits.foreach{x => 
        emit(src"#define ${quote(x).toUpperCase}_exit_arg ${argOuts.toList.length + argIOs.toList.length + instrumentCounterArgs()}")
      }

    }
    super.emitFooter()
  }

}
