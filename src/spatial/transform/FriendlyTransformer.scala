package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.traversal.AccelTraversal
import spatial.util._

case class FriendlyTransformer(IR: State) extends MutateTransformer with AccelTraversal {
  private var dimMapping: Map[I32,I32] = Map.empty
  private var addedArgIns: Seq[(Sym[_],Sym[_])] = Nil
  private var mostRecentWrite: Map[Reg[_], Sym[_]] = Map.empty

  def argIn[A](x: Bits[A]): Sym[A] = {
    implicit val bA: Bits[A] = x.selfType
    val arg: Reg[A] = stage(ArgInNew[A](bA.zero))
    dbg(s"Inserted ArgIn $arg for value $x")
    setArg(arg,x.unbox)
    arg.value
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(block) => inAccel {
      val rawInputs = block.nestedInputs
      val dataInputs = rawInputs.filterNot(_.isRemoteMem).filterNot(addedArgIns.contains)
      val bitsInputs = dataInputs.collect{case b: Bits[_] => b}

      // Add ArgIns for arbitrary bit inputs
      addedArgIns ++= bitsInputs.map{s => s -> argIn(f(s)) }

      isolateSubstWith(addedArgIns:_*){ super.transform(lhs,rhs) }
    }

    // Add ArgIns for DRAM dimensions
    case DRAMNew(ds,_) =>
      val dims = f(ds)
      dimMapping ++= dims.distinct.map{
        case d @ Op(RegRead(reg)) if reg.isArgIn => d -> d
        case d if d.isValue                      => d -> d
        case d if dimMapping.contains(d)         => d -> dimMapping(d)
        case d                                   => d -> argIn(d).unbox
      }
      val dims2 = dims.map{d => dimMapping(d) }
      addedArgIns ++= dims.zip(dims2)
      isolateSubstWith(dims.zip(dims2):_*){ super.transform(lhs,rhs) }

    case RegRead(F(reg)) if !inHw => getArg(reg)

    case RegWrite(F(reg),F(data),_) =>
      mostRecentWrite += reg -> data



    case SetReg(F(reg),F(data)) =>
      mostRecentWrite += reg -> data

      if (inHw && (reg.isArgOut || reg.isHostIO)) {
        (reg := data).asInstanceOf[Sym[A]]
      }
      else {
        error(ctx, "Setting ArgIn registers within Accel is disallowed. Use a HostIO or ArgOut.")
        error(ctx)
        err[A]("Set ArgIn in host")
      }

    case _ => super.transform(lhs,rhs)
  }
}
