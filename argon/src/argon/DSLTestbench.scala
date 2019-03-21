package argon

import argon.schedule.SimpleScheduler
import utils.io.CaptureStream
import utils.isSubtype

import scala.reflect.{ClassTag, classTag}

trait DSLTestbench extends utils.Testbench with DSLRunnable { self =>

  def req[A,B](res: A, gold: B, msg: => String)(implicit ctx: SrcCtx): Unit = {
    // if (!(res equals gold)) res shouldBe gold
  }
  def reqOp[O:ClassTag](x: Sym[_], msg: => String)(implicit ctx: SrcCtx): Unit = {
    val res = x.op.map(_.getClass).getOrElse(x.getClass)
    val gold = classTag[O].runtimeClass
    require(isSubtype(res,gold), msg)
  }

  def reqWarn(calc: => Any, expect: String, msg: => String)(implicit ctx: SrcCtx): Unit = {
    val capture = new CaptureStream(state.out)
    withOut(capture){ calc }
    val lines = capture.dump.split("\n")
    require(lines.exists{line => line.contains("warn") && line.contains(expect)}, s"$msg. Expected warning $expect")
  }

  def checks(): Unit = { }

  // Having this as a should statement makes it lazily evaluated (even though its part of the constructor)
  s"$name" should "check without requirement failures" in { checks() }
}

