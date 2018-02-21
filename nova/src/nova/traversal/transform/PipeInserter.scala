package nova.traversal.transform

import core._
import nova.data._
import spatial.lang._
import spatial.node._
import nova.util._
import core.implicits.mtypes._
import core.transform.MutateTransformer

import scala.collection.mutable.ArrayBuffer

case class PipeInserter(IR: State) extends MutateTransformer {
  private class Stage(val inner: Boolean) {
    val nodes  = ArrayBuffer[Sym[_]]()
    def outer: Boolean = !inner
    def dump(i: Int): Unit = {
      dbgs(s"Stage #$i: " + (if (inner) "[Inner]" else "[Outer]"))
      nodes.foreach{s => dbgs(s"  ${stm(s)}") }
    }
    lazy val inputs: Set[Sym[_]] = nodes.toSet.flatMap{s: Sym[_] => s.nestedInputs}
  }
  private object Stage {
    def outer = new Stage(inner = false)
    def inner = new Stage(inner = true)
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    if (isOuterControl(lhs)) {
      rhs.blocks.foreach{block =>
        register(block -> insertPipes(block))
      }
    }
    super.transform(lhs,rhs)
  }

  protected def insertPipes[R](block: Block[R]): Block[R] = stageBlock({
    val stages = ArrayBuffer[Stage]()
    def curStage: Stage = stages.last
    def nextInnerStage: Stage = {
      if (curStage.outer) { stages += Stage.inner }
      curStage
    }
    def nextOuterStage: Stage = {
      if (curStage.inner) { stages += Stage.outer }
      curStage
    }
    stages += Stage.outer

    block.stms.foreach{
      case Stateless(s) =>
        val i = stages.lastIndexWhere{stage =>
          (stage.nodes intersect s.dataInputs).nonEmpty
        }
        val stage = if (i >= 0) stages(i) else stages.head
        stage.nodes += s

      case Alloc(s)     => nextOuterStage.nodes += s
      case Primitive(s) => nextInnerStage.nodes += s
      case Control(s)   => nextOuterStage.nodes += s
    }

    stages.zipWithIndex.foreach{
      case (stg,i) if stg.inner =>
        stg.dump(i)
        val outs = stg.nodes.filter{s => !s.isVoid }
        val escaping = outs.filter{s => stages.drop(i+1).exists{stage =>
          stage.inputs.contains(s)
        }}
        val escapingBits = escaping.collect{case s: Bits[_] => s}
        val escapingVars = escaping.collect{case s if !s.isBits => s}
        dbgs(s"Escaping bits: ")
        escapingBits.foreach{s => s"  ${stm(s)}"}
        dbgs(s"Escaping vars: ")
        escapingVars.foreach{s => s"  ${stm(s)}"}

        val regs = escapingBits.map{s => regNew(s) }
        val vars = escapingVars.map{s => varNew(s) }

        implicit val ctx: SrcCtx = SrcCtx.empty
        val pipe = stage(UnitPipe(Nil,stageBlock{
          isolateSubst{
            stg.nodes.foreach(visit)
            escapingBits.zip(regs).foreach{case (s,reg) => regWrite(reg,s) }
            escapingVars.zip(vars).foreach{case (s,vrr) => varWrite(vrr,s) }
            Void.c
          }
        }))
        isOuter(pipe) = false
        escapingBits.zip(regs).foreach{case (s,reg) => register(s -> regRead(reg)) }
        escapingVars.zip(vars).foreach{case (s,vrr) => register(s -> varRead(vrr)) }

      case (stg,i) if stg.outer =>
        stg.dump(i)
        stg.nodes.foreach(visit)
    }

    (block.result match {
      case _:Void => void
      case s      => f(s)
    }).asInstanceOf[Sym[R]]
  }, block.options)

  def regNew(s: Bits[_]): Reg[_] = Reg.alloc(s.zero(s.ctx,state))(mbits(s),s.ctx,state)
  def varNew(s: Sym[_]): Var[_] = Var.alloc(None)(mtyp(s.tp),s.ctx,state)
  def regWrite(x: Reg[_], data: Bits[_]): Unit = Reg.write(x.asInstanceOf[Reg[Any]],data)(data.mtp,data.ctx,state)
  def varWrite(x: Var[_], data: Sym[_]): Unit = Var.assign(x.asInstanceOf[Var[Any]],data)(data.mtp,data.ctx,state)
  def regRead(x: Reg[_]): Sym[_] = Reg.read(x)(mbits(x.tA),SrcCtx.empty,state).asInstanceOf[Sym[_]]
  def varRead(x: Var[_]): Sym[_] = Var.read(x)(mtyp(x.tA),SrcCtx.empty,state).asInstanceOf[Sym[_]]
}
