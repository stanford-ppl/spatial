package argon.static

import forge.tags._
import utils.escapeConst
import utils.implicits.terminal._
import utils.io.files
import utils.io.stream.createStream

import java.io.PrintStream

trait Printing {
  def ctx(implicit localCtx: SrcCtx): SrcCtx = localCtx
  def state(implicit localState: State): State = localState
  def config(implicit localState: State): Config = localState.config

  @stateful def raiseIssue(issue: Issue): Unit = state.issues += issue

  /** Compiler Generated Files (reports, codegen, etc.) */
  @stateful def open(): Unit = { state.incGenTab() }
  @stateful def open(x: => Any): Unit = { emit(x); open() }
  @stateful def emit(x: => Any): Unit = if (config.enGen) state.gen.println("  "*state.getGenTab + x)
  @stateful def close(): Unit = { state.decGenTab() }
  @stateful def close(x: => Any): Unit = { close(); emit(x) }
  @stateful def closeopen(x: => Any): Unit = { close(); emit(x); open() }

  /** Compiler Info Messages */
  @stateful def info(x: => Any): Unit = if (config.enInfo) state.out.info(x)
  @stateful def info(ctx: SrcCtx): Unit = info(ctx, showCaret = true)
  @stateful def info(ctx: SrcCtx, showCaret: Boolean): Unit = if (config.enInfo) state.out.info(ctx,showCaret)
  @stateful def info(ctx: SrcCtx, x: => Any, noInfo: Boolean = false): Unit = {
    if (config.enInfo) state.out.info(ctx, x)
  }

  /** Compiler Warnings */
  @stateful def warn(x: => Any): Unit = {
    lazy val msg = x
    if (config.enDbg) state.log.log(s"[warn] $msg")
    if (config.enWarn) state.out.warn(msg)
  }
  @stateful def warn(ctx: SrcCtx): Unit = warn(ctx, showCaret = true)
  @stateful def warn(ctx: SrcCtx, showCaret: Boolean): Unit = {
    if (config.enDbg) { state.log.print("[warn] " ); state.log.log(ctx, showCaret) }
    if (config.enWarn) state.out.warn(ctx,showCaret)
  }
  @stateful def warn(ctx: SrcCtx, x: => Any, noWarning: Boolean = false): Unit = {
    lazy val msg = x
    if (config.enDbg) { state.log.print("[warn] "); state.log.log(ctx, msg) }
    if (config.enWarn) state.out.warn(ctx, msg)
    if (!noWarning) state.logWarning()
  }

  /** Compiler Errors */
  @stateful def error(x: => Any): Unit = {
    lazy val msg = x
    if (config.enDbg) state.log.log(s"[error] $msg")
    if (config.enError) state.out.error(msg)
  }
  @stateful def error(ctx: SrcCtx): Unit = error(ctx, showCaret = true)
  @stateful def error(ctx: SrcCtx, showCaret: Boolean): Unit = {
    if (config.enDbg) { state.log.print("[error] "); state.log.log(ctx, showCaret) }
    if (config.enError) state.out.error(ctx,showCaret)
  }
  @stateful def error(ctx: SrcCtx, x: => String): Unit = error(ctx, x, noError = false)
  @stateful def error(ctx: SrcCtx, x: => String, noError: Boolean): Unit = {
    lazy val msg = x
    if (config.enDbg) { state.log.print("[error] "); state.log.log(ctx, msg) }
    if (config.enError) state.out.error(ctx, msg)
    if (!noError) state.logError()
  }

  /** Compiler Bugs */
  @stateful def bug(x: => Any): Unit = {
    lazy val msg = x
    if (config.enDbg) state.log.log(s"[bug] $msg")
    state.out.bug(x)
  }
  @stateful def bug(ctx: SrcCtx): Unit = bug(ctx, showCaret = true)
  @stateful def bug(ctx: SrcCtx, showCaret: Boolean): Unit = {
    if (config.enDbg) { state.log.print("[bug] "); state.log.log(ctx, showCaret) }
    state.out.bug(ctx, showCaret)
  }
  @stateful def bug(ctx: SrcCtx, x: => Any, noBug: Boolean = false): Unit = {
    lazy val msg = x
    if (config.enDbg) { state.log.print("[bug] "); state.log.log(ctx, msg) }
    state.out.bug(ctx, msg)
    if (!noBug) state.logBug()
  }


  /** Generic Terminal Printing */
  @stateful def msg(x: => Any): Unit = if (config.enInfo) state.out.println(x)

