package object core extends static.Core {
  type SrcCtx = forge.SrcCtx
  lazy val SrcCtx = forge.SrcCtx

  type Sym[+S] = Exp[_,S]
  type Type[S] = ExpType[_,S]

  object Type {
    def apply[A:Type]: Type[A] = implicitly[Type[A]]
    def m[A,B](tp: Type[A]): Type[B] = tp.asInstanceOf[Type[B]]
  }

  def syms(a: Any*): Seq[Sym[_]] = core.Filters.syms(a:_*)
}
