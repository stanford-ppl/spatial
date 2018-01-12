package pcc.core.static

import forge._
import pcc.util.files
import pcc.util.Report._
import pcc.util.Tri._
import java.io.PrintStream
import java.nio.file.{Files,Paths}

trait Printing {
  def state(implicit localState: State): State = localState
  def ctx(implicit localCtx: SrcCtx): SrcCtx = localCtx
  def config(implicit localState: State): Config = localState.config

  def plural(x: Int, singular: String, plur: String): String = pcc.util.Report.plural(x, singular, plur)

  /** Compiler Generated Files (reports, codegen, etc.) **/
  @stateful def open(): Unit = { state.genTab += 1 }
  @stateful def open(x: => Any): Unit = { emit(x); open() }
  @stateful def emit(x: => Any): Unit = if (config.enGen) state.gen.println("  "*state.genTab + x)
  @stateful def close(): Unit = { state.genTab -= 1 }
  @stateful def close(x: => Any): Unit = { emit(x); close() }

  /** Compiler Info Messages **/
  @stateful def info(x: => Any): Unit = if (config.enInfo) state.out.info(x)
  @stateful def info(ctx: SrcCtx): Unit = if (config.enInfo) state.out.info(ctx)
  @stateful def info(ctx: SrcCtx, showCaret: Boolean): Unit = if (config.enInfo) state.out.info(ctx,showCaret)
  @stateful def info(ctx: SrcCtx, x: => Any, noInfo: Boolean = false): Unit = {
    if (config.enInfo) state.out.info(ctx, x)
  }

  /** Compiler Warnings **/
  @stateful def warn(x: => Any): Unit = if (config.enWarn) state.out.warn(x)
  @stateful def warn(ctx: SrcCtx): Unit = if (config.enWarn) state.out.warn(ctx)
  @stateful def warn(ctx: SrcCtx, showCaret: Boolean): Unit = if (config.enWarn) state.out.warn(ctx,showCaret)
  @stateful def warn(ctx: SrcCtx, x: => Any, noWarning: Boolean = false): Unit = {
    if (config.enWarn) state.out.warn(ctx, x)
    if (!noWarning) state.logWarning()
  }

  /** Compiler Errors **/
  @stateful def error(x: => Any): Unit = if (config.enError) state.out.error(x)
  @stateful def error(ctx: SrcCtx): Unit = if (config.enError) state.out.error(ctx)
  @stateful def error(ctx: SrcCtx, showCaret: Boolean): Unit = if (config.enError) state.out.error(ctx,showCaret)
  @stateful def error(ctx: SrcCtx, x: => String): Unit = error(ctx, x, noError = false)
  @stateful def error(ctx: SrcCtx, x: => String, noError: Boolean): Unit = {
    if (config.enError) state.out.error(ctx, x)
    if (!noError) state.logError()
  }

  /** Compiler Bugs **/
  @stateful def bug(x: => Any): Unit = state.out.bug(x)
  @stateful def bug(ctx: SrcCtx): Unit = state.out.bug(ctx)
  @stateful def bug(ctx: SrcCtx, showCaret: Boolean): Unit = state.out.bug(ctx,showCaret)
  @stateful def bug(ctx: SrcCtx, x: => Any, noBug: Boolean = false): Unit = {
    state.out.bug(ctx, x)
    if (!noBug) state.logBug()
  }


  /** Generic Terminal Printing **/
  @stateful def msg(x: => Any): Unit = if (config.enInfo) state.out.println(x)

  /** Debugging **/
  @stateful def dbg(x: => Any): Unit = if (config.enDbg) state.log.println(x)
  @stateful def dbgs(x: => Any): Unit = if (config.enDbg) state.log.println("  "*state.logTab + x)
  @stateful def dbgblk(x: => Unit): Unit = if (config.enDbg) {
    state.logTab += 1
    x
    state.logTab -= 1
  }

