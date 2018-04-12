package spatial.codegen.chiselgen

import scala.math._
import argon.core._
import argon.nodes._
import spatial.targets.DE1._

import spatial.metadata._
import spatial.nodes._
import spatial.utils._

sealed trait AppProperties
object HasLineBuffer extends AppProperties
object HasNBufSRAM extends AppProperties
object HasNBufRegFile extends AppProperties
object HasGeneralFifo extends AppProperties
object HasTileStore extends AppProperties
object HasTileLoad extends AppProperties
object HasGather extends AppProperties
object HasScatter extends AppProperties
object HasLUT extends AppProperties
object HasBreakpoint extends AppProperties
object HasAlignedLoad extends AppProperties
object HasAlignedStore extends AppProperties
object HasUnalignedLoad extends AppProperties
object HasUnalignedStore extends AppProperties
object HasStaticCtr extends AppProperties
object HasVariableCtrBounds extends AppProperties
object HasVariableCtrStride extends AppProperties
object HasFloats extends AppProperties
