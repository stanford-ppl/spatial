package spatial.transform

import argon._
import argon.node._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.retiming._
import spatial.traversal.AccelTraversal

case class AccumTransformer(IR: State) extends MutateTransformer with AccelTraversal {

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => inAccel{ transformControl(lhs,rhs) }
    case _ => transformControl(lhs,rhs)
  }

  def transformControl[A:Type](lhs: Sym[A], rhs: Op[A]): Sym[A] = rhs match {
    case ctrl: Control[_] if !(lhs.isSwitch && lhs.isInnerControl) =>
      ctrl.bodies.foreach{body =>
        if (body.isInnerStage || lhs.isInnerControl) {
          body.blocks.foreach{case (_, block) =>
            register(block -> optimizeAccumulators(block))
          }
        }
      }
      super.transform(lhs,rhs)
    case _ => super.transform(lhs,rhs)
  }


  def regAccumOp[A](reg: Reg[_], data: Bits[A], first: Bit, ens: Set[Bit], op: Accum, invert: Boolean): Void = {
    implicit val A: Bits[A] = data.selfType
    val trueFirst = if (invert) !first else first
    stage(RegAccumOp(reg.asInstanceOf[Reg[A]],data,ens,op,trueFirst))
  }
  def regAccumFMA[A](reg: Reg[_], m0: Bits[A], m1: Bits[_], first: Bit, ens: Set[Bit], invert: Boolean): Void = {
    implicit val A: Bits[A] = m0.selfType
    val trueFirst = if (invert) !first else first
    stage(RegAccumFMA(reg.asInstanceOf[Reg[A]],m0,m1.asInstanceOf[Bits[A]],ens,trueFirst))
  }

  def optimizeAccumulators[R](block: Block[R]): Block[R] = {
    val stms = block.stms
    val (cycles, nonCycles) = stms.partition{s => s.isInCycle && s.reduceCycle.shouldSpecialize }

    // We assume here that the analysis has marked only closed accumulation cycles for specialization
    // We define a cycle as closed if:
    //   1. It overlaps with no other accumulation cycles
    //   2. No partial outputs are used outside of the cycle
    //
    // This allows us to treat the graph as one chunk of compute with zero or more consumer cycles
    // effectively at the end.
    stageScope(f(block.inputs), block.options){
      nonCycles.foreach(visit)

      cycles.groupBy(_.reduceCycle.cycleID).values.filterNot(_.isEmpty).foreach{syms: Seq[Sym[_]] =>
        syms.head.reduceCycle match {
          case WARCycle(reader,writer,mem,_,len,_,_) => writer match {
            case RegAccumlike(RegAccum_Op(reg,data,first,ens,op,invert)) =>
              val result = regAccumOp(reg,data,first,ens,op,invert)
              register(writer -> result)
              result

            case RegAccumlike(RegAccum_FMA(reg,m0,m1,first,ens,invert)) =>
              val result = regAccumFMA(reg,m0,m1,first,ens,invert)
              register(writer -> result)
              result

            // TODO: General lambda case
            //case RegAccumlike(RegAccum_Gen) =>
            //  val block = stageBlock{ syms.foreach(visit) }
            //  stage(RegAccumLambda(reg,))
            case _ => syms.foreach(visit)
          }
          case _ => syms.foreach(visit)
        }
      }

      f(block.result)
    }
  }

  private object RegAdd {
    def unapply(s: Sym[_]): Option[(Reg[_], Bits[_])] = s match {
      case Op(FixAdd(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FixAdd(data, Op(RegRead(reg)))) => Some((reg,data))
      case Op(FltAdd(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FltAdd(data, Op(RegRead(reg)))) => Some((reg,data))
      case _ => None
    }
  }

  private object RegMul {
    def unapply(s: Sym[_]): Option[(Reg[_], Bits[_])] = s match {
      case Op(FixMul(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FixMul(data, Op(RegRead(reg)))) => Some((reg,data))
      case Op(FltMul(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FltMul(data, Op(RegRead(reg)))) => Some((reg,data))
      case _ => None
    }
  }

  private object RegMin {
    def unapply(s: Sym[_]): Option[(Reg[_], Bits[_])] = s match {
      case Op(FixMin(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FixMin(data, Op(RegRead(reg)))) => Some((reg,data))
      case Op(FltMin(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FltMin(data, Op(RegRead(reg)))) => Some((reg,data))
      case _ => None
    }
  }

  private object RegMax {
    def unapply(s: Sym[_]): Option[(Reg[_], Bits[_])] = s match {
      case Op(FixMax(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FixMax(data, Op(RegRead(reg)))) => Some((reg,data))
      case Op(FltMax(Op(RegRead(reg)), data)) => Some((reg,data))
      case Op(FltMax(data, Op(RegRead(reg)))) => Some((reg,data))
      case _ => None
    }
  }

  private object RegFMA {
    def unapply(s: Sym[_]): Option[(Reg[_], Bits[_], Bits[_])] = s match {
      case Op(FixFMA(m0, m1, Op(RegRead(reg)))) => Some((reg,m0,m1))
      case Op(FltFMA(m0, m1, Op(RegRead(reg)))) => Some((reg,m0,m1))
      case _ => None
    }
  }

  private object RegMux {
    def unapply(s: Sym[_]): Option[(Bit, Reg[_], Bits[_], Boolean)] = s match {
      case Op(Mux(sel, Op(RegRead(reg)), data)) => Some((sel, reg, data, false))
      case Op(Mux(sel, data, Op(RegRead(reg)))) => Some((sel, reg, data, true))
    }
  }

  // TODO[5]: Is there a better way to write these patterns?
  sealed abstract class RegAccumlike
  case class RegAccum_Op(reg: Reg[_], data: Bits[_], first: Bit, ens: Set[Bit], op: Accum, invert: Boolean) extends RegAccumlike
  case class RegAccum_FMA(reg: Reg[_], m0: Bits[_], m1: Bits[_], first: Bit, ens: Set[Bit], invert: Boolean) extends RegAccumlike
  case object RegAccum_Gen extends RegAccumlike
  object RegAccumlike {
    def unapply(writer: Sym[_]): Option[RegAccumlike] = writer match {
      case Op(RegWrite(reg,update,ens)) => update match {
        // Specializing sums
        case RegAdd(`reg`,data) => Some(RegAccum_Op(reg,data,false,ens,Accum.Add,invert=false))
        case RegMux(first,`reg`,RegAdd(`reg`,data),invert) => Some(RegAccum_Op(reg,data,first,ens,Accum.Add,invert))

        // Specializing product
        case RegMul(`reg`,data) => Some(RegAccum_Op(reg,data,false,ens,Accum.Mul,invert=false))
        case RegMux(first,`reg`,RegMul(`reg`,data),invert) => Some(RegAccum_Op(reg,data,first,ens,Accum.Mul,invert))

        // Specializing min
        case RegMin(`reg`,data) => Some(RegAccum_Op(reg,data,false,ens,Accum.Min,invert=false))
        case RegMux(first,`reg`,RegMin(`reg`,data),invert) => Some(RegAccum_Op(reg,data,first,ens,Accum.Min,invert))

        // Specializing max
        case RegMax(`reg`,data) => Some(RegAccum_Op(reg,data,false,ens,Accum.Max,invert=false))
        case RegMux(first,`reg`,RegMax(`reg`,data),invert) => Some(RegAccum_Op(reg,data,first,ens,Accum.Max,invert))

        // Specializing FMA
        case RegFMA(`reg`,m0,m1) => Some(RegAccum_FMA(reg,m0,m1,false,ens,invert=false))
        case RegMux(first,`reg`,RegFMA(`reg`,m0,m1),invert) => Some(RegAccum_FMA(reg,m0,m1,first,ens,invert))
        case _ => Some(RegAccum_Gen)
      }
      case _ => None
    }
  }

}
