package spatial.traversal

import core._
import core.passes.Traversal
import emul.Number

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

  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case GetArgOut(_) if inHw =>
      error(lhs.src, "Reading ArgOuts within Accel is disallowed.")
      error("Use a Reg to store intermediate values.")
      error(lhs.src)

    case SetArgIn(arg,_) if inHw =>
      error(lhs.src, "Writing ArgIn within Accel is disallowed.")
      error("Use a Reg to store intermediate values.")
      error(lhs.src)

    case AccelScope(block) => inAccel {
      val (stms,rawInputs) = block.nestedStmsAndInputs
      val illegalUsed = disallowedInputs(stms,rawInputs.iterator)

      if (illegalUsed.nonEmpty) {
        error("One or more values were defined on the host but used in Accel without explicit transfer.")
        error("Use DRAMs with setMem to transfer data to the accelerator.")
        illegalUsed.take(5).foreach{case (in,use) =>
          error(in.src, s"Value ${in.name.getOrElse("defined here")}")
          error(in.src)
          error(use.src,s"First use in Accel occurs here.", noError = true)
          error(use.src)
        }
        if (illegalUsed.size > 5) error(s"(${illegalUsed.size - 5} values elided)")
      }
    }

    case CounterNew(_,_,Const(step: Number),_) if step === 0 =>
      error(lhs.src, "Counter has step size of 0.")
      error("This will create a counter with infinite iterations, which is probably not what you wanted.")
      error("(If this is what you wanted, use the forever counter '*' notation instead.)")
      error(lhs)

    case RegNew(init) if !init.isConst =>
      error(lhs.src, "Reset values of registers must be constants.")
      error(lhs.src)

    case _ => super.visit(lhs, rhs)
  }

}
