package nova.core

object Invert {
  implicit class TypeInvertOps[B[_]<:Top[_]](sym: B[_]) {
    def mtp[A]: B[A] = sym.tp.asInstanceOf[B[A]]
  }

  implicit class TypeInverterOps(sym: Sym[_]) {
    def mtp[A]: Type[A] = sym.tp.asInstanceOf[Type[A]]
  }
}
