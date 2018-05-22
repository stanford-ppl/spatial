package argon

import java.io.File

import utils.io.CaptureStream
import utils.isSubtype
import scala.reflect.{ClassTag, classTag}

trait DSLTestbench extends utils.Testbench { self =>
  def name: String = self.getClass.getName.replace("class ", "").replace('.','/').replace("$","")
  def initConfig(): Config = new Config
  implicit lazy val IR: State = {
    val state = new State
    state.config = initConfig()
    val cwd = new File(".").getAbsolutePath
    state.config.logDir = s"$cwd/logs/testbench/$name/"
    state.config.genDir = s"$cwd/gen/testbench/$name/"
    state.config.repDir = s"$cwd/reports/testbench/$name/"
    state.newScope(motion=false)  // Start a new scope (allows global declarations)
    state
  }

  def req[A,B](res: A, gold: B, msg: => String)(implicit ctx: SrcCtx): Unit = {
    if (!(res equals gold)) res shouldBe gold
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

  s"$name" should "complete without requirement failures" in { true }
}

