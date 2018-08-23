package spatial.codegen.pirgen

import argon._

case class PIRGenSpatial(IR: State) extends PIRCodegen
  with PIRGenArray
  with PIRGenBit
  with PIRGenFixPt
  with PIRGenFltPt
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
  with PIRGenReg
  with PIRGenSeries
  with PIRGenSRAM
  with PIRGenVec
  with PIRGenStream
  with PIRGenRegFile
  with PIRGenFileIO
  with PIRGenDelays
  with PIRGenLUTs {

  override def copyDependencies(out: String): Unit = {
    dependencies ::= FileDep("pirgen", "Makefile", "../")
    dependencies ::= FileDep("pirgen", "run.sh", "../")
    dependencies ::= FileDep("pirgen", "build.sbt", "../")
    dependencies ::= FileDep("pirgen/project", "build.properties", "../project/")
    super.copyDependencies(out)
  }
}
