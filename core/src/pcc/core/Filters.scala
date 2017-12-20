package pcc.core

import pcc.util.recursive

object Filters {

  val symsFunc: PartialFunction[Any,Seq[Sym[_]]] = {
    case s: Sym[_] if s.isSymbol => Seq(s)
    case s: Sym[_] if s.isBound  => Seq(s)
    case b: Block[_]    => syms(b.result) ++ syms(b.impure)
    case d: Op[_]       => d.inputs
    case l: Iterable[_] => recursive.collectSeqs(symsFunc)(l.iterator)
  }

  final def syms(a: Any*): Seq[Sym[_]] = if (symsFunc.isDefinedAt(a)) symsFunc(a) else Nil


}
