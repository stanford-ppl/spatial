package spatial.dse

import argon._
import spatial.metadata.control._
import spatial.metadata.params._
import spatial.node._
import spatial.lang.I32
import spatial.metadata.bounds._
import spatial.metadata.types._
import spatial.metadata.memory._

case class ParameterAnalyzer(IR: State) extends argon.passes.Traversal {

  override protected def preprocess[R](block: Block[R]): Block[R] = {
    super.preprocess(block)
  }

  override protected def postprocess[R](block: Block[R]): Block[R] = {
    super.postprocess(block)
  }

  private def setRange(x: Sym[_], min: Int, max: Int, stride: Int = 1): Unit = x.getParamDomain match {
    case Some((start,end,step)) =>
      x.paramDomain = (Math.max(min,start), Math.min(max,end), Math.max(stride,step))
    case None =>
      x.paramDomain = (min,max,stride)
  }

  private def collectParams(x: Sym[_]): Seq[Sym[_]] = {
    def dfs(frontier: Seq[Sym[_]]): Seq[Sym[_]] = frontier.flatMap{
      case s@Param(_) => Seq(s)
      case Def(d) => dfs(d.inputs)
      case _ => Nil
    }
    dfs(Seq(x))
  }

  private def copyBound(src: Sym[_], dst: Sym[_]): Unit = {
    src.getBound.map(dst.bound = _)
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case AccelScope(func) => TopCtrl.set(lhs); super.visit(lhs,rhs)

    case SetReg(reg, x) if reg.isArgIn => 
      reg.readers.foreach(copyBound(x,_))

    case FIFONew(c@Expect(_)) =>
      warn(s"FIFO $lhs has parametrized size. Tuning of FIFO depths is not yet supported.")
      collectParams(c).foreach(IgnoreParams += _)

    case SRAMNew(dims) =>
      dims.collect{case x@Expect(_) => 
        val p = collectParams(x)
        dbg(s"Found SRAM $lhs with parametrized in dimensions: $p")
        p.foreach(TileSizes += _)
      }

    case RegFileNew(dims,_) =>
      dims.collect{case x@Expect(_) => 
        val p = collectParams(x)
        dbg(s"Found SRAM $lhs with parametrized in dimensions: $p")
        p.foreach(TileSizes += _)
      }

    case e: DenseTransfer[_,_,_] =>
      val pars = e.pars.map(_.asInstanceOf[Sym[_]]).flatMap(collectParams)
      e.pars.collect{case p if !p.isParam => p.setIntValue(p.toInt)}
      pars.foreach(ParParams += _)

    case e: SparseTransfer[_,_] =>
      val pars = e.pars.map(_.asInstanceOf[Sym[_]]).flatMap(collectParams)
      e.pars.collect{case p if !p.isParam => p.setIntValue(p.toInt)}
      pars.foreach(ParParams += _)

    case CounterNew(start,end,step,par) =>
      dbg(s"Found counter with start=$start, end=$end, step=$step, par=$par")
      dbg(s"  bound($start) = " + start.getBound)
      dbg(s"  bound($end) = " + end.getBound)
      dbg(s"  bound($step) = " + step.getBound)

      val pars = collectParams(par)
      pars.foreach{x => dbgs(s"Found Counter Step param in $lhs = $x"); ParParams += x}
      val steps = collectParams(step)
      steps.foreach{x => dbgs(s"Found Counter Step param in $lhs = $x"); TileSizes += x}
      val ends = collectParams(end)
      ends.foreach{x => dbgs(s"Found Counter End param in $lhs = $x"); TileSizes += x}
      val starts = collectParams(start)
      starts.foreach{x => dbgs(s"Found Counter Start param in $lhs = $x"); TileSizes += x}

      // Set constraints on par factor
      if (par.isParam){
        (start,end,step) match {
          case (Final(0),Parameter(e),Final(1)) =>
            val r1 = RLessEqual(par, e)
            val r2 = RDivides(par, e)
            dbg(s"  Case #1: Adding restriction $r1")
            dbg(s"  Case #2: Adding restriction $r2")
            Restrictions += r1
            Restrictions += r2

          case (_,_,_) if end.isParam && step.isParam => // ???

          case (Final(0), Expect(e), _) if step.isParam =>
            val r = RDividesQuotient(par, e.toInt, step)
            dbg(s"  Case #3: Adding restriction $r")
            Restrictions += r

          case (Upper(s),Upper(e), _) if step.isParam =>
            val r = RDividesQuotient(par, (e-s).toInt, step)
            dbg(s"  Case #4: Adding restriction $r")
            Restrictions += r

          case (Upper(s),Upper(e),Upper(t)) =>
            val nIters = (e - s)/t
            //if (nIters < max) max = nIters.toInt
            val r = RDividesConst(par, nIters.toInt)  // HACK: par factor divides bounded loop size (avoid edge case)
            dbg(s"  Case #5: Adding restriction $r")
            Restrictions += r

          case _ => // No restrictions
        }
        // Set constraints on step size
        (start,end,step) match {
          case (_,_,_) if start.isParam && end.isParam && step.isParam => // ???

          case (Final(0),Parameter(p),Parameter(s)) =>
            val r1 = RLessEqual(s, p)
            val r2 = RDivides(s, p)   // HACK: avoid edge case
            dbg(s"  Case #6: Adding restriction $r1")
            dbg(s"  Case #7: Adding restriction $r2")
            Restrictions += r1
            Restrictions += r2

          case (Upper(s),Upper(b),Parameter(p)) =>
            val l = b - s
            setRange(p, 1, l.toInt, 1)
            val r = RDividesConst(p, l.toInt) // HACK: avoid edge case
            dbg(s"  Case #8: Adding restriction $r")
            Restrictions += r

          case _ => // No restrictions
        }
      } else if (par.isConst) {
        (start,end,step) match {
          case (Parameter(s),Parameter(p),Parameter(_)) => // ???

          case (Final(0),Parameter(p),Parameter(s)) =>
            val r1 = RLessEqual(s, p)
            val r2 = RDivides(s, p)   // HACK: avoid edge case
            dbg(s"  Case #6: Adding restriction $r1")
            dbg(s"  Case #7: Adding restriction $r2")
            Restrictions += r1
            Restrictions += r2

          case (Upper(s),Upper(b),Parameter(p)) =>
            val l = b - s
            setRange(p, 1, l.toInt, 1)
            val r = RDividesConst(p, l.toInt) // HACK: avoid edge case
            dbg(s"  Case #8: Adding restriction $r")
            Restrictions += r

          case _ => // No restrictions
        }

      }

    case e: OpForeach =>
      val pars = e.cchain.pars
      pars.foreach{x => dbgs(s"Found OpForeach par param in $lhs = $x"); ParParams += _}
      if (lhs.isOuterControl && !lhs.getUserSchedule.isDefined) PipelineParams += lhs
      super.visit(lhs, rhs)

    case e: OpReduce[_] =>
      val pars = e.cchain.pars
      pars.foreach{x => dbgs(s"Found OpReduce par param in $lhs = $x"); ParParams += _}
      dbgs(s"visit $lhs, ${lhs.isOuterControl}? ${lhs.level}")
      if (lhs.isOuterControl && !lhs.getUserSchedule.isDefined) PipelineParams += lhs
      super.visit(lhs, rhs)

    case e: OpMemReduce[_,_] =>
      val opars = e.cchainMap.pars
      val ipars = e.cchainRed.pars
      opars.foreach{x => dbgs(s"Found OpMemReduce par param in $lhs = $x"); ParParams += _}
      ipars.foreach{x => dbgs(s"Found OpMemReduce par param in $lhs = $x"); ParParams += _}
      if (lhs.isOuterControl && !lhs.getUserSchedule.isDefined) PipelineParams += lhs
      super.visit(lhs, rhs)

    case _ => super.visit(lhs, rhs)
  }

}
