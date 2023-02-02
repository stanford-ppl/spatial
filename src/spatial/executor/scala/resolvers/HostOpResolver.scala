package spatial.executor.scala.resolvers
import argon.node._
import argon.{Exp, Op, dbgs}
import emul.{FixedPoint, FixedPointRange}
import spatial.executor.scala.memories.ScalaTensor
import spatial.executor.scala.{
  EmulResult,
  EmulUnit,
  EmulVal,
  ExecutionState,
  SimpleEmulVal,
  SimulationException,
  SomeEmul
}
import spatial.node._
import utils.Result.CompileError

import scala.reflect.ClassTag

trait HostOpResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], op: Op[V],
                         execState: ExecutionState): EmulResult = op match {

    case InputArguments() =>
      val rtArgs = execState.runtimeArgs
      new ScalaTensor[EmulVal[String]](Seq(rtArgs.size), None, Some(rtArgs.map { x => Some(SimpleEmulVal(x))}))

    case ArrayNew(size) =>
      val rSize = execState.getValue[FixedPoint](size).toInt
      new ScalaTensor[SomeEmul](Seq(rSize), None, None)

    case ArrayMap(array, applyF, func) =>
      val arr = execState.getTensor[SomeEmul](array)
      val result = Seq.tabulate(arr.size) { i =>
        val fpI = SimpleEmulVal(FixedPoint.fromInt(i))
        val applied = runBlock(applyF,
                               Map(applyF.inputA -> arr, applyF.inputB -> fpI),
                               execState)
        runBlock(func, Map(func.input -> applied), execState)
      }

      new ScalaTensor[SomeEmul](arr.shape, None, Some(result.map(Some(_))))

    case ArrayApply(coll, i) =>
      val arr = execState.getTensor[SomeEmul](coll)
      val index = execState.getValue[FixedPoint](i).toInt
      val result = arr.read(Seq(index), true)
      if (result.isEmpty) {
        throw SimulationException(
          s"Attempting to access $coll[$i] = $arr[$index], which is unset.")
      }
      result.orNull

    case mi @ MapIndices(s, func) =>
      val size = execState.getValue[FixedPoint](s).toInt
      val mapped = Seq.tabulate(size) { i =>
        val tmpState = execState.copy()
        tmpState.register(func.input, SimpleEmulVal(FixedPoint.fromInt(i)))
        func.stms.foreach(tmpState.runAndRegister(_))
        tmpState(func.result)
      }
      new ScalaTensor[SomeEmul](Seq(size), None, Some(mapped.map(Some(_))))

    case ArrayZip(arrayA, arrayB, applyA, applyB, func) =>
      val arrA = execState.getTensor(arrayA)
      val arrB = execState.getTensor(arrayB)
      val size = arrA.size
      val newValues = Seq.tabulate(size) { i =>
        val fpI = SimpleEmulVal(FixedPoint.fromInt(i))
        val vA = runBlock(applyA,
                          Map(applyA.inputA -> arrA, applyA.inputB -> fpI),
                          execState)
        val vB = runBlock(applyB,
                          Map(applyB.inputA -> arrB, applyB.inputB -> fpI),
                          execState)
        runBlock(func, Map(func.inputA -> vA, func.inputB -> vB), execState)
      }
      val elementType = func.result.tp
      type ET = elementType.L
      new ScalaTensor[SomeEmul](Seq(size), None, Some(newValues.map(Some(_))))

    case ArrayReduce(array, apply, reduce) =>
      val arr = execState.getTensor(array)
      val newValues = Seq.tabulate(arr.size) { i =>
        val fpI = SimpleEmulVal(FixedPoint.fromInt(i))
        runBlock(apply,
                 Map(apply.inputA -> arr, apply.inputB -> fpI),
                 execState)
      }
      newValues.reduce { (a: EmulResult, b: EmulResult) =>
        runBlock(reduce, Map(reduce.inputA -> a, reduce.inputB -> b), execState)
      }

    case ArrayLength(array) =>
      val arr = execState.getTensor(array)
      SimpleEmulVal(FixedPoint.fromInt(arr.size))

    case TextToFix(t, fmt) =>
      val text = execState.getValue[String](t)
      SimpleEmulVal(FixedPoint(text, fmt.toEmul))

    case SeriesForeach(start, end, step, func) =>
      val rStart = execState.getValue[FixedPoint](start)
      val rEnd = execState.getValue[FixedPoint](end)
      val rStep = execState.getValue[FixedPoint](step)
      FixedPointRange(rStart, rEnd, rStep, false).foreach { i =>
        val newState = execState.copy()
        newState.register(func.input, SimpleEmulVal(i))
        func.stms.foreach(newState.runAndRegister(_))
      }
      EmulUnit(sym)

    case IfThenElse(cond, thenBlk, elseBlk) =>
      if (execState.getValue[emul.Bool](cond).value) {
        runBlock(thenBlk, Map.empty, execState)
      } else {
        runBlock(elseBlk, Map.empty, execState)
      }
    case _ => super.run(sym, op, execState)
  }
}
