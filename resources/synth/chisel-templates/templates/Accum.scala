package templates

import ops._
import chisel3._
import types._

class UIntAccum(val w: Int, val lambda: String) extends Module {
  def this(tuple: (Int, String)) = this(tuple._1, tuple._2)

  val io = IO(new Bundle{
    val next = Input(UInt(w.W))
    val enable = Input(Bool())
    val reset = Input(Bool())
    val init = Input(UInt(w.W))
    val output = Output(UInt(w.W))
  })

  val current = RegInit(io.init)
  val asyncCurrent = Mux(io.reset, io.init, current)
  val update = lambda match { 
    case "add" => asyncCurrent + io.next
    case "max" => Mux(asyncCurrent > io.next, asyncCurrent, io.next)
    case "min" => Mux(asyncCurrent < io.next, asyncCurrent, io.next)
    case _ => asyncCurrent
  }
  current := Mux(io.enable, update, asyncCurrent)

  io.output := asyncCurrent

  def read(port: Int) = {
    io.output
  }

}

// latency = lanes for accumulation

class SpecialAccum(val latency: Int, val lambda: String, val typ: String, val params: List[Int]) extends Module {
  def this(tuple: (Int, String, String, List[Int])) = this(tuple._1, tuple._2, tuple._3, tuple._4)

  val w = if (typ == "FixedPoint") params.drop(1).reduce{_+_} else params.reduce{_+_}

  val io = IO(new Bundle{
    val input = new Bundle{
      val next = Input(UInt(w.W))
      val enable = Input(Bool())
      val direct_enable = Input(Bool())
      val direct_data = Input(UInt(w.W))
      val reset = Input(Bool())
      val init = Input(UInt(w.W))      
    }
    val output = Output(UInt(w.W))
  })


  typ match { // Reinterpret "io.next" bits
    case "UInt" => 
      val current = RegInit(io.input.init)
      val asyncCurrent = Mux(io.input.reset, io.input.init, current)
      val update = lambda match { 
        case "add" => asyncCurrent + io.input.next
        case "max" => Mux(asyncCurrent > io.input.next, asyncCurrent, io.input.next)
        case "min" => Mux(asyncCurrent < io.input.next, asyncCurrent, io.input.next)
        case _ => asyncCurrent
      }
      current := Mux(io.input.direct_enable, io.input.direct_data, Mux(io.input.enable, update, asyncCurrent))
      io.output := asyncCurrent
    case "FixedPoint" => 
      val current = RegInit(io.input.init)
      val asyncCurrent = Mux(io.input.reset, io.input.init, current)

      val fixnext = Wire(new types.FixedPoint(params(0), params(1), params(2)))
      val fixcurrent = Wire(new types.FixedPoint(params(0), params(1), params(2))) 
      val fixasync = Wire(new types.FixedPoint(params(0), params(1), params(2))) 
      fixnext.number := io.input.next
      fixcurrent.number := current
      fixasync.number := asyncCurrent
      val update = lambda match { 
        case "add" => fixcurrent + fixnext
        case "max" => Mux(fixasync > fixnext, fixasync, fixnext)
        case "min" => Mux(fixasync < fixnext, fixasync, fixnext)
        case _ => fixasync
      }
      current := Mux(io.input.direct_enable, io.input.direct_data, Mux(io.input.enable, update.number, fixasync.number))

      io.output := asyncCurrent
    case "FloatingPoint" => // Not implemented
      val current = RegInit(io.input.init)
      val asyncCurrent = Mux(io.input.reset, io.input.init, current)
      val update = lambda match { 
        case "add" => asyncCurrent + io.input.next
        case "max" => Mux(asyncCurrent > io.input.next, asyncCurrent, io.input.next)
        case "min" => Mux(asyncCurrent < io.input.next, asyncCurrent, io.input.next)
        case _ => asyncCurrent
      }
      current := Mux(io.input.direct_enable, io.input.direct_data, Mux(io.input.enable, update, asyncCurrent))
      io.output := asyncCurrent  }

  def write[T](data: T, en: Bool, reset: Bool, port: List[Int]) = {
    io.input.direct_enable := en
    data match {
      case d: UInt =>
        io.input.direct_data := d
      case d: types.FixedPoint =>
        io.input.direct_data := d.number
    }
  }

  def read(port: Int) = {
    io.output
  }

}
