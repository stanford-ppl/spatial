package spatial.traversal

import argon._
import argon.node._
import emul.Number

import spatial.lang._
import spatial.node._
import spatial.metadata.bounds._
import spatial.metadata.types._

case class UserSanityChecks(IR: State) extends AbstractSanityChecks {

  def busWidthCheck(tp: Bits[_], bus: Bus, mem: String): Unit = {
    if (tp.nbits < bus.nbits) {
      warn(ctx, s"Bus bits is greater than number of bits of $mem type.")
      warn(s"Hardware will drive only the first ${tp.nbits} bits of the bus.")
      warn(ctx)
    }
    else if (tp.nbits > bus.nbits) {
      warn(ctx, s"Bus bits is smaller than number of bits of $mem type.")
      warn(s"Hardware will use only the first ${tp.nbits} bits in the word")
      warn(ctx)
    }
  }

  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case GetReg(_) if inHw =>
      error(lhs.ctx, "Reading ArgOuts within Accel is disallowed.")
      error("Use a Reg to store intermediate values.")
      error(lhs.ctx)

    case SetReg(arg,_) if inHw =>
      error(lhs.ctx, "Writing ArgIn within Accel is disallowed.")
      error("Use a Reg to store intermediate values.")
      error(lhs.ctx)

    case AccelScope(block) => inAccel {
      val (stms,rawInputs) = block.nestedStmsAndInputs

      val specialized = stms.collect{
        case sym @ Op(VarRead(v)) if !stms.contains(v) =>
          error(sym.ctx, s"Variables cannot be used within the Accel scope.")
          error("Use an ArgIn, HostIO, or DRAM to pass values from the host to the accelerator.")
          error(sym.ctx, showCaret = true)
          sym

        case sym @ Op(VarNew(init)) =>
          error(sym.ctx, s"Variables cannot be created within the Accel scope.")
          error("Use a local accelerator memory like SRAM or Reg instead.")
          error(sym.ctx)
          sym

        case sym @ Op(VarAssign(v, x)) if !stms.contains(v) =>
          error(sym.ctx, s"Variables cannot be assigned within the Accel scope.")
          error("Use an ArgOut, HostIO, or DRAM to pass values from the accelerator to the host.")
          error(sym.ctx, showCaret = true)
          sym

        case sym @ Op(ArrayApply(Def(InputArguments()), _)) =>
          error(sym.ctx, "Input arguments cannot be accessed in Accel scope.")
          error("Use an ArgIn or HostIO to pass values from the host to the accelerator.")
          error(sym.ctx, showCaret = true)
          sym
      }
      val inputs = rawInputs.filterNot{
        case _:Var[_] => false    // Don't give two errors for Vars
        case _ => true
      }

      val illegalUsed = disallowedInputs(stms diff specialized, inputs.iterator, allowArgInference = true)

      if (illegalUsed.nonEmpty) {
        error("One or more values were defined on the host but used in Accel without explicit transfer.")
        error("Use DRAMs with setMem to transfer data to the accelerator.")
        illegalUsed.take(5).foreach{case (in,use) =>
          error(in.ctx, s"Value ${in.name.getOrElse("defined here")}")
          error(in.ctx)
          error(use.ctx,s"First use in Accel occurs here.", noError = true)
          error(use.ctx)
        }
        if (illegalUsed.size > 5) error(s"(${illegalUsed.size - 5} values elided)")
      }

      super.visit(lhs,rhs)
    }

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

    case RegNew(init) if !inHw =>
      error(lhs.ctx, s"Register was created outside Accel. Use an ArgIn, ArgOut, or HostIO instead.")
      error(lhs.ctx)

    case op @ StreamInNew(bus)  => busWidthCheck(op.A,bus,"StreamIn")
    case op @ StreamOutNew(bus) => busWidthCheck(op.A,bus,"StreamOut")

    case LUTNew(dims,elems) =>
      val size = dims.map(_.toInt).product
      if (elems.length != size) {
        // TODO[5]: This could be downgraded to a warning if we fill zeros in here
        error(lhs.ctx, s"Total size of LUT ($size) does not match the number of supplied elements (${elems.length}).")
        error(lhs.ctx)
      }

    case _ => super.visit(lhs, rhs)
  }

}
