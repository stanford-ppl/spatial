package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.data._
import spatial.lang._
import spatial.node._
import spatial.util._
import spatial.internal.spatialConfig
import spatial.traversal.AccelTraversal

import scala.collection.immutable.SortedSet

case class RetimingTransformer(IR: State) extends MutateTransformer with AccelTraversal {
  override def shouldRun: Boolean = spatialConfig.enableRetiming

  var retimeBlocks: List[Boolean] = Nil
  var ctx: Option[SrcCtx] = None

  var delayLines: Map[Sym[_], SortedSet[ValueDelay]] = Map.empty
  var delayConsumers: Map[Sym[_], List[ValueDelay]] = Map.empty
  var latencies: Map[Sym[_], Double] = Map.empty
  var cycles: Set[Sym[_]] = Set.empty
  var hierarchy: Int = 0
  var inInnerScope: Boolean = false

  def delayOf(x: Sym[_]): Double = latencies.getOrElse(x, 0.0)

  def inBlock[A](block: Block[_])(func: => A): A = {
    val saveDelayLines = delayLines
    val saveDelayConsumers = delayConsumers
    val saveLatencies = latencies
    val saveCycles = cycles
    val saveInnerScope = inInnerScope
    hierarchy += 1
    inInnerScope = true

    val scope = block.stms
    val lines = computeDelayLines(scope)
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

  // Round out to nearest 1/1000 because numbers like 1.1999999997 - 0.2 < 1.0 and screws things up
  def scrubNoise(x: Double): Double = {
    if ( (x*1000) % 1 == 0) x
    else if ( (x*1000) % 1 < 0.5) (x*1000).toInt.toDouble/1000.0
    else ((x*1000).toInt + 1).toDouble/1000.0
  }
  def requiresRegisters(x: Sym[_]): Boolean = latencyModel.requiresRegisters(x, cycles.contains(x))
  def retimingDelay(x: Sym[_]): Double = if (requiresRegisters(x)) latencyOf(x, cycles.contains(x)) else 0.0

  def bitBasedInputs(d: Op[_]): Seq[Sym[_]] = exps(d).filter{_.isBits}.toSeq

  def delayLine[A](size: Int, data: Sym[A])(implicit ctx: SrcCtx): Sym[A] = data.tp match {
    case Bits(bits) =>
      implicit val A: Bits[A] = bits
      val line = stage(DelayLine(size, data.view[Bits]))
      line.name = data.name.map{_ + s"_d$size"}
      line
    case _ => throw new Exception("Unexpected register type")
  }

  case class ValueDelay(input: Sym[_], delay: Int, size: Int, hierarchy: Int, prev: Option[ValueDelay], private val create: () => Sym[_]) {
    private var reg: Option[Sym[_]] = None
    def alreadyExists: Boolean = reg.isDefined
    def value(): Sym[_] = reg.getOrElse{val r = create(); reg = Some(r); r }
  }

  implicit object ValueDelayOrdering extends Ordering[ValueDelay] {
    override def compare(x: ValueDelay, y: ValueDelay): Int = implicitly[Ordering[Int]].compare(y.delay,x.delay)
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

  private def computeDelayLines(scope: Seq[Sym[_]]): Seq[(Sym[_], ValueDelay)] = {
    val innerScope = scope.flatMap(_.blocks.flatMap(_.stms)).toSet

    def createValueDelay(input: Sym[_], reader: Sym[_], delay: Int): ValueDelay = {
      if (delay < 0) {
        bug("Compiler bug? Attempting to create a negative delay between input: ")
        bug(s"  ${stm(input)}")
        bug("and consumer: ")
        bug(s"  ${stm(reader)}")
        state.logBug()
      }
      // Retime inner block results as if we were already in the inner hierarchy
      val h = if (innerScope.contains(input)) hierarchy + 1 else hierarchy
      val existing = delayLines.getOrElse(input, SortedSet[ValueDelay]())
      existing.find{_.delay <= delay} match {
        case Some(prev) =>
          val size = delay - prev.delay
          if (size > 0) {
            logs(s"    Extending existing line of ${prev.delay}")
            ValueDelay(input, delay, size, h, Some(prev), () => delayLine(size, prev.value())(input.ctx))
          }
          else {
            logs(s"    Using existing line of ${prev.delay}")
            prev
          }

        case None =>
          logs(s"    Created new delay line of $delay")
          ValueDelay(input, delay, delay, h, None, () => delayLine(delay, f(input))(input.ctx))
      }
    }

    val consumerDelays = scope.flatMap{case Stm(reader, d) =>
      val inReduce = cycles.contains(reader)
      val criticalPath = scrubNoise(delayOf(reader) - latencyOf(reader, inReduce))  // All inputs should arrive at this offset

      // Ignore non-bit based values
      val inputs = bitBasedInputs(d) //diff d.blocks.flatMap(blk => exps(blk))

      dbgs(s"[Arrive = Dly - Lat: $criticalPath = ${delayOf(reader)} - ${latencyOf(reader,inReduce)}] ${stm(reader)}")
      //logs(c"  " + inputs.map{in => c"in: ${delayOf(in)}"}.mkString(", ") + "[max: " + criticalPath + "]")
      inputs.flatMap{in =>
        val latency_required = scrubNoise(criticalPath)    // Target latency required upon reaching this reader
        val latency_achieved = scrubNoise(delayOf(in))                       // Latency already achieved at the output of this in (assuming latency_missing is already injected)
        val latency_missing  = scrubNoise(retimingDelay(in) - builtInLatencyOf(in)) // Latency of this input that still requires manual register injection
        val latency_actual   = scrubNoise(latency_achieved - latency_missing)
        val delay = latency_required.toInt - latency_actual.toInt
        dbgs(s"..[${latency_required - latency_actual} (-> $delay) = $latency_required - ($latency_achieved - $latency_missing) (-> ${latency_required.toInt} - ${latency_actual.toInt})] ${stm(in)}")
        if (delay.toInt != 0) Some(in -> (reader, delay.toInt)) else None
      }
    }
    val inputDelays = consumerDelays.groupBy(_._1).mapValues(_.map(_._2)).toSeq
    inputDelays.flatMap{case (input, consumers) =>
      val consumerGroups = consumers.groupBy(_._2).mapValues(_.map(_._1))
      val delays = consumerGroups.keySet.toList.sorted  // Presort to maximize coalescing
      delays.flatMap{delay =>
        val readers = consumerGroups(delay)
        readers.map{reader =>
          dbgs(s"  Creating value delay on $input for reader $reader with delay $delay: ")
          logs(s"  Creating value delay on $input for reader $reader with delay $delay: ")
          reader -> createValueDelay(input, reader, delay)
        }
      }
    }
  }

  // This is needed to allow multiple blocks to use the same delay line.
  // Delay lines are created lazily, which can lead to issues where multiple blocks claim the same symbol.
  // To avoid this, we instead create the symbol in the outermost block
  // Since we don't control when stageBlock is called, we have to do this before symbol mirroring.
  // Note that this requires checking *blockNestedContents*, not just blockContents
  private def precomputeDelayLines(d: Op[_]): Unit = {
    hierarchy += 1
    if (d.blocks.nonEmpty) logs(s"  Precomputing delay lines for $d")
    d.blocks.foreach{block =>
      val scope = block.nestedStms
      val lines = computeDelayLines(scope).map(_._2)
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
      val inputs = bitBasedInputs(d)
      val reader2 = isolate() {
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
    val caseBody2: Block[A] = isolate(body.result){ stageBlock{
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
    val switch2 = isolate() {
      registerDelays(switch, selects)
      implicit val ctx: SrcCtx = switch.ctx
      stageWithFlow(Switch(f(selects), body2)){switch2 => transferData(switch, switch2) }
    }
    register(switch -> switch2)
  }

  private def retimeBlock[T](block: Block[T])(implicit ctx: SrcCtx): Sym[T] = {
    val scope = block.nestedStms
    val result = (scope.flatMap{case Op(d) => d.blocks; case _ => Nil} :+ block).flatMap(exps(_))

    dbgs(s"Retiming block $block:")
    //scope.foreach{e => dbgs(s"  ${stm(e)}") }
    //dbgs(s"Result: ")
    //result.foreach{e => dbgs(s"  ${stm(e)}") }
    // The position AFTER the given node
    val (newLatencies, newCycles) = pipeLatencies(result, scope)
    latencies ++= newLatencies
    cycles ++= newCycles.flatMap(_.symbols)

    newLatencies.toList.sortBy(_._2).foreach{case (s,l) => dbgs(s"[$l] ${stm(s)}") }

    dbgs("")
    dbgs("")
    dbgs("Sym Delays:")
    newLatencies.toList.map{case (s,l) => s -> scrubNoise(l - latencyOf(s, inReduce = cycles.contains(s))) }
      .sortBy(_._2)
      .foreach{case (s,l) =>
        s.fullDelay = l
        dbgs(s"  [$l = ${newLatencies(s)} - ${latencyOf(s, inReduce = cycles.contains(s))}]: ${stm(s)} [cycle = ${cycles.contains(s)}]")
      }

    isolate(){ retimeStms(block) }
  }


  def withRetime[A](wrap: List[Boolean], srcCtx: SrcCtx)(x: => A): A = {
    val prevRetime = retimeBlocks
    val prevCtx = ctx

    retimeBlocks = wrap
    ctx = Some(srcCtx)
    val result = x

    retimeBlocks = prevRetime
    ctx = prevCtx
    result
  }

  override protected def inlineBlock[T](b: Block[T]): Sym[T] = {
    val doWrap = retimeBlocks.headOption.getOrElse(false)
    if (retimeBlocks.nonEmpty) retimeBlocks = retimeBlocks.drop(1)
    dbgs(s"Transforming Block $b [$retimeBlocks => $doWrap]")
    if (doWrap) retimeBlock(b)(ctx.get)
    else super.inlineBlock(b)
  }

  private def transformCtrl[T:Type](lhs: Sym[T], rhs: Op[T])(implicit ctx: SrcCtx): Sym[T] = {
    // Switches aren't technically inner controllers from PipeRetimer's point of view.
    if (lhs.isInnerControl && !rhs.isSwitch && inHw) {
      val retimeEnables = rhs.blocks.map{_ => true }.toList
      withRetime(retimeEnables, ctx) { super.transform(lhs, rhs) }
    }
    else rhs match {
      case _:StateMachine[_] => withRetime(List(true,false,true), ctx){ super.transform(lhs, rhs) }
      case _ => if (inHw) withRetime(Nil, ctx){ super.transform(lhs, rhs) } else super.transform(lhs, rhs)
    }
  }

  override def transform[T:Type](lhs: Sym[T], rhs: Op[T])(implicit ctx: SrcCtx): Sym[T] = rhs match {
    case _:AccelScope => inAccel { transformCtrl(lhs,rhs) }
    case _ => transformCtrl(lhs, rhs)
  }
}
