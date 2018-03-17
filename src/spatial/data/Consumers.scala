package spatial.data

import core._

case class Consumers(users: Set[Sym[_]]) extends AnalysisData[Consumers]

object consumersOf {
  def apply(x: Sym[_]): Set[Sym[_]] = metadata[Consumers](x).map(_.users).getOrElse(Set.empty)
  def add(x: Sym[_], consumer: Sym[_]): Unit = metadata.add(x, Consumers(consumersOf(x) + consumer))
}
