package spatial.traversal

import argon._
import argon.node._
import emul.Number

import spatial.lang._
import spatial.node._
import spatial.metadata.bounds._
import spatial.metadata.types._
import spatial.util.spatialConfig

case class UserSanityChecks(IR: State, enable: Boolean) extends AbstractSanityChecks {

  override def shouldRun: Boolean = enable

  def busWidthCheck(lhs:Sym[_], tp: Bits[_], bus: Bus, mem: String): Unit = {
    if (tp.nbits < bus.nbits) {
      warn(lhs.ctx, s"Bus bits is greater than number of bits of $mem type.")
      warn(s"Hardware will drive only the first ${tp.nbits} bits of the bus.")
      warn(lhs.ctx)
    }
    else if (tp.nbits > bus.nbits) {
      warn(lhs.ctx, s"Bus bits is smaller than number of bits of $mem type.")
      warn(s"Hardware will use only the first ${tp.nbits} bits in the word")
      warn(lhs.ctx)
    }
  }

  def sanityCheckBlock(block: Block[_]): Unit = {

  }

  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case GetReg(_) if inHw =>
      error(lhs.ctx, "Reading ArgOuts within Accel is disallowed.")
      error("Use a Reg to store intermediate values.")
      error(lhs.ctx)
      IR.logError()

    case SetReg(arg,_) if inHw =>
      error(lhs.ctx, "Writing ArgIn within Accel is disallowed.")
      error("Use a Reg to store intermediate values.")
      error(lhs.ctx)
      IR.logError()

    case AccelScope(block) => inAccel { sanityCheckBlock(block) }

    case SpatialBlackboxImpl(block) => inBox { sanityCheckBlock(block) }
    case SpatialCtrlBlackboxImpl(block) => inBox { sanityCheckBlock(block) }

    case op: OpMemReduce[_,_] =>
      if (op.map.result == op.accum) {
        warn(op.map.result.ctx, s"${op.map.result.nameOr("Memory")} is used both as a MemReduce map result and accumulator.")
        warn(op.map.result.ctx)
        warn(lhs.ctx, "In the MemReduce defined here.", noWarning = true)
        warn(lhs.ctx)
        warn("This will update both the result and accumulator on every iteraton.")
        warn("It is extremely unlikely this is the behavior you want.\n")
      }
      super.visit(lhs,rhs)


    case CounterNew(_,_,Literal(step: Number),_) if step === 0 =>
      error(lhs.ctx, "Counter has step size of 0.")
      error("This will create a counter with infinite iterations, which is probably not what you wanted.")
      error("(If this is what you wanted, use the forever counter '*' notation instead.)")
      error(lhs)
      IR.logError()

    case CounterNew(Expect(start),Expect(end),Expect(step),Expect(par)) =>
      val len = Math.ceil((end.toDouble - start.toDouble)/step.toDouble).toInt
      dbg(s"${stm(lhs)}")
      dbg(s"length: $len, par: $par")
      if (par > len) {
        warn(lhs.ctx, s"Counter parallelization ($par) is greater than total number of iterations ($len)")
        warn(lhs.ctx)
      }


    case RegNew(init) if !init.isFixedBits =>
      error(lhs.ctx, s"Reset value of register ${lhs.fullname} was not a constant.")
      error(lhs.ctx)
      IR.logError()

    case RegNew(init) if !inHw =>
      error(lhs.ctx, s"Register was created outside Accel. Use an ArgIn, ArgOut, or HostIO instead.")
      error(lhs.ctx)
      IR.logError()

    case op @ StreamInNew(bus)  => busWidthCheck(lhs, op.A,bus,"StreamIn")
    case op @ StreamOutNew(bus) => busWidthCheck(lhs, op.A,bus,"StreamOut")

    case LUTNew(dims,elems) =>
      val size = dims.map(_.toInt).product
      if (elems.length != size) {
        // TODO[5]: This could be downgraded to a warning if we fill zeros in here
        if (spatialConfig.enablePIR) {
          warn(lhs.ctx, s"Total size of LUT ($size) does not match the number of supplied elements (${elems.length}).")
          warn(lhs.ctx)
        } else {
          error(lhs.ctx, s"Total size of LUT ($size) does not match the number of supplied elements (${elems.length}).")
          error(lhs.ctx)
          IR.logError()
        }
      }

    case _ => super.visit(lhs, rhs)
  }

}
