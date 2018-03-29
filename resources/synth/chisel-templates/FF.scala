package templates

import chisel3._
import types._

/**
 * FF: Flip-flop with the ability to set enable and init
 * value as IO
 * @param w: Word width
 */

class FFIn(val w: Int) extends Bundle {
  val data   = UInt(w.W)
  val init = UInt(w.W)
  val enable = Bool()
  val reset = Bool() // Asynchronous reset

  override def cloneType = (new FFIn(w)).asInstanceOf[this.type] // See chisel3 bug 358
}
class FFOut(val w: Int) extends Bundle {
  val data  = UInt(w.W)

  override def cloneType = (new FFOut(w)).asInstanceOf[this.type] // See chisel3 bug 358
}

class FF(val w: Int, val numWriters: Int = 1) extends Module {
  val io = IO(new Bundle{
    val input = Vec(numWriters, Input(new FFIn(w)))
    val output = Output(new FFOut(w))
  })
  
  val ff = RegInit(io.input(0).init)
  val anyReset = io.input.map{_.reset}.reduce{_|_}
  val anyEnable = io.input.map{_.enable}.reduce{_|_}
  val wr_data = chisel3.util.Mux1H(io.input.map{_.enable}, io.input.map{_.data})
  ff := Mux(anyReset, io.input(0).init, Mux(anyEnable, wr_data, ff))
  io.output.data := Mux(anyReset, io.input(0).init, ff)

  var wId = 0
  def write[T,A](data: T, en: Bool, reset: Bool, port: List[Int], init: A, accumulating: Boolean): Unit = {
    data match {
      case d: UInt =>
        io.input(wId).data := d
      case d: SInt => 
        io.input(wId).data := d.asUInt
      case d: types.FixedPoint =>
        io.input(wId).data := d.number
    }
    init match {
      case d: UInt =>
        io.input(wId).init := d
      case d: SInt => 
        io.input(wId).init := d.asUInt
      case d: types.FixedPoint =>
        io.input(wId).init := d.number
    }
    io.input(wId).enable := en
    if (w == 1 || accumulating) {
      io.input(wId).reset := Utils.getRetimed(reset, 1) // Hack to fix correctness bug in MD_KNN and comb loop in Sort_Radix simultaneously, since accum on boolean regs can cause this loop
    } else {
      io.input(wId).reset := Utils.getRetimed(reset, 1) & ~en 
    }
    // Ignore port
    wId = wId + 1
  }

  def write[T,A](data: T, en: Bool, reset: Bool, port: Int, init: A, accumulating: Boolean): Unit = {
    write(data, en, reset, List(port), init, accumulating)
  }

  def read(port: Int) = {
    io.output.data
  }

}

class NBufFF(val numBufs: Int, val w: Int, val numWriters: Int = 1) extends Module {

  // Define overloaded
  def this(tuple: (Int, Int)) = this(tuple._1, tuple._2)

  val io = IO(new Bundle {
    val sEn = Vec(numBufs, Input(Bool()))
    val sDone = Vec(numBufs, Input(Bool()))
    val broadcast = Input(new FFIn(w))
    val input = Vec(numWriters, Input(new FFIn(w)))
    val wr_ports = Vec(numWriters, Input(UInt(32.W)))
    val writerStage = Input(UInt(5.W)) // TODO: Not implemented anywhere, not sure if needed
    val output = Vec(numBufs, Output(new FFOut(w)))
    // val swapAlert = Output(Bool()) // Used for regchains
  })

  def bitsToAddress(k:Int) = {(scala.math.log(k)/scala.math.log(2)).toInt + 1}
  // def rotate[T](x: Vec[T], i:Int)={ // Chisel is so damn annoying with types, so this method doesn't work
  //   val temp = x.toList
  //   val result = x.drop(i)++x.take(i)
  //   Vec(result.toArray)
  // }

  val ff = (0 until numBufs).map{i => Module(new FF(w))}

  val sEn_latch = (0 until numBufs).map{i => Module(new SRFF())}
  val sDone_latch = (0 until numBufs).map{i => Module(new SRFF())}

  val swap = Wire(Bool())

