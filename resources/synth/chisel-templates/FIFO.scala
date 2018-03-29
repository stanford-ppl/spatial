package templates

import ops._
import chisel3._
import chisel3.util
import chisel3.util.Mux1H
import scala.math._
import fringe._




class FIFO(val pR: Int, val pW: Int, val depth: Int, val numWriters: Int, val numReaders: Int, val bitWidth: Int = 32) extends Module {
  def this(tuple: (Int, Int, Int, Int, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)
  def this(p: Int, depth: Int) = this(p, p, 1, 1, depth)

  val io = IO( new Bundle {
    val in = Vec(pW*-*numWriters, Input(UInt(bitWidth.W)))
    val out = Vec(pR*-*numReaders, Output(UInt(bitWidth.W)))
    val enq = Vec(numWriters, Input(Bool()))
    val deq = Vec(numReaders, Input(Bool()))
    val numel = Output(UInt(32.W))
    val almostEmpty = Output(Bool())
    val almostFull = Output(Bool())
    val empty = Output(Bool())
    val full = Output(Bool())
    val debug = new Bundle {
      val overwrite = Output(Bool())
      val overread = Output(Bool())
      val error = Output(Bool())
    }
  })

  // TODO: Assert pW and pR evenly divide p
  val p = scala.math.max(pW,pR)

  val enq_options = Vec((0 until numWriters).map{ q => io.enq(q)}.toList)
  val deq_options = Vec((0 until numReaders).map{ q => io.deq(q)}.toList)

  // Register for tracking number of elements in FIFO
  val elements = Module(new IncDincCtr(pW, pR, depth))
  elements.io.input.inc_en := enq_options.reduce{_|_}
  elements.io.input.dinc_en := deq_options.reduce{_|_}

  // Create physical mems
  val m = (0 until p).map{ i => Module(new Mem1D(depth/p, bitWidth))}

  // Create head and reader sub counters
  val sw_width = 2 + Utils.log2Up(p/pW)
  val sr_width = 2 + Utils.log2Up(p/pR)
  val subWriter = Module(new SingleCounter(1,Some(0),Some(p/pW),Some(1), Some(0),sw_width))
  val subReader = Module(new SingleCounter(1,Some(0),Some(p/pR),Some(1), Some(0),sr_width))
  subWriter.io.input.enable := enq_options.reduce{_|_}
  subWriter.io.input.reset := reset
  subWriter.io.input.saturate := false.B
  subReader.io.input.enable := deq_options.reduce{_|_}
  subReader.io.input.reset := reset
  subReader.io.input.saturate := false.B

  // Create head and reader counters
  val a_width = 2 + Utils.log2Up(depth/p)
  val writer = Module(new SingleCounter(1,Some(0), Some(depth/p), Some(1), Some(0), a_width))
  val reader = Module(new SingleCounter(1,Some(0), Some(depth/p), Some(1), Some(0), a_width))
  writer.io.input.enable := enq_options.reduce{_|_} & subWriter.io.output.done
  writer.io.input.reset := reset
  writer.io.input.saturate := false.B
  reader.io.input.enable := deq_options.reduce{_|_} & subReader.io.output.done
  reader.io.input.reset := reset
  reader.io.input.saturate := false.B  

  // Connect enqer
  if (pW == pR) {
    m.zipWithIndex.foreach { case (mem, i) => 
      val data_options = Vec((0 until numWriters).map{ q => io.in(q*-*pW + i)}.toList)
      mem.io.w.addr := writer.io.output.count(0).asUInt
      mem.io.w.data := Mux1H(enq_options, data_options)
      mem.io.w.en := enq_options.reduce{_|_}
    }
  } else {
    (0 until pW).foreach { w_i => 
      (0 until (p /-/ pW)).foreach { i => 
        val data_options = Vec((0 until numWriters).map{ q => io.in(q*-*pW + w_i)}.toList)
        m(w_i + i*-*pW).io.w.addr := writer.io.output.count(0).asUInt
        m(w_i + i*-*pW).io.w.data := Mux1H(enq_options, data_options)
        m(w_i + i*-*pW).io.w.en := enq_options.reduce{_|_} & (subWriter.io.output.count(0) === i.S(sw_width.W))
      }
    }
  }

  // Connect deqper
  if (pW == pR) {
    m.zipWithIndex.foreach { case (mem, i) => 
      mem.io.r.addr := reader.io.output.count(0).asUInt
      mem.io.r.en := deq_options.reduce{_|_}
      io.out(i) := mem.io.output.data
    }
  } else {
    (0 until pR).foreach { r_i => 
      val rSel = Wire(Vec( (p/pR), Bool()))
      val rData = Wire(Vec( (p/pR), UInt(32.W)))
      (0 until (p /-/ pR)).foreach { i => 
        m(r_i + i*-*pR).io.r.addr := reader.io.output.count(0).asUInt
        m(r_i + i*-*pR).io.r.en := deq_options.reduce{_|_} & (subReader.io.output.count(0) === i.S(sr_width.W))
        rSel(i) := subReader.io.output.count(0) === i.S(sr_width.W)
        // if (i == 0) { // Strangeness from inc-then-read nuisance
        //   rSel((p/pR)-1) := subReader.io.output.count(0) === i.U
        // } else {
        //   rSel(i-1) := subReader.io.output.count(0) === i.U
        // }
        rData(i) := m(r_i + i*-*pR).io.output.data
      }
      io.out(r_i) := chisel3.util.PriorityMux(rSel, rData)
    }
  }

  // Check if there is data
  io.empty := elements.io.output.empty
  io.full := elements.io.output.full
  io.almostEmpty := elements.io.output.almostEmpty
  io.almostFull := elements.io.output.almostFull
  io.numel := elements.io.output.numel.asUInt

  // Debug signals
  io.debug.overread := elements.io.output.overread
  io.debug.overwrite := elements.io.output.overwrite
  io.debug.error := elements.io.output.overwrite | elements.io.output.overread

  var wId = 0
  def connectEnqPort(data: Vec[UInt], en: Bool): Unit = {
    (0 until data.length).foreach{ i => 
      io.in(wId*-*pW + i) := data(i)
    }
    io.enq(wId) := en
    wId += 1
  }

  var rId = 0
  def connectDeqPort(en: Bool): Vec[UInt] = {
    io.deq(rId) := en
    rId += 1
    io.out
  }

  // // Old empty and error tracking
  // val ovW = Module(new SRFF())
  // val ovR = Module(new SRFF())
  // val www_c = writer.io.output.countWithoutWrap(0)*-*(p/pW).U + subWriter.io.output.count(0)
  // val w_c = writer.io.output.count(0)*-*(p/pW).U + subWriter.io.output.count(0)
  // val rww_c = reader.io.output.countWithoutWrap(0)*-*(p/pR).U + subReader.io.output.count(0)
  // val r_c = reader.io.output.count(0)*-*(p/pR).U + subReader.io.output.count(0)
  // val hasData = Module(new SRFF())
  // hasData.io.input.set := (w_c === r_c) & io.enq & !(ovR.io.output.data | ovW.io.output.data)
  // hasData.io.input.reset := (r_c + 1.U === www_c) & io.deq & !(ovR.io.output.data | ovW.io.output.data)
  // io.empty := !hasData.io.output.data

  // // Debugger
  // val overwrite = hasData.io.output.data & (w_c === r_c) & io.enq // TODO: Need to handle sub-counters
  // val fixed_overwrite = (www_c === r_c + 1.U) & io.deq
  // ovW.io.input.set := overwrite
  // ovW.io.input.reset := fixed_overwrite
  // io.debug.overwrite := ovW.io.output.data
  // val overread = !hasData.io.output.data & (w_c === r_c) & io.deq // TODO: Need to handle sub-counters
  // val fixed_overread = (w_c + 1.U === rww_c) & io.enq
  // ovR.io.input.set := overread
  // ovR.io.input.reset := fixed_overread
  // io.debug.overread := ovR.io.output.data

  // io.debug.error := ovR.io.output.data | ovW.io.output.data


}

class enqPort(val w: Int) extends Bundle {
  val data = UInt(w.W)
  val en = Bool()

