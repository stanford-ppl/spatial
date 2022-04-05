package spatial.transform

import argon._
import argon.node._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.types._
import spatial.util.spatialConfig
import spatial.traversal.BlkTraversal

import scala.collection.mutable.ArrayBuffer

case class PipeInserter(IR: State) extends MutateTransformer with BlkTraversal {
  var enable: Set[Bit] = Set.empty

  def withEnable[T](en: Bit)(blk: => T)(implicit ctx: SrcCtx): T = {
    val saveEnable = enable
    enable = enable + en
    val result = blk
    enable = saveEnable
    result
  }


  private class Stage(val inner: Boolean) {
    val nodes: ArrayBuffer[Sym[_]] = ArrayBuffer[Sym[_]]()

    def outer: Boolean = !inner

    def dump(id: Int, i: Int): Unit = {
      dbgs(s"Stage #$id, $i: " + (if (inner) "[Inner]" else "[Outer]"))
      nodes.foreach { s => dbgs(s"  ${stm(s)}") }
    }

    lazy val inputs: Set[Sym[_]] = nodes.toSet.flatMap { s: Sym[_] => s.nestedInputs }
  }

  private object Stage {
    def outer = new Stage(inner = false)
    def inner = new Stage(inner = true)
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case switch @ Switch(F(selects), _) if lhs.isOuterControl && inHw =>
      val res: Option[Either[LocalMem[A,C forSome{type C[_]}],Var[A]]] = if (Type[A].isVoid) None else Some(resFrom(lhs, lhs))

      val cases = (switch.cases,selects,lhs.children).zipped.map { case (SwitchCase(body), sel, swcase) =>
        val controllers = swcase.children
        val primitives = body.stms.collect{case Primitive(s) => s }
        val requiresWrap = primitives.nonEmpty && controllers.nonEmpty

        () => withEnable(sel) {
          val body2: Block[Void] = {
            if (requiresWrap) wrapSwitchCase(body, lhs, res)
            else stageScope(f(body.inputs),body.options){ insertPipes(body, lhs, res, scoped = false).right.get }
          }
          Switch.op_case(body2)
        }
      }
      val switch2: Void = transferDataToAllNew(lhs){ Switch.op_switch(selects, cases) }

      res match {
        case Some(r) => resRead(r)                    // non-void
        case None    => switch2.asInstanceOf[Sym[A]]  // Void case
      }

    case ctrl:Control[_] =>
      inCtrl(lhs) {
        if (lhs.isOuterControl && inHw) {
          dbgs(s"$lhs = $rhs")
          ctrl.bodies.zipWithIndex.foreach { case (body, id) =>
            val stage = Ctrl.Node(lhs, id)
            dbgs(s"  $lhs Body #$id: ")
            body.blocks.zipWithIndex.foreach{case ((_,block),bid) =>
              dbgs(s"    $lhs body #$id block #$bid [" + (if (stage.mayBeOuterBlock) "Outer]" else "Inner]"))
              state.logTab += 1
              // Register substitutions for outer control blocks
              if (stage.mayBeOuterBlock) {
                register(block -> insertPipes(block, lhs).left.get)
              }
              state.logTab -= 1
            }
          }
        }
      }
      super.transform(lhs, rhs)

    case box@SpatialCtrlBlackboxImpl(func) =>
      inCtrl(lhs) {
        dbgs(s"$lhs = $rhs")
        register(func -> insertPipes(func, lhs).left.get)
      }
      super.transform(lhs, rhs)

    case _ => super.transform(lhs, rhs)
  }

  def wrapSwitchCase[A:Type](body: Block[A], parent: Sym[_], res: Option[Either[LocalMem[A,C forSome{type C[_]}],Var[A]]])(implicit ctx: SrcCtx): Block[Void] = {
    stageScope(f(body.inputs), body.options){
      Pipe(enable, {
        insertPipes(body, parent, res, scoped = false).right.get
      })
    }
  }

  private def allStms(s: Stage): Set[Sym[_]] = {
    s.nodes.flatMap(_.blocks.flatMap(_.nestedStms)).toSet ++ s.nodes.toSet
  }

  protected def insertPipes[R](block: Block[R], parent: Sym[_], res: Option[Either[LocalMem[R,C forSome{type C[_]}],Var[R]]] = None, scoped: Boolean = true): Either[Block[R],Sym[Void]] = {
    val blk = stageScopeIf(scoped, f(block.inputs), block.options){
      val stages = ArrayBuffer[Stage]()
      val boundStages = ArrayBuffer[ArrayBuffer[Stage]]()
      def computeStages(): Unit = {
        block.stms.foreach{
          case Transient(s) =>
            val i = stages.lastIndexWhere{stage =>
              // Nodes which produce this value, or nodes which write to a mem read by this sym.
              // this second case is basically a special-case for RegReads, which are transient, but ordered.
              val stageEffects = stage.nodes// ++ stage.nodes.flatMap(sym => sym.effects.writes)
              (stageEffects intersect s.inputs).nonEmpty
            }
            val stage = if (i >= 0) stages(i) else stages.head
            stage.nodes += s
            dbgs(s"inline: $s = ${s.op}, inputs: ${s.inputs}")

          case Alloc(s)      => nextOuterStage.nodes += s; dbgs(s"Alloc: $s = ${s.op}")
          case Primitive(s)  => nextInnerStage.nodes += s; dbgs(s"Primitive: $s = ${s.op}")
          case Control(s)    => nextOuterStage.nodes += s; dbgs(s"Control: $s = ${s.op}")
          case FringeNode(s) => nextOuterStage.nodes += s; dbgs(s"Fringe: $s = ${s.op}")
        }
      }
      def bindStages(): Unit = {
        if (parent.isParallel || parent.isStreamControl) {
          dbgs(s"Binding stages $stages for Parallel execution:")
          stages.dropRight(1).zipWithIndex.foreach{
            case (stg,i) if stg.inner => 
              dbgs(s" - Inner stage $stg $i")
              val calculated = stg.nodes.toSet
              val escaping = stg.nodes.filter{s =>
                val used = s.consumers diff calculated
                !s.tp.isVoid && (s == block.result || used.nonEmpty)
              }

              dbgs(s"   Stage nodes: ")
              stg.nodes.foreach{x => dbgs(s"   - ${stm(x)}")}
              dbgs(s"   Escaping: ")
              escaping.foreach{x =>  dbgs(s"   - ${stm(x)}")}

              val nextStageConsumes = escaping.flatMap(_.consumers).toSet intersect allStms(stages(i+1))
              if (nextStageConsumes.nonEmpty && stages(i+1).outer) {
                dbgs(s"Stage $i should bind with stage ${i+1}")
                boundStages += ArrayBuffer(stg, stages(i+1))
              }
              else boundStages += ArrayBuffer(stg)
            case (stg, i) if stg.outer => 
              dbgs(s" - Outer stage $stg $i")
              if (!boundStages.flatten.contains(stg)) boundStages += ArrayBuffer(stg)
          }
          dbgs(s" - Last stage ${stages.last}")
          if (!boundStages.flatten.contains(stages.last)) boundStages += ArrayBuffer(stages.last)
        } 
        else {
          stages.foreach{stg => boundStages += ArrayBuffer(stg)}
        }
      }

      def wrapInner(stgs: ArrayBuffer[Stage], id: Int): Unit = {
        stgs.zipWithIndex.foreach{
          case (stg,i) if stg.inner =>
            stg.dump(id,i)
            val calculated = stg.nodes.toSet
            val escaping = stg.nodes.filter{s =>
              val used = s.consumers diff calculated
              dbgs(s"  ${stm(s)}, stg $stg")
              dbgs(s"    uses: $used")
              dbgs(s"    nonVoid: ${!s.tp.isVoid}")
              dbgs(s"    isResult: ${s == block.result}")
              !s.tp.isVoid && (s == block.result || used.nonEmpty)
            }

            val escapingHolders = escaping.map{
              case s if s == block.result && res.isDefined => res.get
              case s => resFrom(s, parent)
            }

            Pipe {
              isolateSubst() {
                stg.nodes.foreach(visit)
                escaping.zip(escapingHolders).foreach{case (s, r) => resWrite(r,s) }
              }
            }
            dbgs(s"Escaping: ")
            escaping.zip(escapingHolders).foreach{case (s,r) =>
              val rd = resRead(r)
              dbgs(s"  ${stm(s)}")
              dbgs(s"  => ${stm(rd)}")
              register(s, rd)
            }

//            dbgs(s"Replacements: $subst")

          case (stg,i) if stg.outer =>
            stg.dump(id,i)
            stg.nodes.foreach(visit)
        }
      }


      def curStage: Stage = stages.last
      def nextInnerStage: Stage = {
        if (curStage.outer) { stages += Stage.inner }
        curStage
      }
      def nextOuterStage: Stage = {
        // if (curStage.inner) { stages += Stage.outer }
        stages += Stage.outer 
        curStage
      }
      stages += Stage.outer

      // Assign stms to stages
      computeStages()

      // Bind inserted pipes with existing controllers
      bindStages()
      dbgs(s"stage bindings: $boundStages")

      // Transform
      boundStages.zipWithIndex.foreach{
        case (stgs,id) if stgs.size > 1 => 
          implicit val ctx: SrcCtx = stgs.head.nodes.headOption.map { _.ctx }.getOrElse(SrcCtx.empty)
          Pipe{wrapInner(stgs,id)}
        case (stgs,id) if stgs.size == 1 && stgs.head.inner => 
          wrapInner(stgs,id)
        case (stgs,id) if stgs.size == 1 && stgs.head.outer => 
          stgs.head.dump(id,0)
          stgs.head.nodes.foreach(visit)
      }


      (block.result match {
        case _:Void => void
        case s      => f(s)
      }).asInstanceOf[Sym[R]]
    }

    blk match {
      case Left(b)  => Left(b)
      case Right(_) => Right(void)
    }
  }

  def resFrom[A](s: Sym[A], parent: Sym[_]): Either[LocalMem[A,C forSome{type C[_]}],Var[A]] = s match {
    case b: Bits[_] if parent.isStreamControl => Left(memFrom(b.asInstanceOf[Bits[A]], true))
    case b: Bits[_] =>                           Left(memFrom(b.asInstanceOf[Bits[A]]))
    case _          =>                           Right(varFrom(s))
  }
  def resWrite[A](x: Either[LocalMem[_,C forSome{type C[_]}],Var[_]], d: Sym[A]): Void = x match {
    case Left(reg)  => memWrite(reg.asInstanceOf[LocalMem[A,C forSome{type C[_]}]], d.asInstanceOf[Bits[A]])
    case Right(vrr) => varWrite(vrr.asInstanceOf[Var[A]], d)
  }
  def resRead[A](x: Either[LocalMem[A,C forSome{type C[_]}],Var[A]]): Sym[A] = x match {
    case Left(reg)  => memRead(reg)
    case Right(vrr) => varRead(vrr)
  }


  def memFrom[A](s: Bits[A], inStream: Boolean = false): LocalMem[A,C forSome{type C[_]}] = {
    implicit val ctx: SrcCtx = state.ctxWithPass(s.ctx)
    implicit val tA: Bits[A] = s.tp.view[Bits]
    if (inStream) FIFOReg.alloc[A](s.zero).asInstanceOf[LocalMem[A,C forSome{type C[_]}]]
    else          Reg.alloc[A](s.zero).asInstanceOf[LocalMem[A,C forSome{type C[_]}]]
  }
  def memRead[A](x: LocalMem[A,C forSome{type C[_]}], inStream: Boolean = false): Sym[A] = {
    implicit val ctx: SrcCtx = state.ctxWithPass(x.ctx)
    implicit val tA: Bits[A] = x.A
    (x.asSym match {
      case fr: FIFOReg[A] => fr.deq()
      case reg: Reg[A] => reg.value
    }).asSym
//    if (x.isInstanceOf[FIFOReg[A]]) tA.box(FIFOReg.deq(x.asInstanceOf[FIFOReg[A]]))
//    else                         tA.box(Reg.read(x.asInstanceOf[Reg[A]]))
    
  }
  def memWrite[A](x: LocalMem[A,C forSome{type C[_]}], data: Bits[A]): Unit = {
    implicit val ctx: SrcCtx = state.ctxWithPass(x.ctx)
    x.asSym match {
      case fr: FIFOReg[A] => fr.enq(data.unbox)
      case reg: Reg[A] => reg.write(data.unbox)
    }
//    if (x.isInstanceOf[FIFOReg[_]]) FIFOReg.enq(x.asInstanceOf[FIFOReg[A]],data)
//    else                         Reg.write(x.asInstanceOf[Reg[A]],data)
  }

  def varFrom[A](s: Type[A])(implicit ctx: SrcCtx): Var[A] = {
    implicit val tA: Type[A] = s
    Var.alloc[A](None)
  }
  def varFrom[A](s: Sym[A]): Var[A] = {
    implicit val ctx: SrcCtx = s.ctx
    varFrom(s.tp)
  }
  def varRead[A](x: Var[A]): Sym[A] = {
    implicit val ctx: SrcCtx = x.ctx
    implicit val tA: Type[A] = x.A
    Var.read(x)
  }
  def varWrite[A](x: Var[A], data: Sym[A]): Unit = {
    implicit val tA: Type[A] = x.A
    Var.assign(x,data.unbox)
  }


  override def postprocess[R](block: Block[R]): Block[R] = {
    // Not allowed to have mixed control and primitives after unit pipe insertion
    spatialConfig.allowPrimitivesInOuterControl = false
    super.postprocess(block)
  }

}
