package spatial.data

import argon._

case class ReduceCycle(x: Seq[Sym[_]]) extends FlowData[ReduceCycle]
object reduceCycleOf {
  def apply(x: Sym[_]): Seq[Sym[_]] = metadata[ReduceCycle](x).map(_.x).getOrElse(Nil)
  def update(x: Sym[_], cycle: Seq[Sym[_]]): Unit = metadata.add(x, ReduceCycle(cycle))
}
