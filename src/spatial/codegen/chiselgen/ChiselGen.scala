package spatial.codegen.chiselgen

import argon._
import argon.codegen.{Codegen, FileDependencies}

case class ChiselGen(IR: State) extends ChiselCodegen with ChiselFileGen with ChiselGenReg with ChiselGenController 
								with ChiselGenDebug with ChiselGenDRAM with ChiselGenStream with ChiselGenCounter 
								with ChiselGenMath {
}
