package forge

import forge.tags.SrcCtxMacro

case class SrcCtx(dir: String, file: String, line: Int, column: Int, content: Option[String], previous: Seq[SrcCtx]) extends utils.Ctx {
  override def toString = {
    val previousCtx = previous match {
      case Nil => ""
      case ctxes => s" -- [${ctxes.mkString(", ")}]"
    }
    s"$file:$line:$column$previousCtx"
  }
}

object SrcCtx {
  lazy val empty = SrcCtx("?", "?", 0, 0, None, Seq.empty)

  implicit def _sc: SrcCtx = macro SrcCtxMacro.impl
}