  @stateful def log(x: => Any): Unit = if (config.enLog) state.log.println(x)
  @stateful def logs(x: => Any): Unit = if (config.enLog) state.log.println("  "*state.logTab + x)

  @stateful def stm(lhs: Sym[_]): String = lhs.rhs match {
    case Nix => s"$lhs"
    case One(c) => s"$lhs = $c"
    case Two(rhs) => s"$lhs = $rhs"
  }

  @stateful def createStream(dir: String, filename: String): PrintStream = {
    Files.createDirectories(Paths.get(dir))
    new PrintStream(dir + files.sep + filename)
  }

  @stateful def setupStream(dir: String, filename: String): String = {
    getOrCreateStream(dir, filename)
    dir + files.sep + filename
  }

  @stateful def getStream(path: String): PrintStream = state.streams.getOrElse(path,{
    throw new Exception("Stream used without being created.")
  })

  @stateful def getOrCreateStream(dir: String, filename: String): PrintStream = {
    val path = dir + files.sep + filename
    if (state.streams.contains(path)) state.streams(path)
    else {
      val stream = createStream(dir, filename)
      state.streams += path -> stream
      stream
    }
  }

  @stateful def inStream[T](
    enable: Boolean,
    stream: () => PrintStream,
    block:  => T,
    getStream: () => PrintStream,
    setStream: PrintStream => Unit,
    endStream: PrintStream => Unit = _.flush()
  ): T = {
    if (enable) {
      val save = getStream()
      val s = stream()
      setStream(s)
      try { block }
      finally {
        endStream(s)
        setStream(save)
      }
    }
    else block
  }

  @stateful def inLog[T](stream: PrintStream)(blk: => T): T = {
    if (!config.enDbg) dbg(s"Ignoring logging for ${state.streams.find(_._2 == stream).map(_._1).getOrElse(stream.toString)}")
    inStream(config.enDbg, () => stream, blk, () => state.log, {s => state.log = s})
  }
  @stateful def inLog[T](dir: String, filename: String)(blk: => T): T = {
    if (!config.enDbg) msg(s"Ignoring log for $dir$filename")
    inStream(config.enDbg, () => getOrCreateStream(dir,filename), blk, () => state.log, {s => state.log = s})
  }
  @stateful def inLog[T](path: String)(blk: => T): T = {
    if (!config.enDbg) msg(s"Ignoring log for $path")
    inStream(config.enDbg, () => getStream(path), blk, () => state.log, {s => state.log = s})
  }
  @stateful def withLog[T](dir: String, filename: String)(blk: => T): T = {
    if (!config.enDbg) msg(s"Ignoring log for $dir$filename")
    inStream(config.enDbg, () => createStream(dir,filename), blk, () => state.log, s => state.log = s, _.close())
  }

  @stateful def inGen[T](stream: PrintStream)(blk: => T): T = {
    if (!config.enGen) dbg(s"Ignoring logging for ${state.streams.find(_._2 == stream).map(_._1).getOrElse(stream.toString)}")
    inStream(config.enGen, () => stream, blk, () => state.gen, {s => state.gen = s})
  }
  @stateful def inGen[T](dir: String, filename: String)(blk: => T): T = {
    if (!config.enGen) msg(s"Ignoring gen for $dir$filename")
    inStream(config.enGen, () => getOrCreateStream(dir,filename), blk, () => state.gen, {s => state.gen = s})
  }
  @stateful def withGen[T](dir: String, filename: String)(blk: => T): T = {
    if (!config.enGen) msg(s"Ignoring gen for $dir$filename")
    inStream(config.enGen, () => createStream(dir,filename), blk, () => state.gen, {s => state.gen = s}, _.close())
  }

  @stateful def withOut[T](dir: String, filename: String)(blk: => T): T = {
    inStream(enable = true, () => createStream(dir,filename), blk, () => state.out, {s => state.out = s}, _.close())
  }
}
