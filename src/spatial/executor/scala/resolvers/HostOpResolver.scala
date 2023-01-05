package spatial.executor.scala.resolvers
import argon.node._
import argon.{Exp, Op}
import emul.FixedPoint
import spatial.executor.scala.memories.ScalaTensor
import spatial.executor.scala.{EmulResult, ExecutionState, SimpleEmulVal}
import spatial.node._
import utils.Result.RunError

import scala.reflect.ClassTag


trait HostOpResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = sym match {
    case Op(amap@ArrayMap(array, applyF, func)) =>
      type InputET = amap.A.L
      type OutputET = amap.B.L
      val arr = execState.getTensor[InputET](array)

      execState.log(s"Types: AMAP: ${amap.A} ${amap.B}, ${amap.R}")

      val tmpValues = Seq.tabulate(arr.size) {
        i =>
          // register the inputs to the block
          val tmpExecState = execState.copy()
          val copiedArr = new ScalaTensor[InputET](applyF.inputA, arr.shape, Some(arr.values))
          tmpExecState.register(copiedArr)
          tmpExecState.register(SimpleEmulVal(applyF.inputB, FixedPoint.fromInt(i)))
          applyF.stms.foreach(tmpExecState.runAndRegister(_))
          tmpExecState.getValue[InputET](applyF.result)
      }

      // now run the map part
      val outputValues = tmpValues.map {
        value =>
          val tmpExecState = execState.copy()
          tmpExecState.register(SimpleEmulVal(func.input, value))
          func.stms.foreach(tmpExecState.runAndRegister(_))
          tmpExecState.getValue[OutputET](func.result)
      }

      new ScalaTensor[OutputET](sym, arr.shape, Some(outputValues.map(Some(_))))(amap.B.tag)

    case Op(ArrayApply(coll, i)) =>
      val arr = execState.getTensor[coll.A.L](coll)
      val index = execState.getValue[FixedPoint](i).toInt
      val result = arr.read(Seq(index), true)
      SimpleEmulVal(sym, result.get, result.nonEmpty)

    case Op(ArrayLength(array)) =>
      val arr = execState.getTensor(array)
      SimpleEmulVal(sym, FixedPoint.fromInt(arr.size))

    case Op(TextToFix(t, fmt)) =>
      val text = execState.getValue[String](t)
      SimpleEmulVal(sym, FixedPoint(text, fmt.toEmul))

    case _ => super.run(sym, execState)
  }
}
