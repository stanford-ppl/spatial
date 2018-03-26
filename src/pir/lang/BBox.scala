package pir.lang

import argon._
import forge.tags._

import pir.node._
import spatial.lang._

object BBox {
  @api def GEMM[T:Bits](a: T, b: T): T = stage(GEMMConfig(a,b))
  @api def GEMV[T:Bits](a: T, b: T): T = stage(GEMVConfig(a,b))
  @api def CONV[T:Bits](a: T, b: T): T = stage(CONVConfig(a,b))
  @api def Stream1[T:Bits](a1: T): T = stage(StreamConfig1(a1))
  @api def Stream2[T:Bits](a1: T, a2: T): T = stage(StreamConfig2(a1, a2))
  @api def Stream3[T:Bits](a1: T, a2: T, a3: T): T = stage(StreamConfig3(a1, a2, a3))
  @api def Stream4[T:Bits](a1: T, a2: T, a3: T, a4: T): T = stage(StreamConfig4(a1, a2, a3, a4))
}
