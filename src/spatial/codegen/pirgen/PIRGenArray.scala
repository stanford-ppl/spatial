package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._

trait PIRGenArray extends PIRCodegen {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@ArrayNew(size)                           => emitDummy(lhs, rhs)
    case op@ArrayFromSeq(seq)                        => emitDummy(lhs, rhs)

    case ArrayApply(array @ Op(InputArguments()), i) => emitDummy(lhs, rhs)

    case ArrayApply(array, i)                        => emitDummy(lhs, rhs)
    case ArrayLength(array)                          => emitDummy(lhs, rhs)
    case InputArguments()                            => emitDummy(lhs, rhs)

    case ArrayUpdate(array, i, data)                 => emitDummy(lhs, rhs)
    case MapIndices(size, func)                      => emitDummy(lhs, rhs)

    case ArrayForeach(array,apply,func)              => emitDummy(lhs, rhs)

    case ArrayMap(array,apply,func)                  => emitDummy(lhs, rhs)

    case ArrayZip(a, b, applyA, applyB, func)        => emitDummy(lhs, rhs)

    case ArrayReduce(array, _, reduce)               => emitDummy(lhs, rhs)

    case ArrayFilter(array, _, cond)                 => emitDummy(lhs, rhs)

    case ArrayFlatMap(array, _, func)                => emitDummy(lhs, rhs)

    case _ => super.gen(lhs, rhs)
  }
}
