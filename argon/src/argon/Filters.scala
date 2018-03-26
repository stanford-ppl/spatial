package argon

import utils.recursive

object Filters {

  val expsFunc: PartialFunction[Any,Seq[Sym[_]]] = {
    case s: Sym[_] => Seq(s)
    case b: Block[_] => exps(b.result) ++ exps(b.effects.antiDeps)
    case d: Op[_]    => d.expInputs
    case l: Iterable[_] => recursive.collectSeqs(expsFunc)(l.iterator)
  }

  val symsFunc: PartialFunction[Any,Seq[Sym[_]]] = {
    case s: Sym[_] if s.isSymbol => Seq(s)
    case s: Sym[_] if s.isBound  => Seq(s)
    case b: Block[_]    => syms(b.result) ++ syms(b.effects.antiDeps)
    case d: Op[_]       => d.inputs
    case l: Iterable[_] => recursive.collectSeqs(symsFunc)(l.iterator)
  }

  final def exps(a: Any*): Seq[Sym[_]] = if (expsFunc.isDefinedAt(a)) expsFunc(a) else Nil
  final def syms(a: Any*): Seq[Sym[_]] = if (symsFunc.isDefinedAt(a)) symsFunc(a) else Nil
}
