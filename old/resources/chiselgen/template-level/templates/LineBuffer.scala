package templates

import chisel3._
import ops._
import chisel3.util.log2Ceil
import chisel3.util.MuxLookup


  /*

  Ex. stride = 2, empty_stages_to_buffer = 2, num_lines = 3

     [          ] <- msb row loading
     [          ] <- lsb row loading
     [          ] <- <invisible line>
     [          ] <- <invisible line>
     [          ] <- msb visible line
     [          ] <- visible line
     [          ] <- lsb visible line


  */


// ENHANCEMENT: currently this assumes read col par = 1, read row par = kernel height, and write row/col par is 1 and 1
// See comments below: first should implement read col par, and also read row par == 1
// col_rPar == stride
class LineBuffer(val num_lines: Int, val line_size: Int, val empty_stages_to_buffer: Int, val rstride: Int, 
  val col_wPar: Int, val col_rPar:Int, val col_banks: Int, 
  val numWriters: Int, val row_rPar:Int, val transientPar: Int, val numAccessors: Int, val bitWidth: Int = 32) extends Module {

  def this(tuple: (Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8, tuple._9, tuple._10, tuple._11)

  val extra_rows_to_buffer = empty_stages_to_buffer * rstride

  val io = IO(new Bundle {
    val data_in  = Vec(col_wPar*rstride*numWriters+transientPar, Input(UInt(bitWidth.W)))
    val w_en     = Vec(rstride*numWriters+{if (transientPar != 0) 1 else 0}, Input(Bool()))
    // val r_en     = Input(UInt(1.W))
    // val w_done   = Input(UInt(1.W))

    // Buffering signals
    val sEn = Vec(numAccessors, Input(Bool())) // Too many but at least this is safe
    val sDone = Vec(numAccessors, Input(Bool())) // Too many but at least this is safe
    val transientDone = Input(Bool())
    val transientSwap = Input(Bool())

    // val r_done   = Input(UInt(1.W)) // Like double buffering

    val reset    = Input(UInt(1.W))
    val col_addr = Vec(col_rPar, Input(UInt(log2Ceil(line_size+1).W))) // From each row, read COL_PAR px starting at this col_addr
    // val row_addr = Input(UInt(1.W)) // ENHANCEMENT: Eventually will be a Vec, but for now ROW_PAR is 1 or num_lines only
    // val data_out = Vec(ROW_PAR, Vec(COL_PAR, Output(UInt(bitWidth.W)))) // TODO: Don't use Vec(Vec) since Chisel will switch inputs and outputs
    val data_out = Vec(row_rPar*col_rPar, Output(UInt(bitWidth.W)))
    val swap = Output(Bool()) // for debugging
    // val row_wrap = Output(UInt(1.W))
  })
  
  // ENHANCEMENT: should r_en RAMs (currently not supported, but saves power)
  // ENHANCEMENT: enq multiple @ once since banked -- COL_WRITE_PAR, ROW_WRITE_PAR (and change names of 2 PARs above to COL/ROW_READ_PAR)
  // ENHANCEMENT: could keep internal state of whether we are initializing (false means steady state),
  //                val initializing_state = Reg(init=reset_val.asUInt(log2Ceil(stop+1).W))
  //                ...
  //              and also other info, e.g. set an output high when line fills
  //                row_wrap := WRITE_countRowNum.io.wrap
  //              etc. 
  
  // Buffering logic
  val sEn_latch = (0 until numAccessors).map{i => Module(new SRFF())}
  val sDone_latch = (0 until numAccessors).map{i => Module(new SRFF())}
  val swap = Wire(Bool())
  // Latch whether each buffer's stage is enabled and when they are done
  (0 until numAccessors).foreach{ i => 
    sEn_latch(i).io.input.set := io.sEn(i) & ~io.sDone(i)
    sEn_latch(i).io.input.reset := swap
    sEn_latch(i).io.input.asyn_reset := Utils.getRetimed(reset, 1)
    sDone_latch(i).io.input.set := io.sDone(i)
    sDone_latch(i).io.input.reset := swap
    sDone_latch(i).io.input.asyn_reset := Utils.getRetimed(reset, 1)
  }
  val anyEnabled = sEn_latch.map{ en => en.io.output.data }.reduce{_|_}
  swap := sEn_latch.zip(sDone_latch).map{ case (en, done) => en.io.output.data === done.io.output.data }.reduce{_&_} & anyEnabled //| io.transientSwap
  io.swap := swap

  // assert(ROW_PAR == 1 || ROW_PAR == num_lines)

  // --------------------------------------------------------------------------------------------------------------------------------
  // Declare buffer data structure in SRAM
  // --------------------------------------------------------------------------------------------------------------------------------
  // ENHANCEMENT: support read parallelism
  //   - col_par = # banks
  //   - row_par = # copies of above, i.e. # scratchpads
  //  Banking is needed otherwise parallel col reads are not possible (row is currently possible since there
  //  is 1 SRAM per row, i.e. 1 bank and row_par copies of it, i.e. row_par scratchpads)
  //   - E.g. this would enable parallelism of 3x3 to be 18 instead of 9, and get processing done 2x faster
  //     - would need (for stride 1) to process 1 2 3 4, then 3 4 5 6, etc., so need to read 2 px per row into shift register
  //  Later, also add support for both R and W parallelization
  //   - W par less useful since conv is often compute bound
  // val linebuffer = List.fill(num_lines + extra_rows_to_buffer)(Mem(line_size, UInt(bitWidth.W)))
  val linebuffer = List.fill(num_lines + extra_rows_to_buffer)(Module(new SRAM(List(line_size), bitWidth, 
    List(col_banks), List(1), List(col_wPar), List(col_rPar), BankedMemory)))
  
  // --------------------------------------------------------------------------------------------------------------------------------
  // Write logic
  // --------------------------------------------------------------------------------------------------------------------------------
  
  // Inner counter over row width -- keep track of write address in current row
  val WRITE_countRowPx = (0 until rstride).map{ i =>
    val cnt = Module(new SingleCounter(col_wPar, Some(0), Some(line_size), Some(1), Some(0)))
    cnt.io.input.enable := (0 until numWriters).map{j => io.w_en(j*rstride + i)}.reduce{_||_}
    cnt.io.input.reset := io.reset | swap
    cnt.io.input.saturate := false.B
    cnt.io.input.start := 0.S
    cnt.io.input.stop := line_size.S
    cnt.io.input.stride := 1.S
    cnt.io.input.gap := 0.S
    cnt
  }  
  // Inner counter over row width -- keep track of write address in current row for transient writer
  val WRITE_countRowPx_transient = Module(new SingleCounter(transientPar, Some(0), Some(line_size), Some(1), Some(0)))
  WRITE_countRowPx_transient.io.input.enable := io.w_en.last
  WRITE_countRowPx_transient.io.input.reset := io.reset | swap
  WRITE_countRowPx_transient.io.input.saturate := false.B
  WRITE_countRowPx_transient.io.input.start := 0.S
  WRITE_countRowPx_transient.io.input.stop := line_size.S
  WRITE_countRowPx_transient.io.input.stride := 1.S
  WRITE_countRowPx_transient.io.input.gap := 0.S
  val px_transient = WRITE_countRowPx_transient.io.output.count.asUInt

  // Outer counter over number of SRAM -- keep track of current row
  val wCRN_width = 1 + Utils.log2Up(num_lines+extra_rows_to_buffer)
  val WRITE_countRowNum = (0 until rstride).map{i =>
    val cnt = Module(new NBufCtr(rstride, Some(i), Some(num_lines+extra_rows_to_buffer), wCRN_width))
    cnt.io.input.enable := swap
    cnt.io.input.countUp := true.B
    cnt
  }
  // Outer counter over number of SRAM -- keep track of current row for transient writer
  val WRITE_countRowNum_transient = Module(new NBufCtr(1,Some(0), Some(num_lines+extra_rows_to_buffer), wCRN_width))
  WRITE_countRowNum_transient.io.input.enable := io.transientDone
  WRITE_countRowNum_transient.io.input.countUp := true.B
  val transient_row = WRITE_countRowNum_transient.io.output.count
  
  // Write data_in into line buffer
  for (i <- 0 until (num_lines + extra_rows_to_buffer)) {
    val wen_muxing = (Array.tabulate(rstride)){ ii =>
      val wen = (0 until numWriters).map{jj => io.w_en(jj*rstride + ii)}.reduce{_||_}
      ((WRITE_countRowNum(ii).io.output.count + transient_row).%-%((num_lines+extra_rows_to_buffer).U, Some(0.0)) -> wen)
    }
    for (j <- 0 until col_wPar) {
      // Figure out which input to draw 
      val wdata_muxing = (Array.tabulate(rstride)){ ii =>
        val data_options = (0 until numWriters).map{jj => io.data_in(ii * col_wPar + j + (jj*col_wPar*rstride) )}
        val selector = (0 until numWriters).map{jj => io.w_en(jj*rstride + ii)}
        val data = chisel3.util.Mux1H(selector, data_options)
        ((WRITE_countRowNum(ii).io.output.count + transient_row).%-%((num_lines+extra_rows_to_buffer).U, Some(0.0)) -> data)
      }
      val waddr_muxing = (Array.tabulate(rstride)){ ii =>
        ((WRITE_countRowNum(ii).io.output.count + transient_row).%-%((num_lines+extra_rows_to_buffer).U, Some(0.0)) -> WRITE_countRowPx(ii).io.output.count(j).asUInt)
      }
      if (transientPar != 0) {
        linebuffer(i).io.w(j).addr(0) := Mux(io.w_en.last, px_transient, MuxLookup(i.U(wCRN_width.W), 0.U, waddr_muxing))
        linebuffer(i).io.w(j).data := Mux(io.w_en.last, io.data_in(col_wPar*rstride*numWriters + j), MuxLookup(i.U(wCRN_width.W), 0.U, wdata_muxing))
        linebuffer(i).io.w(j).en := Mux(io.w_en.last, true.B & (i.U(wCRN_width.W) === transient_row), true.B & MuxLookup(i.U(wCRN_width.W), false.B, wen_muxing))
      } else {
        linebuffer(i).io.w(j).addr(0) := MuxLookup(i.U(wCRN_width.W), 0.U, waddr_muxing)
        linebuffer(i).io.w(j).data := MuxLookup(i.U(wCRN_width.W), 0.U, wdata_muxing)
        linebuffer(i).io.w(j).en := true.B & MuxLookup(i.U(wCRN_width.W), false.B, wen_muxing)
      }
    }
  }
    
  // --------------------------------------------------------------------------------------------------------------------------------
  // Read logic
  // --------------------------------------------------------------------------------------------------------------------------------
  
  // ENHANCEMENT: Support row_addr to only read from one row if ROW_PAR == 1
  // - Rather than crossbar from each line to each output, it becomes just a mux (1 output),
  //   but now the sel of that mux also depends on row_addr
  
  // ENHANCEMENT: Support col_addr, to read from more than 1 col (needs banking)
  
  // Read data_out from line buffer
  // This requires a crossbar, i.e. mux from each line (num_lines + extra_rows_to_buffer) to each output (num_lines)
  // ENHANCEMENT: May save some area using a single counter with many outputs and adders/mux for each (to do mod/wrap) but 
  // multiple counters (which start/reset @ various #s) is simpler to write
  val READ_countRowNum = (0 until row_rPar).map{ i => 
    val c = Module(new NBufCtr(rstride, Some(rstride+i), Some(num_lines+extra_rows_to_buffer), 1 + Utils.log2Up(num_lines+extra_rows_to_buffer)))
    // c.io.input.start := (num_lines+extra_rows_to_buffer-1-i).U
    c.io.input.enable := swap
    c.io.input.countUp := true.B
    c
  }

  for (j <- 0 until col_rPar) {
    var linebuf_read_wires_map = Array.tabulate(num_lines + extra_rows_to_buffer) { i =>
      // when(io.r_en) {  // ENHANCEMENT: r_en to save power, i.e. make the below wire RHS of -> into a reg
      linebuffer(i).io.r(j).addr(0) := io.col_addr(j)
      linebuffer(i).io.r(j).en := true.B
      linebuffer(i).io.flow(0) := true.B // This may possibly cause issues
      (i.U -> linebuffer(i).io.output.data(j))
      // }
    }
    for (i <- 0 until (row_rPar)) { // ENHANCEMENT: num_lines -> row par
      io.data_out(i*col_rPar + j) := MuxLookup((READ_countRowNum(i).io.output.count + transient_row).%-%((num_lines+extra_rows_to_buffer).U, Some(0.0)), 0.U, linebuf_read_wires_map)
    }    
  }
  
  def readRow(row: UInt): UInt = { 
    val readableData = (0 until row_rPar).map { i =>
      (i.U -> io.data_out(i))
    }
    MuxLookup(row, 0.U,  readableData)
  }
  def readRowSlice(row: UInt, relative_col: UInt): UInt = { 
    val readableData = (0 until row_rPar * col_rPar).map { i =>
      (i.U -> io.data_out(i))
    }
    MuxLookup(row.*-*(col_rPar.U, None) + relative_col, 0.U,  readableData)
  }
  def readRow(row: Int): UInt = { 
    io.data_out(row)
  }


  val availablePorts = (0 until num_lines).map{p => p}
  var usedPorts = List[Int]()
  def connectStageCtrl(done: Bool, en: Bool, ports: List[Int]) {
    ports.foreach{ port => 
      io.sEn(port) := en
      io.sDone(port) := done
      usedPorts = usedPorts :+ port
    }
  }

  var dPort = 0
  var ePort = 0
  def connectWPort(data: List[UInt], en: List[Bool]) {
    data.foreach{ d => 
      io.data_in(dPort) := d
      dPort = dPort + 1
    }
    en.foreach{ e =>
      io.w_en(ePort) := e
      ePort = ePort + 1
    }
    

  }

  def lockUnusedCtrl() {
    availablePorts.foreach { p =>
      if (!usedPorts.contains(p)) {
        io.sEn(p) := false.B
        io.sDone(p) := false.B
      }
    }
  }

}