  override def cloneType = (new enqPort(w)).asInstanceOf[this.type] // See chisel3 bug 358
}

class Compactor(val ports: List[Int], val banks: Int, val width: Int, val bitWidth: Int = 32) extends Module {
  val num_compactors = ports.max
  val io = IO( new Bundle {
      val numEnabled =Input(UInt(width.W))
      val in = Vec(ports.reduce{_+_}, Input(new enqPort(bitWidth)))
      val out = Vec(num_compactors, Output(new enqPort(bitWidth)))
    })

    val compacted = (0 until num_compactors).map{i => 
      val num_inputs_per_bundle = ports.map{p => if (i < p) p-i else 0}
      val mux_selects = num_inputs_per_bundle.zipWithIndex.map{case(j, id) => 
        val in_start_id = ports.take(id).sum
        val connect_start_id = num_inputs_per_bundle.take(id).sum
        val num_holes = if ((ports(id)-j) > 0) {
          (0 until (ports(id)-j)).map{ k => Mux(!io.in(in_start_id + k).en, 1.U(width.W), 0.U(width.W)) }.reduce{_+_} // number of 0's in this bundle that precede current
        } else {
          0.U(width.W)          
        }
        (0 until j).map{k => 
          val ens_below = if (k > 0) {(0 until k).map{l => Mux(io.in(in_start_id + (ports(id) - j + l)).en, 1.U(width.W), 0.U(width.W)) }.reduce{_+_}} else {0.U(width.W)}
          io.in(in_start_id + (ports(id) - j + k)).en & ens_below >= num_holes
        }
      }.flatten
      val mux_datas = num_inputs_per_bundle.zipWithIndex.map{case(j, id) => 
        val in_start_id = ports.take(id).sum
        val connect_start_id = num_inputs_per_bundle.take(id).sum
        (0 until j).map{k => io.in(in_start_id + (ports(id) - j + k)).data}
      }.flatten
      io.out(i).data := chisel3.util.PriorityMux(mux_selects, mux_datas)
      io.out(i).en := i.U(width.W) < io.numEnabled
    }
}

/* This consists of an innermost compactor, surrounded by a router.  The compactor
   takes all of the enq ports in, has as many priority muxes as required for the largest
   enq port bundle, and outputs the compacted enq port bundle.  The shifter takes this 
   compacted bundle and shifts it so that they get connected to the correct fifo banks
   outside of the module
*/
class CompactingEnqNetwork(val ports: List[Int], val banks: Int, val width: Int, val bitWidth: Int = 32) extends Module {
  val io = IO( new Bundle {
      val headCnt = Input(SInt(width.W))
      val in = Vec(ports.reduce{_+_}, Input(new enqPort(bitWidth)))
      val out = Vec(banks, Output(new enqPort(bitWidth)))
      val debug1 = Output(Bool())
      val debug2 = Output(Bool())
    })

