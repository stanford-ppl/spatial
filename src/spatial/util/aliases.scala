package spatial.util

import argon._
import spatial.data._
import spatial.lang._

object aliases {

  implicit class AliasOps[A](x: Sym[A]) {
    def start_=(start: () => Seq[Idx]): Unit = metadata.add(x, AliasStart(start))
    def start(): Seq[Idx] = metadata[AliasStart](x).get.start()

    def stride_=(stride: () => Seq[Idx]): Unit = metadata.add(x, AliasStride(stride))
    def stride(): Seq[Idx] = metadata[AliasStride](x).get.stride()

    def end_=(end: () => Seq[Idx]): Unit = metadata.add(x, AliasEnd(end))
    def end(): Seq[Idx] = metadata[AliasEnd](x).get.end()

    def pars_=(par: () => Seq[I32]): Unit = metadata.add(x, AliasPar(par))
    def pars(): Seq[I32] = metadata[AliasPar](x).get.par()

    def units_=(unit: () => Seq[Boolean]): Unit = metadata.add(x, AliasUnit(unit))
    def units(): Seq[Boolean] = metadata[AliasUnit](x).get.unit()
  }

}
