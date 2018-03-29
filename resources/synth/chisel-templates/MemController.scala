// package templates

// import chisel3._


// // DEPRECATED

// class FromAccel(val p: Int) extends Bundle {
//   // Command signals
//   val base   = UInt(32.W)
//   val offset   = UInt(32.W)
//   val size = UInt(32.W)
//   val enLoad = Bool()
//   val enStore = Bool()

//   // Data signals  
//   val data = Vec(p, UInt(32.W))
//   val deq = Bool() // For accel to consume ctrl load fifo
//   val enq = Bool() // For accel to enq onto store fifo

//   override def cloneType = (new FromAccel(p)).asInstanceOf[this.type] // See chisel3 bug 358
// }
// class ToAccel(val p: Int) extends Bundle {
//   val data   = Vec(p, UInt(32.W))
//   val deq   = Bool()
//   val valid = Bool()
//   val cmdIssued = Bool() // Indicates when command is issued and data has started filling LoadFIFO
//   val doneStore = Bool()
//   val done = Bool()

//   override def cloneType = (new ToAccel(p)).asInstanceOf[this.type] // See chisel3 bug 358
// }
// class FromDRAM(val p: Int) extends Bundle {
//   val data   = Vec(p, UInt(32.W))
//   val tag = UInt(32.W)
//   val deq = Bool()
//   val valid = Bool()

//   override def cloneType = (new FromDRAM(p)).asInstanceOf[this.type] // See chisel3 bug 358
// }
// class ToDRAM(val p: Int) extends Bundle {
//   val addr   = UInt(32.W)
//   val size  = UInt(32.W)
//   val data = Vec(p, UInt(32.W))
//   val base = UInt(32.W)
//   val tag = UInt(32.W)
//   val receiveBurst = Bool()
//   val sendBurst = Bool()

//   override def cloneType = (new ToDRAM(p)).asInstanceOf[this.type] // See chisel3 bug 358
// }


// class MemController(val pLoadAccel: Int, val pStoreAccel: Int, val pStoreDRAM: Int, val pLoadDRAM: Int) extends Module {
//   def this(pAccel: Int, pDRAM: Int) = this(pAccel, pAccel, pDRAM, pDRAM)
//   def this(p: Int) = this(p,p,p,p)
//   // DRAM pars should be equal to burst size

//   val io = IO(new Bundle{
//     val AccelToCtrl = Input(new FromAccel(pStoreAccel))
//     val CtrlToAccel = Output(new ToAccel(pLoadAccel))
//     val DRAMToCtrl = Input(new FromDRAM(pLoadDRAM))
//     val CtrlToDRAM = Output(new ToDRAM(pStoreDRAM))
//   })

//   // TODO: Implement full memory controller that interfaces with DRAM or DRAMSim

//   // Temporarily pass through signals from hw to test harness
//   io.CtrlToDRAM.base := io.AccelToCtrl.base // Only used for the janky mem controller
//   io.CtrlToDRAM.addr := io.AccelToCtrl.offset + io.AccelToCtrl.base
//   io.CtrlToDRAM.data.zip(io.AccelToCtrl.data).foreach{ case (data, port) => data := port }
//   io.CtrlToDRAM.size := io.AccelToCtrl.size

//   io.CtrlToAccel.data.zip(io.DRAMToCtrl.data).foreach{ case (data, port) => data := port }

//   val burstSize = 512 // TODO: Should probably be an input argument to the constructor

//   // Create FIFO to hold data from DRAM
//   val loadFifo = Module(new FIFO(pLoadAccel, pLoadDRAM, burstSize, 1, 1))
//   loadFifo.io.in := io.DRAMToCtrl.data
//   loadFifo.io.enq := io.DRAMToCtrl.valid
//   loadFifo.io.deq := io.AccelToCtrl.deq
//   io.CtrlToAccel.data := loadFifo.io.out
//   io.CtrlToAccel.valid := !loadFifo.io.empty | (io.AccelToCtrl.enLoad & io.CtrlToDRAM.size === 0.U)
//   io.CtrlToAccel.cmdIssued := !loadFifo.io.empty & Utils.delay(loadFifo.io.empty, 1) // TODO: May cause bug if fifo drains faster than it loads
//   io.CtrlToDRAM.receiveBurst := io.AccelToCtrl.enLoad

//   // Create FIFO to hold data from Accel
//   val storeFifo = Module(new FIFO(pStoreDRAM, pStoreAccel, burstSize, 1, 1))
//   storeFifo.io.in := io.AccelToCtrl.data
//   storeFifo.io.enq := io.AccelToCtrl.enq
//   storeFifo.io.deq := io.DRAMToCtrl.deq
//   io.CtrlToDRAM.data := storeFifo.io.out
//   io.CtrlToDRAM.sendBurst := !storeFifo.io.empty & io.AccelToCtrl.enStore //| (io.AccelToCtrl.en & io.CtrlToDRAM.size === 0.U)
//   io.CtrlToAccel.doneStore := storeFifo.io.empty & io.AccelToCtrl.enStore
// }

