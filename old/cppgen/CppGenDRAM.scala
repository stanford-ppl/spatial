package spatial.codegen.cppgen

import argon.core._
import spatial.aliases._
import spatial.metadata._
import spatial.nodes._

trait CppGenDRAM extends CppGenSRAM {

  override protected def name(s: Dyn[_]): String = s match {
    case Def(_: DRAMNew[_,_])=> s"""${s}_${s.name.getOrElse("dram")}"""
    case _ => super.name(s)
  } 

  override protected def remap(tp: Type[_]): String = tp match {
    // case tp: DRAMType[_] => src"DRAM"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case DRAMNew(dims, _) =>
      drams = drams :+ lhs
      emit(src"""uint64_t ${lhs} = c1->malloc(sizeof(${remapIntType(lhs.tp.typeArguments.head)}) * ${dims.map(quote).mkString("*")});""")
      emit(src"c1->setArg(${argHandle(lhs)}_ptr, $lhs, false); // (memstream in: ${argMapping(lhs)._2}, out: ${{argMapping(lhs)._3}})")
      emit(src"""printf("Allocate mem of size ${dims.map(quote).mkString("*")} at %p\n", (void*)${lhs});""")
      // emit(src"""uint64_t ${lhs} = (uint64_t) ${lhs}_void;""")

    // case Gather(dram, local, addrs, ctr, i)  => emit("// Do what?")
    // case Scatter(dram, local, addrs, ctr, i) => emit("// Do what?")
    // case BurstLoad(dram, fifo, ofs, ctr, i)  => emit("//found load")
    // case BurstStore(dram, fifo, ofs, ctr, i) => emit("//found store")
    case _ => super.gen(lhs, rhs)
  }

  override protected def emitFileFooter() {
    withStream(getStream("ArgAPI", "h")) {
      emit("\n// DRAM Ptrs:")
      drams.foreach { d =>
        emit(src"#define ${d.name.getOrElse(quote(d)).toUpperCase}_ptr ${argMapping(d)._2}")
      }
    }
    super.emitFileFooter()
  }


}