  /** Debugging */
  @stateful def dbg(ctx: SrcCtx, x: => Any): Unit = {
    if (config.enDbg) state.log.println(s"$ctx: $x")
  }
  @stateful def dbg(x: => Any): Unit = if (config.enDbg) state.log.println(x)
  @stateful def dbgs(x: => Any): Unit = if (config.enDbg) state.log.println("  "*state.logTab + x)
  @stateful def dbgss(x: => Any): Unit = if (config.enDbg) {
    x.toString.split("\n").foreach{line => dbgs(line) }
  }
  @stateful def dbgss(prefix: String, x: => Any): Unit = if (config.enDbg) {
    x.toString.split("\n").foreach{line => dbgs(prefix + line) }
  }
  @stateful def dbgblk(x: => Unit): Unit = if (config.enDbg) {
    state.logTab += 1
    x
    state.logTab -= 1
  } else x
  @stateful def dbgblk(h: => String)(x: => Unit): Unit = if (config.enDbg) {
    dbgs(h)
    dbgblk(x)
  } else x

  @stateful def indent[T](x: => T): T = if (config.enDbg) {
    state.logTab += 1
    val r = x
    state.logTab -= 1
    r
  } else x

  @stateful def log(x: => Any): Unit = if (config.enLog) state.log.println(x)
  @stateful def logs(x: => Any): Unit = if (config.enLog) state.log.println("  "*state.logTab + x)

  def stm(lhs: Sym[_]): String = lhs.rhs match {
    case Def.Bound(id)   => s"b$id"
    case Def.Const(c)    => s"${escapeConst(c)}"
    case Def.Param(id,c) => s"p$id = <${escapeConst(c)}>"
    case Def.Node(id,op) => s"x$id = $op"
    case Def.Error(id,_) => s"e$id <error>"
    case Def.TypeRef     => lhs.tp.typeName
  }

  def shortStm(lhs: Sym[_]): String = lhs.rhs match {
    case Def.Bound(id)   => s"b$id"
    case Def.Const(c)    => s"${escapeConst(c)}"
    case Def.Param(id,c) => s"p$id = <${escapeConst(c)}>"
    case Def.Node(id,op) => s"x$id: ${op.productPrefix}"
    case Def.Error(id,_) => s"e$id <error>"
    case Def.TypeRef     => lhs.tp.typeName
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
      state.genTabs += stream -> 0
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
    inStream(config.enDbg, () => stream, blk, () => state.log, {s => state.log = s})
  }
  @stateful def inLog[T](dir: String, filename: String)(blk: => T): T = {
    inStream(config.enDbg, () => getOrCreateStream(dir,filename), blk, () => state.log, {s => state.log = s})
  }
  @stateful def inLog[T](path: String)(blk: => T): T = {
    inStream(config.enDbg, () => getStream(path), blk, () => state.log, {s => state.log = s})
  }
  @stateful def withLog[T](dir: String, filename: String)(blk: => T): T = {
    inStream(config.enDbg, () => createStream(dir,filename), blk, () => state.log, s => state.log = s, _.close())
  }

  @stateful def inGen[T](stream: PrintStream)(blk: => T): T = {
    inStream(config.enGen, () => stream, blk, () => state.gen, {s => state.gen = s})
  }
  @stateful def inGen[T](dir: String, filename: String)(blk: => T): T = {
    inStream(config.enGen, () => getOrCreateStream(dir,filename), blk, () => state.gen, {s => state.gen = s})
  }
  @stateful def inGen[T](path: String)(blk: => T): T = {
    val (dir, filename) = files.splitPath(path)
    inGen(dir, filename)(blk)
  }
  @stateful def withGen[T](dir: String, filename: String)(blk: => T): T = {
    inStream(config.enGen, () => createStream(dir,filename), blk, () => state.gen, {s => state.gen = s}, _.close())
  }

  @stateful def withOut[T](stream: PrintStream)(blk: => T): T = {
    inStream(enable = true, () => stream, blk, () => state.out, {s => state.out = s}, _.close())
  }
  @stateful def withOut[T](dir: String, filename: String)(blk: => T): T = {
    inStream(enable = true, () => createStream(dir,filename), blk, () => state.out, {s => state.out = s}, _.close())
  }

  @stateful def strMeta(lhs: Sym[_]): Unit = {
    lhs.name.foreach{name => dbgs(s" - Name: $name") }
    if (lhs.prevNames.nonEmpty) {
      val aliases = lhs.prevNames.sorted.map{case (tx,alias) => s"$tx: $alias" }.mkString(", ")
      dbgs(s" - Aliases: $aliases")
    }
    dbgs(s" - Type: ${lhs.tp}")
    dbgs(s" - SrcCtx: ${lhs.ctx}")
    val meta = metadata.all(lhs).toList.sortBy {case (k, _) => s"$k"}
    meta.foreach{case (k,m) => dbgss(s" - $k: $m") }
  }
}
