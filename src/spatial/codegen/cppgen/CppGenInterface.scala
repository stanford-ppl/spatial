package spatial.codegen.cppgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.data._
import spatial.util._


trait CppGenInterface extends CppGenCommon {

  // override protected def name(s: Dyn[_]): String = s match {
  //   case Def(ArgInNew(_))  => s"${s}_argin"
  //   case Def(ArgOutNew(_)) => s"${s}_argout"
  //   case Def(RegNew(_))    => s"""${s}_${s.name.getOrElse("reg")}"""
  //   case Def(RegRead(reg:Sym[_]))      => s"${s}_readx${reg.id}"
  //   case Def(RegWrite(reg:Sym[_],_,_)) => s"${s}_writex${reg.id}"
  //   case _ => super.name(s)
  // } 

  // override protected def remap(tp: Type[_]): String = tp match {
  //   case tp: RegType[_] => src"${tp.typeArguments.head}"
  //   case _ => super.remap(tp)
  // }

  def isHostIO(x: Sym[_]) = "false"

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case ArgInNew(init)  => 
      argIns += (lhs -> argIns.toList.length)
      emit(src"${lhs.tp} $lhs = $init;")
    case HostIONew(init)  => 
      argIOs += (lhs -> argIOs.toList.length)
      emit(src"${lhs.tp} $lhs = $init;")
    case ArgOutNew(init) => 
      argOuts += (lhs -> argOuts.toList.length)
      emit(src"//${lhs.tp}* $lhs = new int32_t {0}; // Initialize cpp argout ???")
    // case HostIONew(init) => 
    //   argIOs += lhs.asInstanceOf[Sym[Reg[_]]]
    //   emit(src"${lhs.tp} $lhs = $init;")
    case RegRead(reg)    => 
      emit(src"${lhs.tp} $lhs = $reg;")
    case RegWrite(reg,v,en) => 
      emit(src"// $lhs $reg $v $en reg write")
    case DRAMNew(dims, _) => 
      drams += (lhs -> drams.toList.length)
      emit(src"""uint64_t ${lhs} = c1->malloc(sizeof(${lhs.tp.typeArgs.head}) * ${dims.map(quote).mkString("*")});""")
      emit(src"c1->setArg(${argHandle(lhs)}_ptr, $lhs, false);")
      emit(src"""printf("Allocate mem of size ${dims.map(quote).mkString("*")} at %p\n", (void*)${lhs});""")

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
          emit(src"memcpy(&${v}_raw, &${v}, sizeof(${v}));")
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
      val get_string = src"c1->getArg${bigArg}(${argHandle(reg)}_arg, ${reg.isHostIO})"
    
      lhs.tp match {
        case FixPtType(s,d,f) => 
          emit(src"int64_t ${lhs}_tmp = ${get_string};")            
          emit(src"bool ${lhs}_sgned = $s & ((${lhs}_tmp & ((int64_t)1 << ${d+f-1})) > 0); // Determine sign")
          emit(src"if (${lhs}_sgned) ${lhs}_tmp = ${lhs}_tmp | ~(((int64_t)1 << ${d+f})-1); // Sign-extend if necessary")
          emit(src"${lhs.tp} ${lhs} = (${lhs.tp}) ${lhs}_tmp / ((int64_t)1 << $f);")            
        case FltPtType(g,e) => 
          emit(src"int64_t ${lhs}_tmp = ${get_string};")            
          emit(src"${lhs.tp} ${lhs};")
          emit(src"memcpy(&${lhs}, &${lhs}_tmp, sizeof(${lhs}));")
        case _ => 
          emit(src"${lhs.tp} $lhs = (${lhs.tp}) ${get_string};")
      }

      
    // case SetMem(dram, data) => 
    //   val rawtp = remapIntType(dram.tp.typeArguments.head)
    //   if (spatialNeedsFPType(dram.tp.typeArguments.head)) {
    //     dram.tp.typeArguments.head match { 
    //       case FixPtType(s,d,f) => 
    //         emit(src"vector<${rawtp}>* ${dram}_rawified = new vector<${rawtp}>((*${data}).size());")
    //         open(src"for (int ${dram}_rawified_i = 0; ${dram}_rawified_i < (*${data}).size(); ${dram}_rawified_i++) {")
    //           emit(src"(*${dram}_rawified)[${dram}_rawified_i] = (${rawtp}) ((*${data})[${dram}_rawified_i] * ((${rawtp})1 << $f));")
    //         close("}")
    //         emit(src"c1->memcpy($dram, &(*${dram}_rawified)[0], (*${dram}_rawified).size() * sizeof(${rawtp}));")
    //       case _ => emit(src"c1->memcpy($dram, &(*${data})[0], (*${data}).size() * sizeof(${rawtp}));")
    //     }
    //   } else {
    //     emit(src"c1->memcpy($dram, &(*${data})[0], (*${data}).size() * sizeof(${rawtp}));")
    //   }
    // case GetMem(dram, data) => 
    //   val rawtp = remapIntType(dram.tp.typeArguments.head)
    //   if (spatialNeedsFPType(dram.tp.typeArguments.head)) {
    //     dram.tp.typeArguments.head match { 
    //       case FixPtType(s,d,f) => 
    //         emit(src"vector<${rawtp}>* ${data}_rawified = new vector<${rawtp}>((*${data}).size());")
    //         emit(src"c1->memcpy(&(*${data}_rawified)[0], $dram, (*${data}_rawified).size() * sizeof(${rawtp}));")
    //         open(src"for (int ${data}_i = 0; ${data}_i < (*${data}).size(); ${data}_i++) {")
    //           emit(src"${rawtp} ${data}_tmp = (*${data}_rawified)[${data}_i];")
    //           emit(src"(*${data})[${data}_i] = (double) ${data}_tmp / ((${rawtp})1 << $f);")
    //         close("}")
    //       case _ => emit(src"c1->memcpy(&(*$data)[0], $dram, (*${data}).size() * sizeof(${rawtp}));")
    //     }
    //   } else {
    //     emit(src"c1->memcpy(&(*$data)[0], $dram, (*${data}).size() * sizeof(${rawtp}));")
    //   }

    case _ => super.gen(lhs, rhs)
  }



  override def emitFooter(): Unit = {
    inGen(out,"ArgAPI.hpp") {
      emit("\n// ArgIns")
      argIns.foreach{case (a, id) => emit(src"#define ${argHandle(a)}_arg $id")}
      emit("\n// DRAM Ptrs:")
      drams.foreach {case (d, id) => emit(src"#define ${argHandle(d)}_ptr ${id+argIns.toList.length}")}
      emit("\n// ArgIOs")
      argIOs.foreach{case (a, id) => emit(src"#define ${argHandle(a)}_arg ${id+argIns.toList.length+drams.toList.length}")}
      emit("\n// ArgOuts")
      argOuts.foreach{case (a, id) => emit(src"#define ${argHandle(a)}_arg ${id+argIns.toList.length+drams.toList.length+argIOs.toList.length}")}

    }
    super.emitFooter()
  }

}
