package spatial.codegen.chiselgen

import argon._
import argon.codegen.{Codegen, FileDependencies}

case class ChiselGen(IR: State) extends ChiselCodegen
	with ChiselFileGen
  with ChiselGenArray
  with ChiselGenDelay
  with ChiselGenController
  with ChiselGenDebug
  with ChiselGenInterface
  with ChiselGenStream
  with ChiselGenCounter
  with ChiselGenMath
  with ChiselGenMem
