package argon

case class Consumers(users: Set[Sym[_]]) extends Data[Consumers](SetBy.Flow.Consumer)
