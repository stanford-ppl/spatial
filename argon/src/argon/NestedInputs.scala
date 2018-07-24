package argon

case class NestedInputs(inputs: Set[Sym[_]]) extends Data[NestedInputs](Transfer.Remove)
