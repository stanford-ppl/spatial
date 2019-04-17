package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.metadata.control._
import spatial.metadata.retiming._
import spatial.metadata.types._
import spatial.lang._
import spatial.node._
import spatial.util.modeling._
import spatial.util.spatialConfig
import spatial.traversal.AccelTraversal

import scala.collection.immutable.SortedSet

case class RetimingTransformer(IR: State) extends MutateTransformer with AccelTraversal {
  override def shouldRun: Boolean = spatialConfig.enableRetiming

  var retimeBlocks: List[Boolean] = Nil
  var pushBlocks: List[Boolean] = Nil
  var lastLatency: Double = 0
  var ctx: Option[SrcCtx] = None

  var delayLines: Map[Sym[_], SortedSet[ValueDelay]] = Map.empty
  var delayConsumers: Map[Sym[_], List[ValueDelay]] = Map.empty
  var latencies: Map[Sym[_], Double] = Map.empty
  var cycles: Set[Sym[_]] = Set.empty
  var hierarchy: Int = 0
  var inInnerScope: Boolean = false

  def findDelayLines(scope: Seq[Sym[_]]): Seq[(Sym[_], ValueDelay)] = computeDelayLines(
    scope = scope,
    latencies = latencies,
    hierarchy = hierarchy,
    delayLines = delayLines,
    cycles = cycles,
    createLine = Some({(size: Int, data: Sym[_], ctx: SrcCtx) => delayLine(size,f(data))(ctx) })
  )

  def inBlock[A](block: Block[_])(func: => A): A = {
    val saveDelayLines = delayLines
    val saveDelayConsumers = delayConsumers
    val saveLatencies = latencies
    val saveCycles = cycles
    val saveInnerScope = inInnerScope
    hierarchy += 1
    inInnerScope = true

    val scope = block.stms
    val lines = findDelayLines(scope)
    lines.foreach{case (reader, line) => addDelayLine(reader, line) }

    val result = func

    hierarchy -= 1
    delayLines = saveDelayLines
    delayConsumers = saveDelayConsumers
    latencies = saveLatencies
    cycles = saveCycles
    inInnerScope = saveInnerScope
    result
  }

  def delayLine[A](size: Int, data: Sym[A])(implicit ctx: SrcCtx): Sym[A] = data.tp match {
    case Bits(bits) =>
      implicit val A: Bits[A] = bits
      val line = stage(DelayLine(size, data.view[Bits]))
      line.name = data.name.map{_ + s"_d$size"}
      line
    case _ => throw new Exception("Unexpected register type")
  }


  private def registerDelays(reader: Sym[_], inputs: Seq[Sym[_]]): Unit = {
    delayConsumers.getOrElse(reader, Nil)
      .filter{line => inputs.contains(line.input) }
      .map{line => line.input -> line }
      .toMap    // Unique-ify by line input - makes sure we only register one substitution per input
      .foreach{case (in, line) =>
        val dly = line.value()
        logs(s"  $in -> $dly [${line.size}]")
        register(in, dly)
      }
  }

  private def addDelayLine(reader: Sym[_], line: ValueDelay): Unit = {
    logs(s"Add {in:${line.input}, reader:$reader, delay:${line.delay}, size:${line.size}")

    val existing = delayLines.getOrElse(line.input, SortedSet[ValueDelay]())

    delayLines += line.input -> (existing + line)

    val prevReadByThisReader = delayConsumers.getOrElse(reader, Nil)
    delayConsumers += reader -> (line +: prevReadByThisReader)
  }


  /** Computes the delay lines which will be created within any blocks within the given operation.
    *
    * This is needed to allow multiple blocks to use the same delay line. Delay lines are created
    * lazily, which can lead to issues where multiple blocks claim the same symbol. To avoid this,
    * we instead create the symbol in the outermost block. Since we don't control when stageBlock
    * is called, we have to do this before symbol mirroring.
    *
    * Note that this requires checking block.nestedStms, not just block.stms.
    */
  private def precomputeDelayLines(op: Op[_]): Unit = {
    hierarchy += 1
    if (op.blocks.nonEmpty) logs(s"  Precomputing delay lines for $op")
    op.blocks.foreach{block =>
      val innerScope = block.nestedStms
      val lines = findDelayLines(innerScope).map(_._2)
      // Create all of the lines that need to be visible outside this block
      (lines ++ lines.flatMap(_.prev)).filter(_.hierarchy < hierarchy).foreach{line =>
        if (!line.alreadyExists) {
          val dly = line.value()
          logs(s"    ${line.input} -> $dly [size: ${line.size}, h: ${line.hierarchy}] (cur h: $hierarchy)")
        }
      }
    }
    hierarchy -= 1
  }
  override def updateNode[A](node: Op[A]): Unit = {
    if (inInnerScope) precomputeDelayLines(node)
    super.updateNode(node)
  }

  private def retimeStms[A](block: Block[A]): Sym[A] = inBlock(block) {
    inlineWith(block){stms =>
      stms.foreach{
        case Stm(switch, op: Switch[_]) => retimeSwitch(switch, op.asInstanceOf[Switch[Any]])
        case stm => retimeStm(stm)
      }
      f(block.result)
    }
  }

  private def retimeStm(sym: Sym[_]): Unit = sym match {
    case Stm(reader, d) =>
      logs(s"Retiming $reader = $d")
      val inputs = d.bitInputs
      val reader2 = isolateSubst(){
        registerDelays(reader, inputs)
        visit(sym)
        f(reader)
      }
      logs(s"  => ${stm(reader2)}")
      register(reader -> reader2)
    case _ =>
  }

  private def retimeSwitchCase[A](cas: Sym[A], op: SwitchCase[A], switch: Sym[A]): Unit = {
    implicit val A: Type[A] = cas.tp
    implicit val ctx: SrcCtx = cas.ctx
    val body = op.body
    precomputeDelayLines(op)
    dbgs(s"Retiming case ${stm(cas)}")
    // Note: Don't call inBlock here - it's already being called in retimeStms
    val caseBody2: Block[A] = isolateSubst(body.result){ stageBlock{
      retimeStms(body)
      val size = delayConsumers.getOrElse(switch, Nil).find(_.input == cas).map(_.size).getOrElse(0) +
                 delayConsumers.getOrElse(cas, Nil).find(_.input == body.result).map(_.size).getOrElse(0)
      if (size > 0) {
        dbgs(s"Adding retiming delay of size $size at end of case $cas")
        delayLine(size, f(body.result))
      }
      else {
        dbgs(s"No retiming delay required for case $cas")
        f(body.result)
      }
    }}
    val cas2 = stageWithFlow(SwitchCase(caseBody2)){cas2 => transferData(cas, cas2) }
    register(cas -> cas2)
  }

  private def retimeSwitch[A](switch: Sym[A], op: Switch[A]): Unit = {
    implicit val A: Type[A] = switch.tp
    val Switch(selects, body) = op
    val options = body.options
    precomputeDelayLines(op)
    dbgs(s"Retiming switch $switch = $op")
    val body2 = inBlock(body){
      stageBlock({
        inlineWith(body){stms =>
          stms.foreach {
            case Stm(cas, sc: SwitchCase[_]) => retimeSwitchCase(cas, sc.asInstanceOf[SwitchCase[Any]], switch)
            case stm => retimeStm(stm)
          }
          f(body.result)
        }
      }, options)
    }
    val switch2 = isolateSubst() {
      registerDelays(switch, selects)
      implicit val ctx: SrcCtx = switch.ctx
      stageWithFlow(Switch(f(selects), body2)){switch2 => transferData(switch, switch2) }
    }
    register(switch -> switch2)
  }

  private def retimeBlock[T](block: Block[T], saveLatency: Boolean)(implicit ctx: SrcCtx): Sym[T] = {
    val scope = block.nestedStms
    val result = (scope.flatMap{case Op(d) => d.blocks; case _ => Nil} :+ block).flatMap(exps(_))

    import spatial.metadata.access._
    import spatial.metadata.memory._
    dbgs(s"Retiming block $block:")
    scope.foreach{e => dbgs(s"  ${stm(e)} (${e.fullDelay})") }
    //dbgs(s"Result: ")
    //result.foreach{e => dbgs(s"  ${stm(e)}") }
    // The position AFTER the given node
    val (_, newCycles) = pipeLatencies(result, scope)
    val adjustedLatencies: Map[Sym[_], Double] = scope.map{s => (s -> (s.fullDelay + latencyOf(s, inReduce = cycles.contains(s))))}.toMap
    latencies ++= adjustedLatencies
    cycles ++= newCycles.flatMap(_.symbols)

    isolateSubst(){ retimeStms(block) }
  }


  def withRetime[A](wrap: List[Boolean], push: List[Boolean], srcCtx: SrcCtx)(x: => A): A = {
    val prevRetime = retimeBlocks
    val prevPush = pushBlocks
    val prevCtx = ctx

    retimeBlocks = wrap
    pushBlocks = push
    ctx = Some(srcCtx)
    val result = x

    retimeBlocks = prevRetime
    pushBlocks = prevPush
    ctx = prevCtx
    result
  }

  override protected def inlineBlock[T](b: Block[T]): Sym[T] = {
    val doWrap = retimeBlocks.headOption.getOrElse(false)
    val saveLatency = pushBlocks.headOption.getOrElse(false)
    if (retimeBlocks.nonEmpty) retimeBlocks = retimeBlocks.drop(1)
    if (pushBlocks.nonEmpty) pushBlocks = pushBlocks.drop(1)
    dbgs(s"Transforming Block $b [$retimeBlocks => $doWrap, $pushBlocks => $saveLatency]")
    if (doWrap) retimeBlock(b,saveLatency)(ctx.get)
    else super.inlineBlock(b)
  }

  private def transformCtrl[T:Type](lhs: Sym[T], rhs: Op[T])(implicit ctx: SrcCtx): Sym[T] = {
    // Switches aren't technically inner controllers from PipeRetimer's point of view.
    if (lhs.isInnerControl && !rhs.isSwitch && inHw) {
      val retimeEnables = rhs.blocks.map{_ => true }.toList
      val retimePushLaterBlock = rhs.blocks.map{_ => false }.toList
      rhs match {
        case _:StateMachine[_] => withRetime(retimeEnables, List(false,true,false), ctx) { super.transform(lhs, rhs) }
        case _ => withRetime(retimeEnables, retimePushLaterBlock, ctx) { super.transform(lhs, rhs) }
      }
      
    }
    else rhs match {
      case _:StateMachine[_] => withRetime(List(true,false,false), List(false,false,false), ctx){ super.transform(lhs, rhs) }
      case _ => if (inHw) withRetime(Nil, Nil, ctx){ super.transform(lhs, rhs) } else super.transform(lhs, rhs)
    }
  }

  override def transform[T:Type](lhs: Sym[T], rhs: Op[T])(implicit ctx: SrcCtx): Sym[T] = rhs match {
    case _:AccelScope => inAccel { transformCtrl(lhs,rhs) }
    case _ => transformCtrl(lhs, rhs)
  }
}
