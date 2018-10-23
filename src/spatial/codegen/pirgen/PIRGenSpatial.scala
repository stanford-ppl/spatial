package spatial.codegen.pirgen

import argon._

case class PIRGenSpatial(IR: State) extends PIRCodegen
  with PIRGenController
  with PIRGenArray
  //with PIRGenBit
  with PIRGenFixPt
  //with PIRGenFltPt
  with PIRGenStructs
  //with PIRGenText
  //with PIRGenVoid
  //with PIRGenVar
  //with PIRGenDebugging
  //with PIRGenLIFO
  with PIRGenCounter
  with PIRGenDRAM
  with PIRGenFIFO
  with PIRGenReg
  //with PIRGenSeries
  with PIRGenSRAM
  with PIRGenVec
  with PIRGenStream
  with PIRGenRegFile
  //with PIRGenFileIO
  //with PIRGenDelays
  with PIRGenLUTs 
  with PIRCtxGen
  with PIRSplitGen
  {

  override def copyDependencies(out: String): Unit = {
    dependencies ::= FileDep("pirgen", "pir.Makefile", "../", Some("Makefile"))
    dependencies ::= FileDep("pirgen", "run.sh", "../")
    dependencies ::= FileDep("pirgen", "build.sbt", "../")
    dependencies ::= FileDep("pirgen", "build.properties", "../project/")
    super.copyDependencies(out)
  }
}
