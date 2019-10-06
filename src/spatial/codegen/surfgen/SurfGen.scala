package spatial.codegen.surfgen

import argon._

case class SurfGen(IR: State) extends SurfCodegen
	with SurfFileGen
	with SurfGenCommon
	with SurfGenInterface
	with SurfGenAccel
	with SurfGenDebug
	with SurfGenMath
	with SurfGenArray
//	with SurfGenFileIO