  val numEnabled = io.in.map{i => Mux(i.en, 1.U(width.W), 0.U(width.W))}.reduce{_+_}
  val num_compactors = ports.max

  // Compactor
  val compactor = Module(new Compactor(ports, banks, width, bitWidth))
  compactor.io.in := io.in
  compactor.io.numEnabled := numEnabled

  // Router
  val current_base_bank = Utils.singleCycleModulo(io.headCnt, banks.S(width.W))
  val upper = current_base_bank + numEnabled.asSInt - banks.S(width.W)
  val num_straddling = Mux(upper < 0.S(width.W), 0.S(width.W), upper)
  val num_straight = (numEnabled.asSInt) - num_straddling
  val outs = (0 until banks).map{ i =>
    val lane_enable = Mux(i.S(width.W) < num_straddling | (i.S(width.W) >= current_base_bank & i.S(width.W) < current_base_bank + numEnabled.asSInt), true.B, false.B)
    val id_from_base = Mux(i.S(width.W) < num_straddling, i.S(width.W) + num_straight, i.S(width.W) - current_base_bank)
    val port_vals = (0 until num_compactors).map{ i => 
      (i.U(width.W) -> compactor.io.out(i).data)
    }
    val lane_data = chisel3.util.MuxLookup(id_from_base.asUInt, 0.U(bitWidth.W), port_vals)
    (lane_data,lane_enable)
  }

  (0 until banks).foreach{i => 
    io.out(i).data := outs(i)._1
    io.out(i).en := outs(i)._2
  }
}

class CompactingDeqNetwork(val ports: List[Int], val banks: Int, val width: Int, val bitWidth: Int = 32) extends Module {
  val io = IO( new Bundle {
      val tailCnt = Input(SInt(width.W))
      val input = new Bundle{
        val data = Vec(banks, Input(UInt(bitWidth.W)))
        val deq = Vec(ports.reduce{_+_}, Input(Bool()))
      }
      val output = new Bundle{
        val data = Vec(ports.max, Output(UInt(bitWidth.W)))
      }
    })

