
package object core extends static.Printing with static.Scoping with static.Staging {

  type SrcCtx = forge.SrcCtx
  lazy val SrcCtx = forge.SrcCtx

  def syms(a: Any*): Seq[Sym[_]] = core.Filters.syms(a:_*)
}
