package spatial.codegen.tsthgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.codegen.cppgen._
import spatial.metadata.bounds.Expect

trait TungstenHostGenInterface extends TungstenHostCodegen with CppGenCommon {

  override def emitHeader = {
    super.emitHeader

    genIO {
      emit("""
#include <iostream>

""")
    }

  }

  val allocated = scala.collection.mutable.ListBuffer[String]()

  override def emitFooter = {
    genIO {
      open(src"void AllocAllMems() {")
      allocated.foreach { a =>
        emit(s"$a();")
      }
      close(src"}")
    }
    allocated.clear
    super.emitFooter
  }

  def genIO(block: => Unit):Unit = {
    inGen(out, "hostio.h") {
      block
    }
  }

  def genAlloc(lhs:Sym[_], inFunc:Boolean)(block: => Unit):Unit = {
    if (inFunc) {
      val func = s"Alloc${lhs}" 
      allocated += func
      emit(src"$func();")
      genIO {
        open(src"void $func() {")
        block
        close(src"}")
      }
    } else {
      block
    }
  }

  val bytePerBurst = 64 //TODO

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


    case SetReg(reg, v) =>
      emit(src"$reg = $v;")
    //case _: CounterNew[_] => 
    //case _: CounterChainNew => 
    case GetReg(reg)    =>
      emit(src"auto $lhs = $reg;")

    case DRAMHostNew(dims, _) =>
      val tp = lhs.tp.typeArgs.head
      genIO {
        // Make sure allocated address is burst aligned
        emit(src"""void* $lhs;""")
      }
      val cdims = dims.map { case Expect(c) => c; case d => quote(d) }
      genAlloc(lhs, dims.forall { _.isConst }) { 
        emit(src"$lhs = malloc(sizeof($tp) * ${cdims.mkString("*")} + ${bytePerBurst});")
        emit(src"$lhs = (void *) (((uint64_t) ${lhs} + $bytePerBurst - 1) / $bytePerBurst * $bytePerBurst);")
        emit(src"""cout << "Allocate ${lhs.name.getOrElse(lhs)} of size "<< ${cdims.mkString(""" << " * " << """)} << " at " << ($tp*)${lhs} << " (" << (long)$lhs << ")" << endl;""")
      }

    case SetMem(dram, data) =>
      emit(src"memcpy($dram, &(*${data})[0], (*${data}).size() * sizeof(${dram.tp.typeArgs.head}));")

    case GetMem(dram, data) =>
      emit(src"memcpy(&(*$data)[0], $dram, (*${data}).size() * sizeof(${dram.tp.typeArgs.head}));")

    case _ => super.gen(lhs, rhs)
  }

}
