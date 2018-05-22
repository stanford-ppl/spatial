package argon

case class ShallowAliases(aliases: Set[Sym[_]]) extends Data[ShallowAliases](transfer = Transfer.Ignore)

case class DeepAliases(aliases: Set[Sym[_]]) extends Data[DeepAliases](transfer = Transfer.Ignore)
