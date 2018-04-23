package argon

/** Holds constant scala values. **/
class Literal(c: Any) { def value: Any = c }

/** Helper function for testing equality with literal constants
  * The extracted value may not be the same type as the normal constant type for the given symbol
  * (This is a hack to get around the fact that new types cannot have value equality with Int, Long, etc.)
  */
object Literal {
  def unapply(x: Any): Option[Any] = x match {
    case s: Sym[_] if s.isConst => s.__extract
    case _ => None
  }
}

/** Used to match on or extract the constant value of this symbol if it is constant (not a parameter) */
object Const {
  def unapply[C,A](x: Exp[C,A]): Option[C] = if (x.isConst) x.c else None
}

/** Used to match on or extract the constant value of this symbol if it is a parameter (not a constant) */
object Param {
  def unapply[C,A](x: Exp[C,A]): Option[C] = if (x.isParam) x.c else None
}

/** Used to match on or extract the constant value of this symbol if it is a parameter or constant */
object Value {
  def unapply[C,A](x: Exp[C,A]): Option[C] = x.c
}
