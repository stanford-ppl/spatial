package spatial.transform

import argon._
import argon.node.Enabled
import argon.passes.RepeatableTraversal
import spatial.node._
import spatial.lang._
import argon.transform.MutateTransformer
import spatial.metadata.control._
import spatial.metadata.memory._

import scala.annotation.tailrec
import scala.collection.{mutable => cm}

case class FIFOAccessFusion(IR: State) extends MutateTransformer {
  def transformBlock[A](block: Block[A]): Block[A] = {
    // TODO(stanfurd): Fuse FIFODeqs
    val FIFOEnqs: cm.Map[Sym[_], (cm.ArrayBuffer[Sym[_]], Set[Bit], cm.ArrayBuffer[SrcCtx])] = cm.Map.empty

    def flushEnqs(mem: Sym[_]): Unit = {
      FIFOEnqs.remove(mem) match {
        case None =>
        case Some((writes, ens, ctxes)) =>
          dbgs(s"Flushing: $mem <- (${writes.mkString(", ")})[$ens]")
          val untypedFIFO = f(mem).asInstanceOf[FIFO[_]]
          type ET = untypedFIFO.A.R
          implicit def tEV: Bits[ET] = untypedFIFO.A.asInstanceOf[Bits[ET]]
          val data = writes.map(f(_)).map(_.asInstanceOf[ET])
          val typedFIFO = untypedFIFO.asInstanceOf[FIFO[ET]]
          if (data.size > 1) {
            val vecValue = Vec.fromSeq(data)
            stageWithFlow(FIFOVecEnq(typedFIFO, vecValue, Seq(I32(0)), f(ens))) {
              lhs2 =>
                if (ctxes.size == 1) {
                  lhs2.ctx = ctxes.head
                } else {
                  lhs2.ctx = implicitly[SrcCtx].copy(previous = ctxes)
                }
                dbgs(s"Result: $lhs2 = ${lhs2.op.get}")
            }
          } else {
            stageWithFlow(FIFOEnq(typedFIFO, data.head, f(ens))) {
              lhs2 =>
                lhs2.ctx = ctxes.head
                dbgs(s"Result: $lhs2 = ${lhs2.op.get}")
            }
          }
      }

    }

    @tailrec def append(mem: Sym[_], data:Seq[Sym[_]], ens: Set[Bit], ctx: SrcCtx): Unit = {
      FIFOEnqs.get(mem) match {
        case None =>
          FIFOEnqs(mem) = (cm.ArrayBuffer(data:_*), ens, cm.ArrayBuffer(ctx))
        case Some((current, oldEns, oldCtx)) if oldEns == ens =>
          current.appendAll(data)
          oldCtx.append(ctx)
        case Some((_, oldEns, _)) if oldEns != ens =>
          flushEnqs(mem)
          append(mem, data, ens, ctx)
      }
    }

    stageBlock {
      block.stms foreach {
        case stmt@Op(FIFOEnq(mem, data, ens)) =>
          dbgs(s"Appending: $stmt = ${stmt.op.get}")
          append(mem, Seq(data), ens, stmt.ctx)
          register(stmt -> Invalid)
        case stmt@Op(FIFOVecEnq(mem, data, _, ens)) =>
          dbgs(s"Appending: $stmt = ${stmt.op.get}")
          val components = data.elems.map(_.asInstanceOf[Sym[_]])
          append(mem, components, ens, stmt.ctx)
          register(stmt -> Invalid)
        case rt@Op(RetimeGate()) if FIFOEnqs.nonEmpty =>
          dbgs(s"Flushing due to RetimeGate $rt")
          FIFOEnqs.keys.foreach(flushEnqs)
          register(rt -> mirrorSym(rt))
        case stmt =>
          visit(stmt)
      }
      FIFOEnqs.keys.foreach(flushEnqs)
      f(block.result)
    }
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case ctrl:Control[_] if lhs.isInnerControl =>
      ctrl.blocks.foreach {
        block => register(block -> transformBlock(block))
      }
      update(lhs, rhs)
    case _ => super.transform(lhs, rhs)
  }
}
