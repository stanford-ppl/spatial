package object argon extends static.Core {
  type SrcCtx = forge.SrcCtx
  lazy val SrcCtx = forge.SrcCtx

  type Sym[+S] = Exp[_,S]
  type Type[S] = ExpType[_,S]

  object Type {
    def apply[A:Type]: Type[A] = implicitly[Type[A]]
    def m[A,B](tp: Type[A]): Type[B] = tp.asInstanceOf[Type[B]]
  }

  def syms(a: Any*): Seq[Sym[_]] = argon.Filters.syms(a:_*)
  def exps(a: Any*): Seq[Sym[_]] = argon.Filters.exps(a:_*)

  def proto[A](exp: Exp[_,A]): A = { exp._rhs = Def.TypeRef; exp.asInstanceOf[A] }
}