  // Latch whether each buffer's stage is enabled and when they are done
  (0 until numBufs).foreach{ i => 
    sEn_latch(i).io.input.set := (io.sEn(i) & ~io.sDone(i)) || (io.sEn(i) & io.sDone(i) & Utils.getRetimed(~io.sEn(i), 1) /*Special case when en and done go on at same time in first cycle*/)
    sEn_latch(i).io.input.reset := swap || Utils.getRetimed(swap, 1)
    sEn_latch(i).io.input.asyn_reset := Utils.getRetimed(reset, 1)
    sDone_latch(i).io.input.set := io.sDone(i)
    sDone_latch(i).io.input.reset := swap || Utils.getRetimed(swap, 1)
    sDone_latch(i).io.input.asyn_reset := Utils.getRetimed(reset, 1)
  }
  val anyEnabled = sEn_latch.map{ en => en.io.output.data }.reduce{_|_}
  swap := Utils.risingEdge(sEn_latch.zip(sDone_latch).zipWithIndex.map{ case ((en, done), i) => en.io.output.data === (done.io.output.data || io.sDone(i)) }.reduce{_&_} & anyEnabled)
  // swap := sEn_latch.zip(sDone_latch).zipWithIndex.map{ case ((en, done),i) => (en.io.output.data === done.io.output.data) || (en.io.output.data && io.sDone(i)) }.reduce{_&_} & anyEnabled
  // io.swapAlert := ~swap & anyEnabled & (0 until numBufs).map{ i => sEn_latch(i).io.output.data === (sDone_latch(i).io.output.data | io.sDone(i))}.reduce{_&_} // Needs to go high when the last done goes high, which is 1 cycle before swap goes high

  val statesIn = (0 until numWriters).map{ i => 
    val c = Module(new NBufCtr(1,None, Some(numBufs), 1+Utils.log2Up(numBufs)))
    c.io.input.start := io.wr_ports(i) 
    c.io.input.enable := swap
    c.io.input.countUp := false.B
    c
  }

  val statesOut = (0 until numBufs).map{  i => 
    val c = Module(new NBufCtr(1,Some(i), Some(numBufs), 1+Utils.log2Up(numBufs)))
    c.io.input.enable := swap
    c.io.input.countUp := false.B
    c
  }

  ff.zipWithIndex.foreach{ case (f,i) => 
    val wrMask = statesIn.zipWithIndex.map{ case (si, ii) => si.io.output.count === i.U }
    val normal =  Wire(new FFIn(w))
    // val selected = chisel3.util.Mux1H(wrMask, io.input)
    normal.data := chisel3.util.Mux1H(wrMask, (0 until numWriters).map{i => io.input(i).data})
    normal.init := chisel3.util.Mux1H(wrMask, (0 until numWriters).map{i => io.input(i).init})
    normal.enable := chisel3.util.Mux1H(wrMask, (0 until numWriters).map{i => io.input(i).enable}) & wrMask.reduce{_|_}
    normal.reset := chisel3.util.Mux1H(wrMask, (0 until numWriters).map{i => io.input(i).reset})
    f.io.input(0) := Mux(io.broadcast.enable, io.broadcast, normal)
  }

  io.output.zip(statesOut).foreach{ case (wire, s) => 
    val sel = (0 until numBufs).map{ i => s.io.output.count === i.U }
    wire.data := chisel3.util.Mux1H(sel, Vec(ff.map{f => f.io.output.data}))
  }

  var wId = 0
  def write[T,A](data: T, en: Bool, reset: Bool, port: Int, init: A, accumulating: Boolean): Unit = {
    write(data, en, reset, List(port), init, accumulating)
  }

  def write[T,A](data: T, en: Bool, reset: Bool, ports: List[Int], init: A, accumulating: Boolean): Unit = {
    if (ports.length == 1) {
      val port = ports(0)
      data match { 
        case d: UInt => 
          io.input(wId).data := d
        case d: SInt => 
          io.input(wId).data := d.asUInt
        case d: types.FixedPoint => 
          io.input(wId).data := d.number
      }
      init match { 
        case d: UInt => 
          io.input(wId).init := d
        case d: SInt => 
          io.input(wId).init := d.asUInt
        case d: types.FixedPoint => 
          io.input(wId).init := d.number
      }
      io.input(wId).enable := en
      if (w == 1 || accumulating) {
        io.input(wId).reset := Utils.getRetimed(reset, 1) // Hack to fix correctness bug in MD_KNN and comb loop in Sort_Radix simultaneously, since accum on boolean regs can cause this loop
      } else {
        io.input(wId).reset := Utils.getRetimed(reset, 1) & ~en 
      }
      
      io.wr_ports(wId) := port.U
      wId = wId + 1
    } else {
      data match { 
        case d: UInt => 
          io.broadcast.data := d
        case d: types.FixedPoint => 
          io.broadcast.data := d.number
      }
      io.broadcast.enable := en & ~en
      io.broadcast.reset := Utils.getRetimed(reset, 1)      
    }
  }

