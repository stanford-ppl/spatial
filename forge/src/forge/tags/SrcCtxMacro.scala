package forge.tags

import forge.SrcCtx

import scala.reflect.macros.blackbox
import scala.language.experimental.macros

object SrcCtxMacro {
  def impl(c: blackbox.Context): c.Expr[SrcCtx] = {
    import c.universe._
    val pos = c.enclosingPosition
    val path = pos.source.path
    val filename = pos.source.file.name
    val line = pos.line
    val column = pos.column
    val lineContent = if (line > 0) Some(pos.source.lineToString(line-1)) else None

    c.Expr(q"forge.SrcCtx($path, $filename, $line, $column, $lineContent, Seq.empty)")
  }
}
