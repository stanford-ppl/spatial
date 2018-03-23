// Not sure what this template does

// import Chisel._
// 
// class BlockSM extends Module {
// 
//   // States
//   val pipeInit :: pipeRun :: pipeDelay :: pipeDone :: pipeW :: Nil = Enum(UInt// (), 5)
// 
//   // Module IO
//   val io = new Bundle {
//       
//       // State machine IO
//       val sm_done = Bool(OUTPUT)
//       val sm_en = Bool(INPUT)
// 
//       // Inputs
//       val forceLdSt = Bool(INPUT)
//       val i = UInt(INPUT)
//       val jburst = UInt(INPUT)
//       val memDone = Bool(INPUT)
// 
//       // Outputs
//       val memStart = Bool(OUTPUT)
//       val iOut = Bool(OUTPUT)
//       val jburstOut = Bool(OUTPUT)
//       val isLdStOut = Bool(OUTPUT)
//   }
// 
//   // State storage
//   val stateFF = Reg(init = pipeInit)
//   val iFF = Reg(init = UInt(0, 32))
//   val jburstFF = Reg(init = UInt(0, 32))
//   val forceLdStFF = Reg(init = Bool(false))
//   val isLdStFF = Reg(init = Bool(false))
//   val delayFF = Reg(init = UInt(0, 32))
// 
//   // Other variables
//   val boolean_dbg = false
//   val delayInitVal = 64  // TODO: Should be a sm input, and should wait only // on stores
// 
//   // Defaults
//   io.sm_done := Bool(false);
//   io.memStart := Bool(false); // isLdStFF;
//   io.iOut := iFF;
//   io.jburstOut := jburstFF;
//   io.isLdStOut := isLdStFF;
// 
//   // State Machine
//   when (io.sm_en) {
//     switch (stateFF) {
//       
//       is (pipeInit) {
//         
//         // Latch all input signals
//         iFF := io.i
//         jburstFF := io.jburst
//         forceLdStFF := io.forceLdSt
//         delayFF := UInt(delayInitVal)
// 
//         when ((io.i != iFF) || (io.jburst != jburstFF) || (io.forceLdSt)) {
//           isLdStFF := Bool(true)
//           stateFF := pipeRun
//         } 
//         .otherwise {
//           isLdStFF := Bool(false);
//           stateFF := pipeDone
//         }
//       }
//       
//       is (pipeRun) {
// 
//         io.memStart := Bool(false)
//         
//         when (io.memDone) {
//           stateFF := pipeDelay
//         }
//       }
// 
//       is (pipeDelay) {
//         delayFF := delayFF-UInt(1);
//         when (delayFF === UInt(0)) {
//           stateFF := pipeDone;
//         }
//       }
// 
//       is (pipeDone) {
//         io.memStart := Bool(false)
//         io.sm_done := Bool(true)
//         stateFF := pipeW;
//       }
//       
//       is (pipeW) {
//         stateFF := pipeW;
//       }
//     }
//   }
//   .otherwise {
//     isLdStFF := Bool(false);
//     stateFF := pipeInit;
//   }
// 
// }
// 