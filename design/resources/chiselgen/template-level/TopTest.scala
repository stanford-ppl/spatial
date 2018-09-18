// // See LICENSE for license details.

// package example

// import chisel3.core.Module
// import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

// import scala.collection.mutable.ListBuffer

// /**
//  * TopModule test harness
//  */
// class TopModuleUnitTester(c: TopModule)(implicit args: Array[String]) extends ArgsTester(c) {

//   // ---- Fringe software API ----
//   def writeReg(reg: Int, data: Int) {
//     poke(c.io.waddr, reg)
//     poke(c.io.wdata, data)
//     poke(c.io.wen, 1)
//     step(1)
//     poke(c.io.wen, 0)
//   }

//   def readReg(reg: Int): Int = {
//     poke(c.io.raddr, reg)
//     peek(c.io.rdata).toInt
//   }

//   def run() = {
//     var numCycles = 0
//     var status = 0
//     writeReg(c.fringe.commandReg, 1)
//     while ((status == 0) && (numCycles <= 100)) {
//       step(1)
//       status = readReg(c.fringe.statusReg)
//       numCycles += 1
//     }
//     numCycles
//   }

//   // ---- Host code ----
//   // Write to all argIns: Regs 2..numArgIns+2
//   for (i <- 2 until c.numArgIns+2) {
//     if (i == 2) writeReg(i, 0x400)
//     else writeReg(i, (i-1)*2)  // Write pattern 4,6..
//   }
//   run()
//   // Read all argOuts: numargIns+2..numArgIns+2+numArgOuts
//   val argOuts = List.tabulate(c.numArgOuts) { i =>
//     readReg(c.numArgIns+2+i)
//   }

//   println(s"argOuts: $argOuts")
// }

// object TopModuleTest extends CommonMain {
//   type DUTType = TopModule

//   def supportedTarget(t: String) = t match {
//     case "aws" => true
//     case "verilator" => true
//     case _ => false
//   }

//   def dut = () => {
//     val w = 32
//     val numArgIns = 1
//     val numArgOuts = 1
//     val numMemoryStreams = 2
//     val target = if (args.size > 0) args(0) else "verilator"
//     Predef.assert(supportedTarget(target), s"ERROR: Unsupported Fringe target '$target'")
//     new TopModule(w, numArgIns, numArgOuts, numMemoryStreams, target)
//   }
//   def tester = { c: DUTType => new TopModuleUnitTester(c) }
// }

