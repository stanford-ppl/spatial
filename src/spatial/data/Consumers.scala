package spatial.data

import core._

case class Consumers(users: Seq[Sym[_]]) extends AnalysisData[Consumers]

object consumersOf {
  def apply(x: Sym[_]): Seq[Sym[_]] = metadata[Consumers](x).map(_.users).getOrElse(Nil)
  def add(x: Sym[_], consumer: Sym[_]): Unit = metadata.add(x, Consumers(consumer +: consumersOf(x)))
}
