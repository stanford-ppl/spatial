package spatial.codegen.cppgen

import argon.codegen.cppgen._

trait CppGenSpatial extends CppCodegen with CppFileGen
  with CppGenBool with CppGenUnit with CppGenFixPt with CppGenFltPt
  with CppGenCounter with CppGenReg with CppGenSRAM with CppGenFIFO
  with CppGenIfThenElse with CppGenController with CppGenMath with CppGenFringeCopy with CppGenString
  with CppGenDRAM with CppGenHostTransfer with CppGenUnrolled with CppGenVector
  with CppGenArray with CppGenArrayExt with CppGenRange with CppGenAlteraVideo with CppGenStream
  with CppGenHashMap with CppGenStruct with CppGenDebugging with CppGenFileIO with CppGenFunction
  with CppGenVar with CppGenPlaceholders with CppGenAsserts
