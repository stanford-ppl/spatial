package argon

import utils.recursive

object Filters {

  val expsFunc: PartialFunction[Any,Set[Sym[_]]] = {
    case s: Sym[_] if !s.isType => Set(s)
    case b: Block[_] => exps(b.result) ++ exps(b.effects.antiDeps)
    case d: Op[_]    => d.expInputs.toSet
  }

  val symsFunc: PartialFunction[Any,Set[Sym[_]]] = {
    case s: Sym[_] if s.isSymbol => Set(s)
    case s: Sym[_] if s.isBound  => Set(s)
    case b: Block[_]    => syms(b.result) ++ syms(b.effects.antiDeps)
    case d: Op[_]       => d.inputs.toSet
  }

  final def exps(a: Any*): Set[Sym[_]] = recursive.collectSets(expsFunc)(a)
  final def syms(a: Any*): Set[Sym[_]] = recursive.collectSets(symsFunc)(a)
}
