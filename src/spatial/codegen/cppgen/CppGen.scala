package spatial.codegen.cppgen

import argon._
import argon.codegen.{Codegen, FileDependencies}

case class CppGen(IR: State) extends CppCodegen with CppFileGen with CppGenCommon with CppGenInterface  
			with CppGenAccel with CppGenDebug with CppGenMath {
}
