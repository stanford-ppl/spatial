package spatial.codegen.tsthgen

import argon._
import spatial.codegen.cppgen._

case class TungstenHostGenSpatial(IR: State) extends TungstenHostCodegen
	with CppGenCommon
	with CppGenDebug
	with CppGenMath
	with CppGenArray
	with CppGenFileIO
	with TungstenHostGenInterface
	with TungstenHostGenAccel
