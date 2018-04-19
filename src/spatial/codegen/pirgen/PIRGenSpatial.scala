package spatial.codegen.pirgen

import argon._
import spatial.codegen.naming.NamedCodegen

case class PIRGenSpatial(IR: State) extends PIRCodegen
  with PIRGenArray
  with PIRGenBit
  with PIRGenFixPt
  with PIRGenFltPt
  with PIRGenIfThenElse
  with PIRGenStructs
  with PIRGenText
  with PIRGenVoid
  with PIRGenVar
  with PIRGenDebugging
  with PIRGenLIFO
  with PIRGenController
  with PIRGenCounter
  with PIRGenDRAM
  with PIRGenFIFO
  with PIRGenArgs
  with PIRGenReg
  with PIRGenSRAM
  with PIRGenUnrolled
  with PIRGenVec
  with PIRGenStream
  with PIRGenRegFile
  with PIRGenStateMachine
  with PIRGenFileIO
  with PIRGenDelays
  with PIRGenLUTs
  with PIRGenSwitch
  with PIRGenOp
  with NamedCodegen {

  //override def copyDependencies(out: String): Unit = {
    //dependencies ::= FileDep("pirgen", "Makefile", "../")
    //super.copyDependencies(out)
  //}
}
