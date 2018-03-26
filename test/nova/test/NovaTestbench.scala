package nova.test

import argon.{DSLApp, _}
import utils.implicits.Readable._
import utils.implicits.terminal._
import utils.isSubtype
import utest._
import utils.io.CaptureStream

import scala.collection.mutable.ArrayBuffer
import scala.reflect.{ClassTag, classTag}

trait Requirements {

  private val failed = ArrayBuffer.empty[Seq[String]]
  def req[A,B](res: A, gold: B, msg: => String)(implicit ctx: SrcCtx): Unit = {
    if (!(res equals gold)) {
      failed += Seq(ctx + ": " + msg, s"Expected $gold, got $res")
    }
  }
  def reqOp[O:ClassTag](x: Sym[_], msg: => String)(implicit ctx: SrcCtx): Unit = {
    val res = x.op.map(_.getClass).getOrElse(x.getClass)
    val gold = classTag[O].runtimeClass
    if (!isSubtype(res,gold)) {
      failed += Seq(ctx + ": " + msg, r"Expected $gold, got $res")
    }
  }

  def reqWarn(calc: => Any, expect: String, msg: => String)(implicit ctx: SrcCtx, state: State): Unit = {
    val capture = new CaptureStream(state.out)
    withOut(capture){ calc }
    val lines = capture.dump.split("\n")
    if (!lines.exists{line => line.contains("warn") && line.contains(expect)}) {
      failed += Seq(ctx + ": " + msg, s"Expected warning $expect")
    }
  }

  def complete(): Unit = {
    if (failed.nonEmpty) {
      failed.foreach{msg =>
        msg.foreach{x => Console.out.error(x) }
        Console.out.println("")
      }
      throw argon.TestbenchFailure(s"Test failed ${failed.length} requirements")
    }
  }

}

abstract class NovaTestbench extends TestSuite {
  type Fail = argon.TestbenchFailure
  val defaultArgs: Array[String] = Array.empty

  def test(x: Any, args: Array[String] = defaultArgs): Unit = {
    x match {
      case x: DSLApp => x.main(args)
      case _ => throw new Exception(r"Don't know how to run test for ${x.getClass}")
    }
    x match {
      case x: Requirements => x.complete()
      case _ =>
    }
  }
}
