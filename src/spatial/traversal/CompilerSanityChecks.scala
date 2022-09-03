package spatial.traversal

import argon._
import spatial.lang._
import spatial.node._
import spatial.util.spatialConfig
import spatial.metadata.control._
import spatial.metadata.bounds._
import spatial.metadata.retiming._
import spatial.issues.ControlPrimitiveMix

/** Used to automatically detect invalid changes that occurred during transformations. */
case class CompilerSanityChecks(IR: State, enable: Boolean) extends AbstractSanityChecks {
  var visitedStms: Map[Sym[_],Option[Sym[_]]] = Map.empty
  var nestedScope: Set[Sym[_]] = Set.empty
  var parent: Option[Sym[_]] = None
  override def shouldRun: Boolean = enable

  override def postprocess[R](block: Block[R]): Block[R] = {
    visitedStms = Map.empty
    nestedScope = Set.empty
    if (IR.hadBugs) { throw CompilerErrors(s"${this.getClass} ${IR.pass}", IR.bugs) }
    super.postprocess(block)
  }

  override def visitBlock[R](block: Block[R]): Block[R] = {
    val saveScope = nestedScope
    nestedScope ++= block.inputs
    val result = super.visitBlock(block)
    nestedScope = saveScope
    result
  }

  def check[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    val inputs = rhs.nonBlockSymInputs
    val unknownInputs = (inputs.toSet diff nestedScope).filterNot { _.vecConst.nonEmpty }
    if (unknownInputs.nonEmpty) {
      bug(lhs.ctx, s"Statement $lhs = $rhs used one or more undefined values: ")
      unknownInputs.foreach { in =>
        bug("  " + stm(in) + s"  [${in.ctx}]")
      }
    }
    if (visitedStms.contains(lhs)) {
      bug(lhs.ctx, s"Statement $lhs = $rhs appears to be defined twice: ")
      bug(s"First here: ${visitedStms(lhs).map(stm).getOrElse("<Host>")}")
      bug(s"Then here: ${parent.map(stm).getOrElse("<Host>")}")
    }

    nestedScope ++= (rhs.binds + lhs)
    visitedStms += (lhs -> parent)

    rhs match {
      case AccelScope(block) =>
        val (stms, inputs) = block.nestedStmsAndInputs
        val illegalUsed = disallowedInputs(stms, inputs.iterator, allowArgInference = true)

        if (illegalUsed.nonEmpty) {
          bug("One or more values were defined on the host but used in Accel without explicit transfer.")
          illegalUsed.foreach { case (in, use) =>
            bug(in.ctx, stm(in))
            bug(use.ctx, s"First use in Accel occurs here.", noBug = true)
            bug(stm(use))
            bug(use.ctx)
          }
        }

      case _ => // Nothing
    }

    if (!spatialConfig.allowPrimitivesInOuterControl) rhs match {
      case ctrl:Control[_] if lhs.isOuterControl =>
        val blocks = ctrl.blocks
        blocks.foreach{block =>
          val stms = block.stms
          val ctrl = stms.filter{s => s.isControl && !(s.isBranch && s.isInnerControl) }
          val prim = stms.filter{s => s.isPrimitive && !s.isTransient }
          if (ctrl.nonEmpty && prim.nonEmpty) {
            raiseIssue(ControlPrimitiveMix(lhs,ctrl,prim))
          }
        }
      case _ =>
    }

    // check inner controllers to make sure that if latency is defined that they respect retimeGates
    rhs match {
      case ctrl: Control[_] if lhs.isInnerControl =>
        ctrl.blocks.foreach {
          block =>
            var mustOccurAfter: Double = -1
            var currentLatest: Double = 0
            var lastRetimeGate: Option[Sym[_]] = None
            block.stms.foreach {
              case rg if rg.isRetimeGate =>
                mustOccurAfter = currentLatest
                lastRetimeGate = Some(rg)
              case stmt if stmt.delayDefined =>
                val delay = stmt.fullDelay
                if (delay <= mustOccurAfter) {
                  error(s"$stmt occurred at $delay, but previous retimeGate ${lastRetimeGate} had delay $mustOccurAfter.")
                  error(s"$stmt was created at ${stmt.ctx}.")
                }
                currentLatest = scala.math.max(currentLatest, delay)
              case _ =>
            }
        }
      case _ =>
    }
  }

  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    val saveParent = parent
    parent = Some(lhs)
    rhs match {
      case AccelScope(_) => inAccel {check(lhs, rhs); super.visit(lhs, rhs) }
      case SpatialBlackboxImpl(block) =>
        if (lhs.parent match { case Ctrl.Host => false; case _ => true }) error(ctx, s"Please declare Blackbox implementation outside of Accel!")
        inBox { check(lhs,rhs); super.visit(lhs, rhs) }
      case SpatialCtrlBlackboxImpl(block) =>
        if (lhs.parent match { case Ctrl.Host => false; case _ => true }) error(ctx, s"Please declare Blackbox implementation outside of Accel!")
        inBox { check(lhs,rhs); super.visit(lhs, rhs) }
      case FIFODeq(_, _) | FIFOBankedDeq(_, _) =>
        val inputDelays = rhs.inputs.map(_.fullDelay)
        if (inputDelays.exists(_ > 1.0)) {
          val dbgDelays = (rhs.inputs zip inputDelays) map { case (i, d) => s"$i ($d)"}
          error(lhs.ctx, s"Found a banked dequeue ${lhs} with input delays ${dbgDelays.mkString(", ")}")
        }
      case SRAMRead(mem, addr, _) =>
        if (mem.rank != addr.size) {
          error(lhs.ctx, s"Found an SRAM Read $lhs = $rhs at ${lhs.ctx} with rank ${mem.rank} but had an address ${addr} of size ${addr.size}")
        }
      case SRAMWrite(mem, _, addr, _) =>
        if (mem.rank != addr.size) {
          error(lhs.ctx, s"Found an SRAM Write $lhs = $rhs at ${lhs.ctx} with rank ${mem.rank} but had an address ${addr} of size ${addr.size}")
        }
      case FIFOEnq(mem, data, _) =>
        if (mem.A.nbits != data.nbits) {
          error(lhs.ctx, s"Expected a write of size ${mem.A.nbits} but got $data of size ${data.nbits}")
        }
      case FIFOVecEnq(mem, data, _, _) =>
        mem match {
          case Op(FIFONew(size)) =>
            val intSize = size.c.get.toInt
            if (intSize < data.width) {
              error(lhs.ctx, s"Attempting a write of size ${data.width} to a FIFO of depth ${intSize}. This doesn't fit!")
            }
        }
      case FIFOVecDeq(mem, addr, _) =>
        mem match {
          case Op(FIFONew(size)) =>
            val intSize = size.c.get.toInt
            if (intSize < addr.size) {
              error(lhs.ctx, s"Attempting a dequeue of size ${addr.size} from a FIFO of depth ${intSize}. This will always stall!")
            }
        }

      case _ =>
        check(lhs, rhs)
        super.visit(lhs, rhs)
    }
    parent = saveParent
  }

}
