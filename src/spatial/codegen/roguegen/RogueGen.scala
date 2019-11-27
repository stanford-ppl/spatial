package spatial.codegen.roguegen

import argon._

case class RogueGen(IR: State) extends RogueCodegen
	with RogueFileGen
	with RogueGenCommon
	with RogueGenInterface
	with RogueGenAccel
	with RogueGenDebug
	with RogueGenMath
	with RogueGenArray
//	with RogueGenFileIO
