package spatial.lang.api

import forge.tags._
import argon._

import spatial.node.{Mux,OneHotMux,PriorityMux}

trait MuxAPI { this: Implicits =>

  @api def mux[A](s: Bit, a: Bits[A], b: Bits[A]): A = {
    implicit val tA: Bits[A] = a.selfType
    stage(Mux(s,a,b))
  }
  @api def mux[A](s: Bit, a: Bits[A], b: Literal): A = {
    implicit val tA: Bits[A] = a.selfType
    stage(Mux(s, a, tA.from(b.value)))
  }
  @api def mux[A](s: Bit, a: Literal, b: Bits[A]): A = {
    implicit val tB: Bits[A] = b.selfType
    stage(Mux(s, tB.from(a.value), b))
  }
  @api def mux[A:Bits](s: Bit, a: Lift[A], b: Lift[A]): A = {
    stage(Mux(s,a.unbox,b.unbox))
  }

  @api def oneHotMux[A:Bits](sels: Seq[Bit], vals: Seq[A]): A = {
    stage(OneHotMux(sels,vals.map{s => boxBits(s) }))
  }

  @api def priorityMux[A:Bits](sels: Seq[Bit], vals: Seq[A]): A = {
    stage(PriorityMux(sels,vals.map{s => boxBits(s) }))
  }

}
