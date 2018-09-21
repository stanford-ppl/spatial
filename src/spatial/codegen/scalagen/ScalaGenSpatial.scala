package spatial.codegen.scalagen

import argon._

case class ScalaGenSpatial(IR: State) extends ScalaCodegen
  with ScalaGenArray
  with ScalaGenBit
  with ScalaGenFixPt
  with ScalaGenFltPt
  with ScalaGenStructs
  with ScalaGenText
  with ScalaGenVoid
  with ScalaGenVar
  with ScalaGenDebugging
  with ScalaGenLIFO
  with ScalaGenController
  with ScalaGenCounter
  with ScalaGenDRAM
  with ScalaGenFIFO
  with ScalaGenReg
  with ScalaGenSeries
  with ScalaGenSRAM
  with ScalaGenVec
  with ScalaGenStream
  with ScalaGenRegFile
  with ScalaGenFileIO
  with ScalaGenDelays
  with ScalaGenLUTs {

  override def copyDependencies(out: String): Unit = {
    dependencies ::= FileDep("scalagen", "Makefile.sim", "../", Some("Makefile"))
    dependencies ::= FileDep("scalagen", "run.sh", "../")
    dependencies ::= FileDep("scalagen", "build.sbt", "../")
    dependencies ::= FileDep("scalagen/project", "build.properties", "../project/")
    super.copyDependencies(out)
  }
}
