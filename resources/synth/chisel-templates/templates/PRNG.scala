// See LICENSE.txt for license details.
package templates

import chisel3._
import chisel3.util.{log2Ceil, isPow2}
import chisel3.internal.sourceinfo._
import types._


class PRNG(val seed: Int, val bitWidth:Int = 32) extends Module { 

  val io = IO( new Bundle {
    val en = Input(Bool())
    val output = Output(UInt(bitWidth.W))
  })


  // // Single Cycle Version
  // val reg = RegInit(seed.U(bitWidth.W))
  // val shifted = ((reg ^ (reg << 1)) ^ ((reg ^ (reg << 1)) >> 3)) ^ (((reg ^ (reg << 1)) ^ ((reg ^ (reg << 1)) >> 3)) << 10)
  // reg := Mux(io.en, shifted, reg)

  // 3 Cycle Version
  val reg = RegInit(seed.U(bitWidth.W))
  val cnt = RegInit(0.U(2.W))
  cnt := Mux(io.en, Mux(cnt === 2.U, 0.U, cnt + 1.U), cnt)
  val shift_options = List((0.U -> {reg << 1}), (1.U -> {reg >> 3}), (2.U -> {reg << 10}))
  reg := Mux(io.en, reg ^ chisel3.util.MuxLookup(cnt, 0.U, shift_options), reg)

  io.output := reg

}

// MAXJ VERSION:

// package engine;
//   import com.maxeler.maxcompiler.v2.kernelcompiler.KernelLib;
//   import com.maxeler.maxcompiler.v2.statemachine.DFEsmInput;
//   import com.maxeler.maxcompiler.v2.statemachine.DFEsmOutput;
//   import com.maxeler.maxcompiler.v2.statemachine.DFEsmStateEnum;
//   import com.maxeler.maxcompiler.v2.statemachine.DFEsmStateValue;
//   import com.maxeler.maxcompiler.v2.statemachine.DFEsmValue;
//   import com.maxeler.maxcompiler.v2.statemachine.kernel.KernelStateMachine;
//   import com.maxeler.maxcompiler.v2.statemachine.types.DFEsmValueType;

// class PRNGFastSM extends KernelStateMachine {

//     // States
//     enum States {
//       INIT,
//       HOLD,
//       SHIFT
//     }

//     // State IO
//     private final DFEsmOutput oNumber;
//     private final DFEsmInput iNext;
//     private final int mSeed;

//     // State storage
//     private final DFEsmStateValue sNumber;
//     private final DFEsmStateEnum<States> sMode;

//     // Initialize state machine in constructor
//     public PRNGFastSM(KernelLib owner, int seed) {
//       super(owner);
//       mSeed = seed;

//       // Declare all types required to wire the state machine together
//       DFEsmValueType numberType = dfeInt(32);
//       DFEsmValueType uType = dfeUInt(32);
//       DFEsmValueType wireType = dfeBool();

//       // Define state machine IO
//       oNumber = io.output("oNumber", uType);
//       iNext = io.input("iNext", wireType);

//       // Define state storage elements and initial state
//       sMode = state.enumerated(States.class, States.INIT);
//       sNumber = state.value(numberType, mSeed);
//     }

//     private DFEsmValue Gen(DFEsmStateValue number) {
//       // // Separated XORSHIFT
//       // DFEsmValue s1 = number ^ (number << 1);
//       // DFEsmValue s2 = s1 ^ (s1 >> 3);
//       // DFEsmValue s3 = s2 ^ (s2 << 10);
//       // return s3;

//       // In-lined XORSHIFT via substitution
//       return ((number ^ (number << 1)) ^ ((number ^ (number << 1)) >> 3)) ^ (((number ^ (number << 1)) ^ ((number ^ (number << 1)) >> 3)) << 10);
//     }

//     @Override
//     protected void nextState() {
//       SWITCH(sMode) {
//         CASE(States.INIT) {
//           IF(iNext === true) {
//             sNumber.next <== sNumber;
//             sMode.next <== States.SHIFT;
//           } ELSE {
//             sNumber.next <== sNumber;
//             sMode.next <== States.HOLD;
//           }
//         }
//         CASE(States.SHIFT) {
//           IF(iNext === true) {
//             sNumber.next <== Gen(sNumber); // DO LOGIC
//             sMode.next <== States.SHIFT;
//           } ELSE {
//             sNumber.next <== sNumber;
//             sMode.next <== States.HOLD;
//           }
//         }
//         CASE(States.HOLD) {
//           IF(iNext === true) {
//             sNumber.next <== Gen(sNumber); // DO LOGIC
//             sMode.next <== States.SHIFT;
//           } ELSE {
//             sNumber.next <== sNumber;
//             sMode.next <== States.HOLD;
//           }
//         }
//       }
//     }

//   @Override
//     protected void outputFunction() {
//       DFEsmValueType numberType = dfeUInt(32);
//       oNumber <== sNumber.cast(numberType);
//     }
// }