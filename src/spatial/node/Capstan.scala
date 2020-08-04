package spatial.node

import argon._
import argon.node._
import forge.tags._
import spatial.lang._
import spatial.metadata.memory._
import spatial.metadata.control._
import spatial.metadata.bounds._
import spatial.util.memops._
import spatial.util.spatialConfig
import spatial.util.modeling.target

//@op case class BitVecGenerator[Local[T]<:LocalMem1[T,Local]](tree: Boolean, shift: scala.Int,
  //max: I32, len: Local[I32], indices: Local[I32],
  //bv: Local[U32], last: Local[Bit])  extends FringeNode[I32,Void] {
    //override def effects = Effects.Writes(bv, last) andAlso Effects.Sticky andAlso Effects.Reads(indices)
    // override def effects = Effects.Writes(bv, last) andAlso Effects.Reads(len) andAlso Effects.Reads(indices) andAlso Effects.Sticky

  //}
@op case class BitVecGeneratorNoTree[Local[T]<:LocalMem[T,Local]](
    shift:   Int,
    indices: Local[I32],
    bv:      Local[U32],
    params:  Tup2[I32,I32], // max and len
    ens:     Set[Bit] = Set.empty,
    )
  extends EarlyBlackBox[Void] {

  override def effects: Effects = Effects.Writes(bv) 
  @rig def lower(old:Sym[Void]): Void = BitVecGeneratorNoTree.transfer(old, shift, indices, bv, params, ens)
}

object BitVecGeneratorNoTree {

  @virtualize
  @rig def transfer[Local[T]<:LocalMem[T,Local]](
    old:     Sym[Void],
    shift:   Int,
    indices: Local[I32],
    bv:      Local[U32],
    params:  Tup2[I32,I32], // max and len
    ens:     Set[Bit],
    ) : Void = {

    assert(spatialConfig.enablePIR)
    val max = params._1
    val len = params._2

    val top = Stream {
      val indexFIFO = indices.asInstanceOf[Sym[_]] match {case Op(_:FIFONew[_]) => true; case _ => false}
      val bvFIFO    = bv.asInstanceOf[Sym[_]]      match {case Op(_:FIFONew[_]) => true; case _ => false}

      val setupBus = StreamOut[Tup2[I32,I32]](BlackBoxBus[Tup2[I32,I32]]("setupBus"))
      val idxBus = StreamOut[I32](BlackBoxBus[I32]("idxBus"))
      val retBus = StreamIn[U32](BlackBoxBus[U32]("retBus"))

      setupBus := pack(max.unbox, len.unbox)
      Foreach(len.unbox par 16){i =>
        val idx       = indices.__read(Seq(i), Set.empty)
        idxBus := idx
      }
      // Fringe
      val op = Fringe.bvBuildNoTree(shift, setupBus, idxBus, retBus)
      // transferSyncMeta(old, store)
      // Receive
      // Foreach(len.unbox by p par 1){i =>
      //Stream (*) {
        Foreach(max by 32 par 16){i =>
          val ret = retBus.value()
          // val bitvec = retBus.value()._1
          // val l = retBus.value()._2
          bv.__write(ret, Seq(i), Set.empty)
          //last.__write(ret._2, Seq(i), Set.empty)
        }
      //}
    }
  }
}

@op case class BitVecGeneratorTree[Local[T]<:LocalMem[T,Local]](
    shift:   Int,
    indices: Local[I32],
    bv:      Local[U32],
    prevLen:      Local[I32],
    last:      Local[Bit],
    params:  Tup2[I32,I32], // discard and len
    ens:     Set[Bit] = Set.empty,
    )
  extends EarlyBlackBox[Void] {

  override def effects: Effects = Effects.Writes(bv, last, prevLen) 
  @rig def lower(old:Sym[Void]): Void = BitVecGeneratorTree.transfer(old, shift, indices, bv, prevLen, last, params, ens)
}

object BitVecGeneratorTree {

  @virtualize
  @rig def transfer[Local[T]<:LocalMem[T,Local]](
    old:     Sym[Void],
    shift:   Int,
    indices: Local[I32],
    bv:      Local[U32],
    prevLen:      Local[I32],
    last:      Local[Bit],
    params:  Tup2[I32,I32], // discard and len
    ens:     Set[Bit],
    ) : Void = {

    assert(spatialConfig.enablePIR)
    val discard = params._1
    val len = params._2

    val top = Stream {
      val indexFIFO   = indices.asInstanceOf[Sym[_]] match {case Op(_:FIFONew[_]) => true; case _ => false}
      val bvFIFO      = bv.asInstanceOf[Sym[_]]      match {case Op(_:FIFONew[_]) => true; case _ => false}
      val prevLenFIFO = prevLen.asInstanceOf[Sym[_]] match {case Op(_:FIFONew[_]) => true; case _ => false}
      val lastFIFO    = last.asInstanceOf[Sym[_]]    match {case Op(_:FIFONew[_]) => true; case _ => false}

      val setupBus = StreamOut[I32](BlackBoxBus[I32]("setupBus"))
      val idxBus = StreamOut[I32](BlackBoxBus[I32]("idxBus"))
      val vecRetBus = StreamIn[U32](BlackBoxBus[U32]("vecRetBus"))
      val scalRetBus = StreamIn[Tup2[I32,Bit]](BlackBoxBus[Tup2[I32,Bit]]("scalRetBus"))

      setupBus := len.unbox
      Foreach(len.unbox par 16){i =>
        val idx       = indices.__read(Seq(i), Set.empty)
        idxBus := idx
      }
      // Fringe
      val op = Fringe.bvBuildTree(shift, setupBus, idxBus, vecRetBus, scalRetBus)
      Stream (*) {
        Foreach(16 par 16){i =>
          val vecRet = vecRetBus.value()
          bv.__write(vecRet, Seq(i), Set.empty)
        }
        val scalRet = scalRetBus.value()
        prevLen.__write(scalRet._1, Seq(), Set.empty)
        last.__write(scalRet._2, Seq(), Set.empty)
      }
    }
  }
}
