package argon

case class ShallowAliases(aliases: Set[Sym[_]]) extends AnalysisData[ShallowAliases]

case class DeepAliases(aliases: Set[Sym[_]]) extends AnalysisData[DeepAliases]
