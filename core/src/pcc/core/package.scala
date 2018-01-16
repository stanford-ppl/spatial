package pcc

package object core extends static.Printing with static.Scoping with static.Staging  {

  def syms(a: Any*): Seq[Sym[_]] = core.Filters.syms(a:_*)

  type SrcCtx = forge.SrcCtx
}
