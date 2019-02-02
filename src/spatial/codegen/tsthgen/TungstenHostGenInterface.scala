package spatial.codegen.tsthgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.codegen.cppgen._

trait TungstenHostGenInterface extends TungstenHostCodegen {

  override def emitHeader = {
    super.emitHeader

    genIO {
      emit("""
#include <iostream>

std::stringstream stopsim;

  long cycle = 0;
""")
    }
  }

  def genIO(block: => Unit) = {
    inGen(out, "hostio.h") {
      block
    }
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case ArgInNew(init)  => 
      genIO {
        emit(src"${lhs.tp} $lhs = $init;")
      }
    case HostIONew(init)  => 
      genIO {
        emit(src"${lhs.tp} $lhs = $init;")
      }
    case ArgOutNew(init) => 
      genIO {
        emit(src"${lhs.tp} $lhs = $init;")
      }
    // case HostIONew(init) => 
    //   argIOs += lhs.asInstanceOf[Sym[Reg[_]]]
    //   emit(src"${lhs.tp} $lhs = $init;")
    case RegRead(reg)    => 
      emit(src"${lhs.tp} $lhs = $reg;")
    case RegWrite(reg,v,en) => 
      emit(src"// $lhs $reg $v $en reg write")
    //case DRAMHostNew(dims, _) =>
      //drams += lhs
      //emit(src"""uint64_t ${lhs} = c1->malloc(sizeof(${lhs.tp.typeArgs.head}) * ${dims.map(quote).mkString("*")});""")
      //emit(src"c1->setArg(${argHandle(lhs)}_ptr, $lhs, false);")
      //emit(src"""printf("Allocate mem of size ${dims.map(quote).mkString("*")} at %p\n", (void*)${lhs});""")
      //TODO

    case SetReg(reg, v) =>
      emit(s"$reg = $v;")
    //case _: CounterNew[_] => 
    //case _: CounterChainNew => 
    case GetReg(reg)    =>
      emit(s"auto $lhs = $reg;")

    //case SetMem(dram, data) =>
      //val rawtp = asIntType(dram.tp.typeArgs.head)
      //val f = fracBits(dram.tp.typeArgs.head)
      //if (f > 0) {
        //emit(src"vector<${rawtp}>* ${dram}_rawified = new vector<${rawtp}>((*${data}).size());")
        //open(src"for (int ${dram}_rawified_i = 0; ${dram}_rawified_i < (*${data}).size(); ${dram}_rawified_i++) {")
        //emit(src"(*${dram}_rawified)[${dram}_rawified_i] = (${rawtp}) ((*${data})[${dram}_rawified_i] * ((${rawtp})1 << $f));")
        //close("}")
        //emit(src"c1->memcpy($dram, &(*${dram}_rawified)[0], (*${dram}_rawified).size() * sizeof(${rawtp}));")
      //}
      //else {
        //emit(src"c1->memcpy($dram, &(*${data})[0], (*${data}).size() * sizeof(${dram.tp.typeArgs.head}));")
      //}

    //case GetMem(dram, data) =>
      //val rawtp = asIntType(dram.tp.typeArgs.head)
      //val f = fracBits(dram.tp.typeArgs.head)
      //if (f > 0) {
        //emit(src"vector<${rawtp}>* ${data}_rawified = new vector<${rawtp}>((*${data}).size());")
        //emit(src"c1->memcpy(&(*${data}_rawified)[0], $dram, (*${data}_rawified).size() * sizeof(${rawtp}));")
        //open(src"for (int ${data}_i = 0; ${data}_i < (*${data}).size(); ${data}_i++) {")
        //emit(src"${rawtp} ${data}_tmp = (*${data}_rawified)[${data}_i];")
        //emit(src"(*${data})[${data}_i] = (double) ${data}_tmp / ((${rawtp})1 << $f);")
        //close("}")
      //}
      //else {
        //emit(src"c1->memcpy(&(*$data)[0], $dram, (*${data}).size() * sizeof(${dram.tp.typeArgs.head}));")
      //}

    case _ => super.gen(lhs, rhs)
  }

}
