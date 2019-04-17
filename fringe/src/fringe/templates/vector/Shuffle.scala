package fringe.templates.vector

import chisel3._
import chisel3.util._

import fringe._
import fringe.utils._

class ShuffleCompressNetwork[T <: Data](t: T, v: Int) extends Module {
  class ShuffleItem extends Bundle {
    val d = t.cloneType
    val m = Bool()
  }

  val io = IO(new Bundle {
    val in = Input(Vec(v, new ShuffleItem))
    val out = Output(Vec(v, new ShuffleItem))
  })

  def stageMap(v: Int) = {
    def levelEven = {
      List.range(0, v).sliding(2, 2).map { case List(a, b) => (a, b) }
    }

    def levelOdd = {
      (0, 0) +: List.range(1, v - 1).sliding(2, 2).map { case List(a, b) => (a, b) }.toList :+ (v - 1, v - 1)
    }

    List.tabulate(v) { i => if (i % 2 == 0) levelEven else levelOdd }
  }
  
  val stages = List.fill(v) { Wire(Vec(v, new ShuffleItem)) }

  stageMap(v).zipWithIndex.foreach { case (stage, i) =>
    val in = if (i == 0) io.in else stages(i - 1)
    val out = VecInit(stage.flatMap { case (a, b) =>
      val s = in(a).m
      if (a == b) List(in(a)) else List(
        Mux(s, in(a), in(b)),
        Mux(s, in(b), in(a))
      )
    }.toList)
    stages(i) := out //getRetimed(out, 1)
  }

  io.out := stages.last
}

object Shuffle {
  def compress[T <: Data](data: Vec[T], mask: Vec[Bool]) = {
    val m = Module(new ShuffleCompressNetwork(data(0), data.length))
    m.io.in.zipWithIndex.foreach { case(in, i) =>
      in.d := data(i)
      in.m := mask(i)
    }
    (VecInit(m.io.out.map { _.d }), VecInit(m.io.out.map { _.m }))
  }
}
