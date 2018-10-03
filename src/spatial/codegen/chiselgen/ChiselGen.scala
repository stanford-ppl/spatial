package spatial.codegen.chiselgen

import argon._

case class ChiselGen(IR: State) extends ChiselCodegen
	with ChiselFileGen
  with ChiselGenController
	with ChiselGenCounter
  with ChiselGenDebug
	with ChiselGenDelay
	with ChiselGenDRAM
  with ChiselGenInterface
  with ChiselGenMath
  with ChiselGenMem
	with ChiselGenStream
	with ChiselGenStruct
	with ChiselGenVec
