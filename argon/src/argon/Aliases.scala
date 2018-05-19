package argon

case class ShallowAliases(aliases: Set[Sym[_]]) extends Data[ShallowAliases](SetBy.Flow.Self)

case class DeepAliases(aliases: Set[Sym[_]]) extends Data[DeepAliases](SetBy.Flow.Self)
