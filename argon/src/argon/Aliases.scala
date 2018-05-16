package argon

case class ShallowAliases(aliases: Set[Sym[_]]) extends StableData[ShallowAliases] {
  /** Always prefer the old aliases metadata since this is updated during staging. */
  override def merge(old: ShallowAliases): Data[ShallowAliases] = old
}

case class DeepAliases(aliases: Set[Sym[_]]) extends StableData[DeepAliases] {
  /** Always prefer the old aliases metadata since this is updated during staging. */
  override def merge(old: DeepAliases): Data[DeepAliases] = old
}
