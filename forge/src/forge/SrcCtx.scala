package forge

import forge.tags.SrcCtxMacro

case class SrcCtx(dir: String, file: String, line: Int, column: Int, content: Option[String], previous: Option[SrcCtx]) extends utils.Ctx {
  override def toString = {
    val previousCtx = previous match {
      case Some(ctx) => s" -- $ctx"
      case None => ""
    }
    s"$file:$line:$column$previousCtx"
  }
}

object SrcCtx {
  lazy val empty = SrcCtx("?", "?", 0, 0, None, None)

  implicit def _sc: SrcCtx = macro SrcCtxMacro.impl
}
