//package fringe

//import chisel3._

//class IdealMemory(
//  val w: Int,
//  val burstSizeBytes: Int
//) extends Module {
//
//  val wordSizeBytes = w/8
//  val pageSizeBytes = 16
//  val pageSizeWords = pageSizeBytes / (wordSizeBytes)
//
//  val io = IO(Flipped(new DRAMStream(w, burstSizeBytes/wordSizeBytes)))
//
//  // Flop vld and tag inputs to delay the response by one cycle
//  val tagInFF = Module(new FF(w))
//  tagInFF.io.enable := true.B
//  tagInFF.io.in := io.cmd.bits.tag
//  io.resp.bits.tag := tagInFF.io.out
//
//  val vldInFF = Module(new FF(1))
//  vldInFF.io.enable := true.B
//  vldInFF.io.in := io.cmd.valid & ~io.cmd.bits.isWr
//  io.resp.valid := vldInFF.io.out
//
//  // Main memory: Big SRAM module
//  // TODO: Chisel's simulation is barfing when the SRAM depth is big
//  // Current size is a measly 1 Mega Word / Bank. This limits the total
//  // memory size to 64 MB (assuming w=32 bits).
//  // Investigate alternatives if necessary.
//  val mems = List.tabulate(burstSizeBytes/wordSizeBytes) { i =>
//    val m = Module(new SRAM(w, (1 << 20) * 1))
//    m.io.raddr := io.cmd.bits.addr
//    m.io.waddr := io.cmd.bits.addr
//    m.io.wdata := io.cmd.bits.wdata(i)
//    m.io.wen := io.cmd.bits.isWr & io.cmd.valid
//    m
//  }
//  io.resp.bits.rdata := mems.map { _.io.rdata }
//}
