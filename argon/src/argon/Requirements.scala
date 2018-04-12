package argon

import utils.io.CaptureStream
import utils.isSubtype
import utils.implicits.terminal._
import utils.implicits.Readable._

import scala.collection.mutable.ArrayBuffer
import scala.reflect.{ClassTag, classTag}

trait Requirements {
  case class FailedRequirements(n: Int) extends Exception(s"Test failed $n requirements")

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
      throw FailedRequirements(failed.length)
    }
  }

}

