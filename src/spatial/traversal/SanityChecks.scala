package spatial.traversal

import argon._
import argon.passes.Traversal
import emul.Number

import spatial.data._
import spatial.lang._
import spatial.node._
import spatial.util._

case class SanityChecks(IR: State) extends Traversal with AccelTraversal {
  override val recurse: Recurse = Recurse.Always

  def disallowedInputs(stms: Set[Sym[_]], ins: Iterator[Sym[_]]): Iterator[(Sym[_],Sym[_])] = {
    ins.filterNot(_.isRemoteMem)                // Remote memories are assumed to be shared
       .filterNot{_.tp.isInstanceOf[Bits[_]]}   // We can infer ArgIns (see FriendlyTransformer)
       .filterNot{_.tp.isInstanceOf[Text]}      // TODO[3]: Debug only?
       .flatMap{in => stms.find(_.inputs.contains(in)).map{use => (in,use) }}
  }

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
      val illegalUsed = disallowedInputs(stms,rawInputs.iterator)

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
    }

    case CounterNew(_,_,Literal(step: Number),_) if step === 0 =>
      error(lhs.ctx, "Counter has step size of 0.")
      error("This will create a counter with infinite iterations, which is probably not what you wanted.")
      error("(If this is what you wanted, use the forever counter '*' notation instead.)")
      error(lhs)

    case RegNew(init) if !init.isFixedBits =>
      error(lhs.ctx, s"Reset value of register ${lhs.fullname} was not a constant.")
      error(lhs.ctx)

    case op @ StreamInNew(bus)  => busWidthCheck(op.A,bus,"StreamIn")
    case op @ StreamOutNew(bus) => busWidthCheck(op.A,bus,"StreamOut")

    case LUTNew(dims,elems) =>
      val size = dims.map(_.toInt).product
      if (elems.length != size) {
        // TODO[5]: This could be downgraded to a warning if we fill zeros in here
        error(ctx, s"Total size of LUT ($size) does not match the number of supplied elements (${elems.length}).")
        error(ctx)
      }

    case _ => super.visit(lhs, rhs)
  }

}
