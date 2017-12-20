package pcc.core

case class SrcCtx(dir: String, file: String, line: Int, column: Int, content: Option[String]) {
  override def toString = s"$file:$line:$column"
}
object SrcCtx {
  lazy val empty = SrcCtx("?", "?", 0, 0, None)
}