  def chain_pass[T](dat: T, en: Bool) { // Method specifically for handling reg chains that pass counter values between metapipe stages
    dat match {
      case data: UInt => 
        io.input(0).data := data
      case data: FixedPoint => 
        io.input(0).data := data.number
    }
    io.input(0).enable := en
    io.input(0).reset := Utils.getRetimed(reset, 1)
    io.input(0).init := 0.U
    io.wr_ports(0) := 0.U
    io.broadcast.enable := false.B

  }

  def connectStageCtrl(done: Bool, en: Bool, ports: List[Int]) {
    ports.foreach{ port => 
      io.sEn(port) := en
      io.sDone(port) := done
    }
  }

  def connectUnwrittenPorts(ports: List[Int]) { // TODO: Remnant from maxj?
    // ports.foreach{ port => 
    //   io.input(port).enable := false.B
    // }
  }
 
  def connectUnreadPorts(ports: List[Int]) { // TODO: Remnant from maxj?
    // Used for SRAMs
  }

  def connectUntouchedPorts(ports: List[Int]) {
    ports.foreach{ port => 
      io.sEn(port) := false.B
      io.sDone(port) := false.B
    }
  }

  def connectDummyBroadcast() {
    io.broadcast.enable := false.B
  }

  def read(port: Int) = {
    io.output(port).data
  }

}

class FFNoInit(val w: Int) extends Module {
  val io = IO(new Bundle{
    val input = Input(new FFIn(w))
    val output = Output(new FFOut(w))
  })

  val ff = Module(new FF(w))
  ff.io.input(0).data := io.input.data
  ff.io.input(0).enable := io.input.enable
  ff.io.input(0).reset := io.input.reset
  ff.io.input(0).init := 0.U(w.W)
  io.output.data := ff.io.output.data
}

class FFNoInitNoReset(val w: Int) extends Module {
  val io = IO(new Bundle{
    val input = Input(new FFIn(w))
    val output = Output(new FFOut(w))
  })

  val ff = Module(new FF(w))
  ff.io.input(0).data := io.input.data
  ff.io.input(0).enable := io.input.enable
  ff.io.input(0).reset := false.B
  ff.io.input(0).init := 0.U(w.W)
  io.output.data := ff.io.output.data
}

class FFNoReset(val w: Int) extends Module {
  val io = IO(new Bundle{
    val input = Input(new FFIn(w))
    val output = Output(new FFOut(w))
  })

  val ff = Module(new FF(w))
  ff.io.input(0).data := io.input.data
  ff.io.input(0).enable := io.input.enable
  ff.io.input(0).reset := false.B
  ff.io.input(0).init := io.input.init
  io.output.data := ff.io.output.data
}

class TFF() extends Module {

  // Overload with null string input for testing
  def this(n: String) = this()

  val io = IO(new Bundle {
    val input = new Bundle {
      val enable = Input(Bool())
    }
    val output = new Bundle {
      val data = Output(Bool())      
    }
  })

  val ff = RegInit(false.B)
  ff := Mux(io.input.enable, ~ff, ff)
  io.output.data := ff
}

class SRFF(val strongReset: Boolean = false) extends Module {

  // Overload with null string input for testing
  def this(n: String) = this()

  val io = IO(new Bundle {
    val input = new Bundle {
      val set = Input(Bool()) // Set overrides reset.  Asyn_reset overrides both
      val reset = Input(Bool())
      val asyn_reset = Input(Bool())
    }
    val output = new Bundle {
      val data = Output(Bool())      
    }
  })

  if (!strongReset) { // Set + reset = on
    val ff = RegInit(false.B)
    ff := Mux(io.input.asyn_reset, false.B, Mux(io.input.set, 
                                    true.B, Mux(io.input.reset, false.B, ff)))
    io.output.data := Mux(io.input.asyn_reset, false.B, ff)
  } else { // Set + reset = off
    val ff = RegInit(false.B)
    ff := Mux(io.input.asyn_reset, false.B, Mux(io.input.reset, 
                                    false.B, Mux(io.input.set, true.B, ff)))
    io.output.data := Mux(io.input.asyn_reset, false.B, ff)

  }
}


