package spatial.metadata

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.bounds.Expect
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.util.spatialConfig
import forge.tags.stateful

package object memory {

  implicit class AccumulatorOps(s: Sym[_]) {
    def accumType: AccumType = metadata[AccumulatorType](s).map(_.tp).getOrElse(AccumType.Unknown)
    def accumType_=(tp: AccumType): Unit = metadata.add(s, AccumulatorType(tp))

    def reduceType: Option[ReduceFunction] = metadata[ReduceType](s).map(_.func)
    def reduceType_=(func: ReduceFunction): Unit = metadata.add(s, ReduceType(func))
    def reduceType_=(func: Option[ReduceFunction]): Unit = func.foreach{f => s.reduceType = f }

    def fmaReduceInfo: Option[(Sym[_],Sym[_],Sym[_],Sym[_],Double)] = metadata[FMAReduce](s).map(_.info)
    def fmaReduceInfo_=(info: (Sym[_],Sym[_],Sym[_],Sym[_],Double)): Unit = metadata.add(s, FMAReduce(info))
    def fmaReduceInfo_=(info: Option[(Sym[_],Sym[_],Sym[_],Sym[_],Double)]): Unit = info.foreach{f => s.fmaReduceInfo = f }

    def getIterDiff: Option[Int] = metadata[IterDiff](s).map(_.diff)
    def iterDiff: Int = metadata[IterDiff](s).map(_.diff).getOrElse(1)
    def iterDiff_=(diff: Int): Unit = metadata.add(s, IterDiff(diff))
    def removeIterDiff: Unit = metadata.add(s,IterDiff(1))

    def segmentMapping: Map[Int,Int] = metadata[SegmentMapping](s).map(_.mapping).getOrElse(Map[Int,Int]())
    def segmentMapping_=(mapping: Map[Int,Int]): Unit = metadata.add(s, SegmentMapping(mapping))
    def removeSegmentMapping: Unit = metadata.add(s,SegmentMapping(Map[Int,Int]()))

    def isInnerAccum: Boolean = metadata[InnerAccum](s).map(_.isInnerAccum).getOrElse(false)
    def isInnerAccum_=(v: Boolean): Unit = metadata.add(s, InnerAccum(v))

    def isInnerReduceOp: Boolean = metadata[InnerReduceOp](s).map(_.flag).getOrElse(false)
    def isInnerReduceOp_=(v: Boolean): Unit = metadata.add(s, InnerReduceOp(v))
  }

  implicit class BankedMemoryOps(s: Sym[_]) {
    def isWriteBuffer: Boolean = metadata[EnableWriteBuffer](s).exists(_.flag)
    def isWriteBuffer_=(flag: Boolean): Unit = metadata.add(s, EnableWriteBuffer(flag))

    def isNonBuffer: Boolean = metadata[EnableNonBuffer](s).exists(_.flag)
    def isNonBuffer_=(flag: Boolean): Unit = metadata.add(s, EnableNonBuffer(flag))

    def isNoHierarchicalBank: Boolean = metadata[NoHierarchicalBank](s).exists(_.flag)
    def isNoHierarchicalBank_=(flag: Boolean): Unit = metadata.add(s, NoHierarchicalBank(flag))

    def noBlockCyclic: Boolean = metadata[NoBlockCyclic](s).exists(_.flag)
    def noBlockCyclic_=(flag: Boolean): Unit = metadata.add(s, NoBlockCyclic(flag))

    def onlyBlockCyclic: Boolean = metadata[OnlyBlockCyclic](s).exists(_.flag)
    def onlyBlockCyclic_=(flag: Boolean): Unit = metadata.add(s, OnlyBlockCyclic(flag))

    def blockCyclicBs: Seq[Int] = metadata[BlockCyclicBs](s).map(_.bs).getOrElse {
      Seq(2, 4, 8, 16, 32, 64, 128, 256)
    }
    def blockCyclicBs_=(bs: Seq[Int]): Unit = metadata.add(s, BlockCyclicBs(bs))

    def shouldIgnoreConflicts: Boolean = metadata[IgnoreConflicts](s).exists(_.flag)
    def shouldIgnoreConflicts_=(flag: Boolean): Unit = metadata.add(s, IgnoreConflicts(flag))

    @stateful def bankingEffort: Int = metadata[BankingEffort](s).map(_.effort).getOrElse(spatialConfig.bankingEffort)
    def bankingEffort_=(effort: Int): Unit = metadata.add(s, BankingEffort(effort))

    def isNoFlatBank: Boolean = metadata[NoFlatBank](s).exists(_.flag)
    def isNoFlatBank_=(flag: Boolean): Unit = metadata.add(s, NoFlatBank(flag))

    def isOnlyDuplicate: Boolean = metadata[OnlyDuplicate](s).exists(_.flag)
    def isOnlyDuplicate_=(flag: Boolean): Unit = metadata.add(s, OnlyDuplicate(flag))

    def duplicateOnAxes: Option[Seq[Seq[Int]]] = metadata[DuplicateOnAxes](s).map(_.opts)
    def duplicateOnAxes_=(opts: Seq[Seq[Int]]): Unit = metadata.add(s, DuplicateOnAxes(opts))

    def isNoDuplicate: Boolean = metadata[NoDuplicate](s).exists(_.flag)
    def isNoDuplicate_=(flag: Boolean): Unit = metadata.add(s, NoDuplicate(flag))

    def shouldCoalesce: Boolean = metadata[ShouldCoalesce](s).exists(_.flag)
    def shouldCoalesce_=(flag: Boolean): Unit = metadata.add(s, ShouldCoalesce(flag))

    /** Pre-unrolling duplicates (one or more Memory instances per node) */

    def getDuplicates: Option[Seq[Memory]] = metadata[Duplicates](s).map(_.d)
    def duplicates: Seq[Memory] = getDuplicates.getOrElse{throw new Exception(s"No duplicates defined for $s")}
    def duplicates_=(ds: Seq[Memory]): Unit = metadata.add(s, Duplicates(ds))

    /** Post-banking analysis metadata about padding based on banking selection */

    def getPadding: Option[Seq[Int]] = metadata[Padding](s).map(_.dims)
    def padding: Seq[Int] = getPadding.getOrElse{throw new Exception(s"No padding defined for $s")}
    def padding_=(ds: Seq[Int]): Unit = metadata.add(s, Padding(ds))

    def getDarkVolume: Option[Int] = metadata[DarkVolume](s).map(_.b)
    def darkVolume: Int = getDarkVolume.getOrElse{throw new Exception(s"No darkVolume defined for $s")}
    def darkVolume_=(b: Int): Unit = metadata.add(s, DarkVolume(b))

    /** Stride info for LineBuffer */
    @stateful def stride: Int = s match {case Op(_@LineBufferNew(_,_,stride)) => stride match {case Expect(c) => c.toInt; case _ => -1}; case _ => -1}

    /** Post-unrolling duplicates (exactly one Memory instance per node) */

    def getInstance: Option[Memory] = getDuplicates.flatMap(_.headOption)
    def instance: Memory = getInstance.getOrElse{throw new Exception(s"No instance defined for $s")}
    def instance_=(inst: Memory): Unit = metadata.add(s, Duplicates(Seq(inst)))

    def broadcastsAnyRead: Boolean = s.readers.exists{r => if (r.getPorts.isDefined) r.port.broadcast.exists(_ > 0) else false}

    /** Controllers just below the LCA who are responsible for swapping a buffered memory */
    import forge.tags.stateful
    @stateful def swappers: List[Sym[_]] = {
      val accesses = s.accesses.filter(_.port.bufferPort.isDefined)
      if (accesses.nonEmpty) {
        val lca = if (accesses.size == 1) accesses.head.parent else LCA(accesses)
        if (lca.isParallel){ // Assume memory analysis chose buffering based on lockstep of different bodies within this parallel, and just use one
          val releventAccesses = accesses.toList.filter(_.ancestors.contains(lca.children.head)).toSet
          val logickingLca = LCA(releventAccesses)
          val (basePort, numPorts) = if (logickingLca.s.get.isInnerControl) (0,0) else LCAPortMatchup(releventAccesses.toList, logickingLca)
          val info = if (logickingLca.s.get.isInnerControl) List[Sym[_]]() else (basePort to {basePort+numPorts}).map { port => logickingLca.children.toList(port).s.get }
          info.toList        
        } else {
          val (basePort, numPorts) = if (lca.s.get.isInnerControl) (0,0) else LCAPortMatchup(accesses.toList, lca)
          val info = if (lca.s.get.isInnerControl) List[Sym[_]]() else (basePort to {basePort+numPorts}).map { port => lca.children.toList(port).s.get }
          info.toList        
        }
      } else {
        throw new Exception(s"Cannot create a buffer on $s, which has no accesses")
      }
    }

  }

  implicit class BankedAccessOps(s: Sym[_]) {
    def getDispatches: Option[Map[Seq[Int], Set[Int]]] = metadata[Dispatch](s).map(_.m)
    def dispatches: Map[Seq[Int], Set[Int]] = getDispatches.getOrElse{ Map.empty }
    def dispatches_=(ds: Map[Seq[Int],Set[Int]]): Unit = metadata.add(s, Dispatch(ds))
    def getDispatch(uid: Seq[Int]): Option[Set[Int]] = getDispatches.flatMap(_.get(uid))
    def dispatch(uid: Seq[Int]): Set[Int] = getDispatch(uid).getOrElse{throw new Exception(s"No dispatch defined for $s {${uid.mkString(",")}}")}

    def addDispatch(uid: Seq[Int], d: Int): Unit = getDispatch(uid) match {
      case Some(set) => s.dispatches += (uid -> (set + d))
      case None      => s.dispatches += (uid -> Set(d))
    }
    def getGroupIds: Option[Map[Seq[Int], Set[Int]]] = metadata[GroupId](s).map(_.m)
    def getGroupId(uid: Seq[Int]): Option[Set[Int]] = getGroupIds.flatMap(_.get(uid))
    def gid(uid: Seq[Int]): Set[Int] = getGroupId(uid).getOrElse{throw new Exception(s"No group id defined for $s {${uid.mkString(",")}}")}
    def gids: Map[Seq[Int], Set[Int]] = getGroupIds.getOrElse{ Map.empty }
    def gids_=(gs: Map[Seq[Int],Set[Int]]): Unit = metadata.add(s, GroupId(gs))
    def addGroupId(uid: Seq[Int], g: Set[Int]): Unit = getGroupId(uid) match {
      case Some(set) => s.gids += (uid -> (set ++ g))
      case None      => s.gids += (uid -> g)
    }

    def getPorts: Option[Map[Int, Map[Seq[Int],Port]]] = metadata[Ports](s).map(_.m)
    def getPorts(dispatch: Int): Option[Map[Seq[Int],Port]] = getPorts.flatMap(_.get(dispatch))
    def ports(dispatch: Int): Map[Seq[Int],Port] = getPorts(dispatch).getOrElse{ throw new Exception(s"No ports defined for $s on dispatch #$dispatch") }
    def addPort(dispatch: Int, uid: Seq[Int], port: Port): Unit = getPorts match {
      case Some(map) => map.get(dispatch) match {
        case Some(ports) => metadata.add(s, Ports(map + (dispatch -> (ports + (uid -> port)))))
        case None        => metadata.add(s, Ports(map + (dispatch -> Map(uid -> port))))
      }
      case None      => metadata.add(s, Ports(Map(dispatch -> Map(uid -> port))))
    }

    def getPort(dispatch: Int, uid: Seq[Int]): Option[Port] = getPorts(dispatch).flatMap(_.get(uid))
    def port(dispatch: Int, uid: Seq[Int]): Port = getPort(dispatch, uid).getOrElse{ throw new Exception(s"No ports defined for $s {${uid.mkString(",")}}") }

    /** Returns the final port after unrolling. For use after unrolling only. */
    def port: Port = getPorts(0).flatMap(_.values.headOption).getOrElse{ throw new Exception(s"No final port defined for $s") }
    def setBufPort(p: Int): Unit = {
      val originalPortMetadata = getPorts
      if (originalPortMetadata.isDefined) {
        val originalPort = port
        val newPort = Port(Some(p), originalPort.muxPort, originalPort.muxOfs, originalPort.castgroup, originalPort.broadcast)
        metadata.remove(s, classOf[Ports])
        originalPortMetadata.get.foreach{case (k,v) => metadata.add(s, Ports(Map((k -> Map((v.toList.head._1 -> newPort))))))}
      }
    }

  }


  implicit class MemoryOps(mem: Sym[_]) {
    /** Returns the statically defined rank (number of dimensions) of the given memory. */
    def sparseRank: Seq[Int] = mem match {
      case Op(m: MemAlloc[_,_])   => m.rank
      case Op(m: MemAlias[_,_,_]) => m.sparseRank
      case _ => throw new Exception(s"Could not statically determine the rank of $mem")
    }

    /** Returns the statically defined underlying rank (number of dimensions) of the given memory. */
    def rawRank: Seq[Int] = mem match {
      case Op(m: MemAlloc[_,_])   => m.rank
      case Op(m: MemAlias[_,_,_]) => m.rawRank
      case _ => throw new Exception(s"Could not statically determine the rank of $mem")
    }

    /** Returns the statically defined staged dimensions (symbols) of the given memory. */
    def stagedDims: Seq[I32] = mem match {
      case Op(m: MemAlloc[_,_]) => m.dims
      case _ => throw new Exception(s"Could not statically determine the dimensions of $mem")
    }

    def stagedSize: I32 = mem match {
      case Op(m: MemAlloc[_,_]) if m.dims.size == 1 => m.dims.head
      case _ => throw new Exception(s"Could not get static size of $mem")
    }

    def hotSwapPairings: Map[Sym[_], Set[Sym[_]]] = {
      metadata[HotSwapPairings](mem).map(_.pairings).getOrElse(Map.empty)
    }
    def substHotSwap(src: Sym[_], dst: Sym[_]): Unit = {
      if (hotSwapPairings.map(_._1).toList.contains(src)) {
        hotSwapPairings = hotSwapPairings.filter(_._1 != src) ++ Map((dst -> hotSwapPairings(src)))
      } else if (hotSwapPairings.map(_._2).flatten.toList.contains(src)) {
        val newMap = hotSwapPairings.map{case (k,v) => 
          if (v.contains(src)) (k -> (v.filter(_ != src) ++ Set(dst)))
          else (k -> v)
        }
        hotSwapPairings = newMap
      }
    }
    def hotSwapPairings_=(pairings: Map[Sym[_], Set[Sym[_]]]): Unit = {
      metadata.add(mem, HotSwapPairings(pairings)) 
    }

    /** Returns constant values of the dimensions of the given memory. */
    @stateful def constDims: Seq[Int] = {
      if (stagedDims.forall{case Expect(c) => true; case _ => false}) {
        stagedDims.collect{case Expect(c) => c.toInt }
      }
      else {
        throw new Exception(s"Could not get constant dimensions of $mem")
      }
    }

    @stateful def getConstDims: Option[Seq[Int]] = {
      if (stagedDims.forall{case Expect(c) => true; case _ => false}) {
        Some(stagedDims.collect{case Expect(c) => c.toInt })
      } else None
    }

    def readWidths: Set[Int] = mem.readers.map{
      case Op(read: UnrolledAccessor[_,_]) => read.width
      case _ => 1
    }

    def writeWidths: Set[Int] = mem.writers.map{
      case Op(write: UnrolledAccessor[_,_]) => write.width
      case _ => 1
    }

    def isDuplicatable: Boolean = (mem.isSRAM || mem.isReg || mem.isRegFile || mem.isLUT)

    def isLocalMem: Boolean = mem match {
      case _: LocalMem[_,_] => true
      case _ => false
    }
    def isRemoteMem: Boolean = mem match {
      case _: RemoteMem[_,_] => true
      case _: Reg[_] => mem.isArgOut || mem.isArgIn || mem.isHostIO
      case _ => false
    }

    def asMem[C[_]]:Mem[_,C] = mem.asInstanceOf[Mem[_,C]]
    def isMem: Boolean = isLocalMem || isRemoteMem
    def isDenseAlias: Boolean = mem.op.exists{
      case _: MemDenseAlias[_,_,_] => true
      case _ => false
    }
    def isSparseAlias: Boolean = mem.op.exists{
      case _: MemSparseAlias[_,_,_,_,_] => true
      case _ => false
    }

    def isNBuffered: Boolean = mem.getInstance.exists(_.depth > 1)
    
    def isOptimizedReg: Boolean = mem.writers.exists{ _.op.get.isInstanceOf[RegAccum[_]] }
    def optimizedRegType: Option[Accum] = if (!mem.isOptimizedReg) None else 
      mem.writers.collect{ 
      case x if x.op.get.isInstanceOf[RegAccum[_]] => x}.head match {
        case Op(RegAccumOp(_,_,_,t,_)) => Some(t)
        case Op(_: RegAccumFMA[_]) => Some(AccumFMA)
        case Op(_: RegAccumLambda[_]) => Some(AccumUnk)
      }
    def isReg: Boolean = mem.isInstanceOf[Reg[_]]
    def isArgIn: Boolean = mem.isReg && mem.op.exists{ _.isInstanceOf[ArgInNew[_]] }
    def isArgOut: Boolean = mem.isReg && mem.op.exists{ _.isInstanceOf[ArgOutNew[_]] }
    def isHostIO: Boolean = mem.isReg && mem.op.exists{ _.isInstanceOf[HostIONew[_]] }

    def isDRAM: Boolean = mem match {
      case _:DRAM[_,_] => true
      case _ => false
    }
    def isDRAMAccel: Boolean = mem.op.exists{ case _: DRAMAccelNew[_,_] => true; case _ => false}

    def isStreamIn: Boolean = mem.isInstanceOf[StreamIn[_]]
    def isStreamOut: Boolean = mem.isInstanceOf[StreamOut[_]]
    def isInternalStream: Boolean = (mem.isStreamIn || mem.isStreamOut) && mem.parent != Ctrl.Host

    def isMemPrimitive: Boolean = (isSRAM || isLineBuffer || isRegFile || isFIFO || isLIFO || isFIFOReg || isReg || isLUT) && !isNBuffered && (!isRemoteMem && !isOptimizedReg)


    def isSRAM: Boolean = mem match {
      case _: SRAM[_,_] => true
      case _ => false
    }
    def isRegFile: Boolean = mem match {
      case _: RegFile[_,_] => true
      case _ => false
    }
    def isLineBuffer: Boolean = mem.isInstanceOf[LineBuffer[_]]
    def isFIFO: Boolean = mem.isInstanceOf[FIFO[_]]
    def isLIFO: Boolean = mem.isInstanceOf[LIFO[_]]
    def isMergeBuffer: Boolean = mem.isInstanceOf[MergeBuffer[_]]
    def isFIFOReg: Boolean = mem.isInstanceOf[FIFOReg[_]]
    def hasDestructiveReads: Boolean = isFIFO || isLIFO || isFIFOReg

    def isLUT: Boolean = mem match {
      case _: LUT[_,_] => true
      case _ => false
    }

    def memName: String = mem match {
      case _: LUT[_,_] => "LUT"
      case _: SRAM[_,_] => "BankedSRAM"
      case _: Reg[_] => "FF"
      case _: FIFOReg[_] => "FIFOReg"
      case _: RegFile[_,_] => "ShiftRegFile"
      case _: LineBuffer[_] => "LineBuffer"
      case _: MergeBuffer[_] => "MergeBuffer"
      case _: FIFO[_] => "FIFO"
      case _: LIFO[_] => "LIFO"
      case _ => "Unknown"
    }

    def hasInitialValues: Boolean = mem match {
      case Op(RegNew(_)) => true
      case Op(RegFileNew(_,inits)) => inits.isDefined
      case Op(LUTNew(_,_)) => true
      case _ => false
    }
  }


  implicit class MemoryAccessOps(s: Sym[_]) {
    def readers: Set[Sym[_]] = metadata[Readers](s).map(_.readers).getOrElse(Set.empty)
    def readers_=(rds: Set[Sym[_]]): Unit = metadata.add(s, Readers(rds))

    def writers: Set[Sym[_]] = metadata[Writers](s).map(_.writers).getOrElse(Set.empty)
    def writers_=(wrs: Set[Sym[_]]): Unit = metadata.add(s, Writers(wrs))

    def accesses: Set[Sym[_]] = s.readers ++ s.writers

    def resetters: Set[Sym[_]] = metadata[Resetters](s).map(_.resetters).getOrElse(Set.empty)
    def resetters_=(rst: Set[Sym[_]]): Unit = metadata.add(s, Resetters(rst))

    def originalSym: Option[Sym[_]] = metadata[OriginalSym](s).map(_.forbiddenFruit).headOption
    def originalSym_=(forbiddenFruit: Sym[_]): Unit = metadata.add(s, OriginalSym(forbiddenFruit))

    def dephasedAccesses: Set[Sym[_]] = metadata[DephasedAccess](s).map(_.accesses).getOrElse(Set.empty)
    def addDephasedAccess(access: Sym[_]): Unit = metadata.add(s, DephasedAccess(Set(access) ++ s.dephasedAccesses))

    def isUnusedMemory: Boolean = metadata[UnusedMemory](s).exists(_.flag)
    def isUnusedMemory_=(flag: Boolean): Unit = metadata.add(s, UnusedMemory(flag))

    def isBreaker: Boolean = metadata[Breaker](s).exists(_.flag)
    def isBreaker_=(flag: Boolean): Unit = metadata.add(s, Breaker(flag))

    def getBroadcastAddr: Option[Boolean] = metadata[BroadcastAddress](s).map(_.flag).headOption
    def isBroadcastAddr: Boolean = metadata[BroadcastAddress](s).exists(_.flag)
    def isBroadcastAddr_=(flag: Boolean): Unit = metadata.add(s, BroadcastAddress(flag))

    /** Find Fringe<Dense/Sparse><Load/Store> streams associated with this DRAM */
    def loadStreams: List[Sym[_]] = s.consumers.filter(_.isLoad).toList
    /** Find Fringe<Dense/Sparse><Load/Store> streams associated with this DRAM */
    def storeStreams: List[Sym[_]] = s.consumers.filter(_.isStore).toList
    /** Find Fringe<Dense/Sparse><Load/Store> streams associated with this DRAM */
    def gatherStreams: List[Sym[_]] = s.consumers.filter(_.isGather).toList
    /** Find Fringe<Dense/Sparse><Load/Store> streams associated with this DRAM */
    def scatterStreams: List[Sym[_]] = s.consumers.filter(_.isScatter).toList

    /** Get BurstCmd bus */
    def addrStream: Sym[_] = s match {
      case Op(FringeDenseStore(_,cmd,_,_)) => cmd
      case Op(FringeDenseLoad(_,cmd,_)) => cmd
      case Op(FringeSparseLoad(_,cmd,_)) => cmd
      case Op(FringeSparseStore(_,cmd,_)) => cmd //sic
      case _ => throw new Exception("No addrStream for $s")
    }

    def dataStream: Sym[_] = s match {
      case Op(FringeDenseStore(_,_,data,_)) => data
      case Op(FringeDenseLoad(_,_,data)) => data
      case Op(FringeSparseLoad(_,_,data)) => data
      case _ => throw new Exception("No dataStream for $s")
    }

    def ackStream: Sym[_] = s match {
      case Op(FringeDenseStore(_,_,_,ack)) => ack
      case Op(FringeSparseStore(_,_,ack)) => ack
      case _ => throw new Exception("No dataStream for $s")
    }
  }


}
