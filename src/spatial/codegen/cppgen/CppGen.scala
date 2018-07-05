package spatial.codegen.cppgen

import argon._

case class CppGen(IR: State) extends CppCodegen
	with CppFileGen
	with CppGenCommon
	with CppGenInterface
	with CppGenAccel
	with CppGenDebug
	with CppGenMath
	with CppGenArray
	with CppGenFileIO