  // Compactor
  val num_compactors = ports.max
  // val numPort_width = 1 + Utils.log2Up(ports.max)
  val numEnabled = io.input.deq.map{i => Mux(i, 1.U(width.W), 0.U(width.W))}.reduce{_+_}

  // Router
  val current_base_bank = Utils.singleCycleModulo(io.tailCnt, banks.S(width.W))
  val upper = current_base_bank + numEnabled.asSInt - banks.S(width.W)
  val num_straddling = Mux(upper < 0.S(width.W), 0.S(width.W), upper)
  val num_straight = (numEnabled.asSInt) - num_straddling
  // TODO: Probably has a bug if you have more than one dequeuer
  (0 until ports.max).foreach{ i =>
    val id_from_base = Mux(i.S(width.W) < num_straddling, i.S(width.W) + num_straight, Utils.singleCycleModulo((i.S(width.W) + current_base_bank), banks.S(width.W)))
    val ens_below = if (i>0) (0 until i).map{j => Mux(io.input.deq(j), 1.U(width.W), 0.U(width.W)) }.reduce{_+_} else 0.U(width.W)
    val proper_bank = Utils.singleCycleModulo((current_base_bank.asUInt + ens_below), banks.U(width.W))
    val port_vals = (0 until banks).map{ j => 
      (j.U(width.W) -> io.input.data(j)) 
    }
    io.output.data(i) := chisel3.util.MuxLookup(proper_bank.asUInt, 0.U(bitWidth.W), port_vals)
  }

}

class GeneralFIFO(val pR: List[Int], val pW: List[Int], val depth: Int, val bitWidth: Int = 32) extends Module {
  def this(tuple: (List[Int], List[Int], Int, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4)

  val io = IO( new Bundle {
    val in = Vec(pW.reduce{_+_}, Input(new enqPort(bitWidth)))
    val out = Vec(pR.max, Output(UInt(bitWidth.W)))
    val deq = Vec(pR.reduce{_+_}, Input(Bool()))
    val numel = Output(UInt(32.W))
    val almostEmpty = Output(Bool())
    val almostFull = Output(Bool())
    val empty = Output(Bool())
    val full = Output(Bool())
    val debug = new Bundle {
      val overwrite = Output(Bool())
      val overread = Output(Bool())
      val error = Output(Bool())
    }
  })

  // Create counters
  val width = 2 + Utils.log2Up(depth)

  val headCtr = Module(new CompactingCounter(pW.reduce{_+_}, depth, width))
  val tailCtr = Module(new CompactingCounter(pR.reduce{_+_}, depth, width))
  (0 until pW.reduce{_+_}).foreach{i => headCtr.io.input.enables(i) := io.in(i).en}
  (0 until pR.reduce{_+_}).foreach{i => tailCtr.io.input.enables(i) := io.deq(i)}
  headCtr.io.input.reset := reset
  tailCtr.io.input.reset := reset
  headCtr.io.input.dir := true.B
  tailCtr.io.input.dir := true.B

  // Register for tracking number of elements in FIFO
  val elements = Module(new CompactingIncDincCtr(pW.reduce{_+_}, pR.reduce{_+_}, depth, width))
  (0 until pW.reduce{_+_}).foreach{i => elements.io.input.inc_en(i) := io.in(i).en}
  (0 until pR.reduce{_+_}).foreach{i => elements.io.input.dinc_en(i) := io.deq(i)}

    //Mux(headCtr.io.output.count < tailCtr.io.output.count, headCtr.io.output.count + depth.S(width.W), headCtr.io.output.count - tailCtr.io.output.count)

  // Create physical mems
  val banks = max(pW.max, pR.max)
  val m = (0 until banks).map{ i => Module(new Mem1D(depth/banks, bitWidth))}

  // Create compacting network
  val enqCompactor = Module(new CompactingEnqNetwork(pW, banks, width, bitWidth))
  enqCompactor.io.headCnt := headCtr.io.output.count
  (0 until pW.reduce{_+_}).foreach{i => enqCompactor.io.in(i) := io.in(i)}

  // Connect compacting network to banks
  val active_w_bank = Utils.singleCycleModulo(headCtr.io.output.count, banks.S(width.W))
  val active_w_addr = Utils.singleCycleDivide(headCtr.io.output.count, banks.S(width.W))
  (0 until banks).foreach{i => 
    val addr = Mux(i.S(width.W) < active_w_bank, active_w_addr + 1.S(width.W), active_w_addr)
    m(i).io.w.addr := addr.asUInt
    m(i).io.w.data := enqCompactor.io.out(i).data
    m(i).io.w.en   := enqCompactor.io.out(i).en
  }

  // Create dequeue compacting network
  val deqCompactor = Module(new CompactingDeqNetwork(pR, banks, width, bitWidth))
  deqCompactor.io.tailCnt := tailCtr.io.output.count
  val active_r_bank = Utils.singleCycleModulo(tailCtr.io.output.count, banks.S(width.W))
  val active_r_addr = Utils.singleCycleDivide(tailCtr.io.output.count, banks.S(width.W))
  (0 until banks).foreach{i => 
    val addr = Mux(i.S(width.W) < active_r_bank, active_r_addr + 1.S(width.W), active_r_addr)
    m(i).io.r.addr := addr.asUInt
    deqCompactor.io.input.data(i) := m(i).io.output.data
  }
  (0 until pR.reduce{_+_}).foreach{i =>
    deqCompactor.io.input.deq(i) := io.deq(i)
  }
  (0 until pR.max).foreach{i =>
    io.out(i) := deqCompactor.io.output.data(i)
  }

  // Check if there is data
  io.empty := elements.io.output.empty
  io.full := elements.io.output.full
  io.almostEmpty := elements.io.output.almostEmpty
  io.almostFull := elements.io.output.almostFull
  io.numel := elements.io.output.numel.asUInt

  // Debug signals
  io.debug.overread := elements.io.output.overread
  io.debug.overwrite := elements.io.output.overwrite
  io.debug.error := elements.io.output.overwrite | elements.io.output.overread

  val wPort_mask = scala.collection.mutable.MutableList((0 until pW.length).map{i => 0}:_*)
  def connectEnqPort(data: Vec[UInt], en: Vec[Bool]): Unit = {
    // Figure out which bundle this port can go with
    val bundle_id = wPort_mask.zip(pW).indexWhere{a => a._2 == data.length && a._1 == 0}
    wPort_mask(bundle_id) = 1
    val base = pW.take(bundle_id).sum

    (0 until data.length).foreach{ i => 
      io.in(base + i).data := data(i)
      io.in(base + i).en := en(i)
    }
  }

  val rPort_mask = scala.collection.mutable.MutableList((0 until pR.length).map{i => 0}:_*)
  def connectDeqPort(en: Vec[Bool]): Vec[UInt] = {
    // Figure out which bundle this port can go with
    val bundle_id = rPort_mask.zip(pR).indexWhere{a => a._2 == en.length && a._1 == 0}
    rPort_mask(bundle_id) = 1
    val base = pR.take(bundle_id).sum

    Vec((0 until en.length).map{ i =>
      io.deq(base + i) := Utils.getRetimed(en(i), {if (Utils.retime) 1 else 0})
      io.out(i) // TODO: probably wrong output
    })
  }

  // // Old empty and error tracking
  // val ovW = Module(new SRFF())
  // val ovR = Module(new SRFF())
  // val www_c = writer.io.output.countWithoutWrap(0)*-*(p/pW).U + subWriter.io.output.count(0)
  // val w_c = writer.io.output.count(0)*-*(p/pW).U + subWriter.io.output.count(0)
  // val rww_c = reader.io.output.countWithoutWrap(0)*-*(p/pR).U + subReader.io.output.count(0)
  // val r_c = reader.io.output.count(0)*-*(p/pR).U + subReader.io.output.count(0)
  // val hasData = Module(new SRFF())
  // hasData.io.input.set := (w_c === r_c) & io.enq & !(ovR.io.output.data | ovW.io.output.data)
  // hasData.io.input.reset := (r_c + 1.U === www_c) & io.deq & !(ovR.io.output.data | ovW.io.output.data)
  // io.empty := !hasData.io.output.data

  // // Debugger
  // val overwrite = hasData.io.output.data & (w_c === r_c) & io.enq // TODO: Need to handle sub-counters
  // val fixed_overwrite = (www_c === r_c + 1.U) & io.deq
  // ovW.io.input.set := overwrite
  // ovW.io.input.reset := fixed_overwrite
  // io.debug.overwrite := ovW.io.output.data
  // val overread = !hasData.io.output.data & (w_c === r_c) & io.deq // TODO: Need to handle sub-counters
  // val fixed_overread = (w_c + 1.U === rww_c) & io.enq
  // ovR.io.input.set := overread
  // ovR.io.input.reset := fixed_overread
  // io.debug.overread := ovR.io.output.data

  // io.debug.error := ovR.io.output.data | ovW.io.output.data


}

class FILO(val pR: Int, val pW: Int, val depth: Int, val numWriters: Int, val numReaders: Int, val bitWidth: Int = 32) extends Module {
  def this(tuple: (Int, Int, Int, Int, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)
  def this(p: Int, depth: Int) = this(p, p, 1, 1, depth)

  val io = IO( new Bundle {
    val in = Vec(pW*-*numWriters, Input(UInt(bitWidth.W)))
    val out = Vec(pR*-*numReaders, Output(UInt(bitWidth.W)))
    val push = Vec(numWriters, Input(Bool()))
    val pop = Vec(numReaders, Input(Bool()))
    val empty = Output(Bool())
    val full = Output(Bool())
    val almostEmpty = Output(Bool())
    val almostFull = Output(Bool())
    val numel = Output(UInt(32.W)) // TODO: Should probably be signed fixpt
    val debug = new Bundle {
      val overwrite = Output(Bool())
      val overread = Output(Bool())
      val error = Output(Bool())
    }
  })

  // TODO: Assert pW and pR evenly divide p
  val p = max(pW,pR)

  val push_options = Vec((0 until numWriters).map{ q => io.push(q)}.toList)
  val pop_options = Vec((0 until numReaders).map{ q => io.pop(q)}.toList)

  // Register for tracking number of elements in FILO
  val elements = Module(new IncDincCtr(pW, pR, depth))
  elements.io.input.inc_en := push_options.reduce{_|_}
  elements.io.input.dinc_en := pop_options.reduce{_|_}

  // Create physical mems
  val m = (0 until p).map{ i => Module(new Mem1D(depth/p, bitWidth))}

  // Create head and reader sub counters
  val sa_width = 2 + Utils.log2Up(p)
  val subAccessor = Module(new SingleSCounterCheap(1,0,p,pW,-pR,0,sa_width))
  subAccessor.io.input.enable := push_options.reduce{_|_} | pop_options.reduce{_|_}
  subAccessor.io.input.dir := push_options.reduce{_|_}
  subAccessor.io.input.reset := reset
  subAccessor.io.input.saturate := false.B
  val subAccessor_prev = Mux(subAccessor.io.output.count(0) - pR.S(sa_width.W) < 0.S(sa_width.W), (p-pR).S(sa_width.W), subAccessor.io.output.count(0) - pR.S(sa_width.W))

  // Create head and reader counters
  val a_width = 2 + Utils.log2Up(depth/p)
  val accessor = Module(new SingleSCounterCheap(1, 0, (depth/p), 1, -1, 0, a_width))
  accessor.io.input.enable := (push_options.reduce{_|_} & subAccessor.io.output.done) | (pop_options.reduce{_|_} & subAccessor_prev === 0.S(sa_width.W))
  accessor.io.input.dir := push_options.reduce{_|_}
  accessor.io.input.reset := reset
  accessor.io.input.saturate := false.B


  // Connect pusher
  if (pW == pR) {
    m.zipWithIndex.foreach { case (mem, i) => 
      val data_options = Vec((0 until numWriters).map{ q => io.in(q*-*pW + i)}.toList)
      mem.io.w.addr := accessor.io.output.count(0).asUInt
      mem.io.w.data := Mux1H(push_options, data_options)
      mem.io.w.en := push_options.reduce{_|_}
    }
  } else {
    (0 until pW).foreach { w_i => 
      (0 until (p /-/ pW)).foreach { i => 
        val data_options = Vec((0 until numWriters).map{ q => io.in(q*-*pW + w_i)}.toList)
        m(w_i + i*-*pW).io.w.addr := accessor.io.output.count(0).asUInt
        m(w_i + i*-*pW).io.w.data := Mux1H(push_options, data_options)
        m(w_i + i*-*pW).io.w.en := push_options.reduce{_|_} & (subAccessor.io.output.count(0) === (i*-*pW).S(sa_width.W))
      }
    }
  }

  // Connect popper
  if (pW == pR) {
    m.zipWithIndex.foreach { case (mem, i) => 
      mem.io.r.addr := (accessor.io.output.count(0) - 1.S(a_width.W)).asUInt
      mem.io.r.en := pop_options.reduce{_|_}
      io.out(i) := mem.io.output.data
    }
  } else {
    (0 until pR).foreach { r_i => 
      val rSel = Wire(Vec( (p/pR), Bool()))
      val rData = Wire(Vec( (p/pR), UInt(bitWidth.W)))
      (0 until (p /-/ pR)).foreach { i => 
        m(r_i + i*-*pR).io.r.addr := (accessor.io.output.count(0) - 1.S(sa_width.W)).asUInt
        m(r_i + i*-*pR).io.r.en := pop_options.reduce{_|_} & (subAccessor_prev === (i*-*pR).S(sa_width.W))
        rSel(i) := subAccessor_prev === i.S
        // if (i == 0) { // Strangeness from inc-then-read nuisance
        //   rSel((p/pR)-1) := subReader.io.output.count(0) === i.U
        // } else {
        //   rSel(i-1) := subReader.io.output.count(0) === i.U
        // }
        rData(i) := m(r_i + i*-*pR).io.output.data
      }
      io.out(r_i) := chisel3.util.PriorityMux(rSel, rData)
    }
  }

  // Check if there is data
  io.empty := elements.io.output.empty
  io.full := elements.io.output.full
  io.almostEmpty := elements.io.output.almostEmpty
  io.almostFull := elements.io.output.almostFull
  io.numel := elements.io.output.numel.asUInt()

  // Debug signals
  io.debug.overread := elements.io.output.overread
  io.debug.overwrite := elements.io.output.overwrite
  io.debug.error := elements.io.output.overwrite | elements.io.output.overread

  var wId = 0
  def connectPushPort(data: Vec[UInt], en: Bool): Unit = {
    (0 until data.length).foreach{ i => 
      io.in(wId*-*pW + i) := data(i)
    }
    io.push(wId) := en
    wId += 1
  }

  var rId = 0
  def connectPopPort(en: Bool): Vec[UInt] = {
    io.pop(rId) := en
    rId += 1
    io.out
  }
  // // Old empty and error tracking
  // val ovW = Module(new SRFF())
  // val ovR = Module(new SRFF())
  // val www_c = accessor.io.output.countWithoutWrap(0)*-*(p/pW).U + subAccessor.io.output.count(0)
  // val w_c = accessor.io.output.count(0)*-*(p/pW).U + subAccessor.io.output.count(0)
  // val rww_c = reader.io.output.countWithoutWrap(0)*-*(p/pR).U + subReader.io.output.count(0)
  // val r_c = reader.io.output.count(0)*-*(p/pR).U + subReader.io.output.count(0)
  // val hasData = Module(new SRFF())
  // hasData.io.input.set := (w_c === r_c) & io.push & !(ovR.io.output.data | ovW.io.output.data)
  // hasData.io.input.reset := (r_c + 1.U === www_c) & io.pop & !(ovR.io.output.data | ovW.io.output.data)
  // io.empty := !hasData.io.output.data

  // // Debugger
  // val overwrite = hasData.io.output.data & (w_c === r_c) & io.push // TODO: Need to handle sub-counters
  // val fixed_overwrite = (www_c === r_c + 1.U) & io.pop
  // ovW.io.input.set := overwrite
  // ovW.io.input.reset := fixed_overwrite
  // io.debug.overwrite := ovW.io.output.data
  // val overread = !hasData.io.output.data & (w_c === r_c) & io.pop // TODO: Need to handle sub-counters
  // val fixed_overread = (w_c + 1.U === rww_c) & io.push
  // ovR.io.input.set := overread
  // ovR.io.input.reset := fixed_overread
  // io.debug.overread := ovR.io.output.data

  // io.debug.error := ovR.io.output.data | ovW.io.output.data

}
