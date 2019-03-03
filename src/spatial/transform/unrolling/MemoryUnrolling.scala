package spatial.transform.unrolling

import argon._
import spatial.metadata.access._
import spatial.metadata.memory._
import spatial.lang._
import spatial.node._
import utils.tags.instrument
import utils.implicits.collections._

import scala.collection.mutable.ArrayBuffer
import spatial.util.IntLike._

trait MemoryUnrolling extends UnrollingBase {

  override def unroll[T:Type](lhs: Sym[T], rhs: Op[T])(implicit ctx: SrcCtx): List[Sym[_]] = rhs match {
    case _: MemAlloc[_,_] if lhs.isRemoteMem => unrollGlobalMemory(lhs, rhs)
    case _: MemAlloc[_,_] if lhs.isLocalMem  => unrollMemory(lhs)

    case op: StatusReader[_] => unrollStatus(lhs, op)
    case op: Resetter[_]   => unrollResetter(lhs, op)
    case op: Accessor[_,_] => unrollAccess(lhs, op)
    case op: ShuffleOp[_] => unrollShuffle(lhs, op)

    // TODO: treat vector enqueues like single operations for now
    //    case op:VectorEnqueueLikeOp[_] if op.localWrites.length == 1 =>
    //      val (mem,data,addr,en) = op.localWrites.head
    //      unrollAccess(lhs,mem,data,addr,en,lanes)(op.mT,op.bT,ctx)

    //    case op:VectorReader[_,_] if op.localReads.length == 1 =>
    //      val (mem,addr,en) = op.localReads.head
    //      unrollVectorAccess(lhs,mem,NoData,addr,en,lanes, len=op.accessWidth, axis=Some(op.axis))(op.mT,op.bT,ctx)

    //    case op:VectorWriter[_,_] if op.localWrites.length == 1 =>
    //      val (mem,data,addr,en) = op.localWrites.head
    //      unrollVectorAccess(lhs,mem,DataOption(data),addr,en,lanes, len=op.accessWidth, axis=Some(op.axis))(op.mT,op.bT,ctx)

    case _ => super.unroll(lhs,rhs)
  }


  /**
    * Unrolls a status check node on the given memory.
    * Assumes that the memory being checked may have no more than one duplicate.
    * Status checks on duplicates memories are currently undefined.
    */
  def unrollStatus[A](lhs: Sym[A], rhs: StatusReader[A])(implicit ctx: SrcCtx): List[Sym[_]] = {
    val mem = rhs.mem
    if (mem.duplicates.length > 1) {
      warn(lhs.ctx, "Unrolling a status check node on a duplicated memory. Behavior is undefined.")
      warn(lhs.ctx)
    }
    dbgs(s"Unrolling status ${stm(lhs)}")
    dbgs(s"  on memory $mem -> ${memories((mem,0))}")
    val lhs2s = lanes.map{i =>
      val lhs2 = isolateSubstWith(escape=Nil, mem -> memories((mem,0)) ){ mirror(lhs, rhs) }
      dbgs(s"  Lane #$i: ${stm(lhs2)}")
      register(lhs -> lhs2)     // Use this duplicate in this lane
      lhs2
    }
    lanes.foreach{i =>
      dbgs(s"Lane #$i:")
      subst.foreach{case (k,v) => dbgs(s"  $k -> $v") }
    }
    lhs2s
  }

  def duplicateMemory(mem: Sym[_])(implicit ctx: SrcCtx): Seq[(Sym[_], Int)] = {
    val op = mem.op.getOrElse{ throw new Exception("Could not duplicate memory with no def") }
    dbgs(s"Duplicating ${stm(mem)}")
    mem.duplicates.zipWithIndex.map{case (inst,d) =>
      dbgs(s"  #$d: $inst")
      val mem2 = mirror(mem.asInstanceOf[Sym[Any]],op.asInstanceOf[Op[Any]])
      mem2.instance = inst
      mem2.name = mem2.name.map{x => s"${x}_$d"}
      mem2.padding = inst.padding
      dbgs(s"  ${stm(mem2)}")
      strMeta(mem2)
      (mem2,d)
    }
  }

  /** Unrolls a local memory, both across lanes and duplicating within each lane.
    * Duplicates are registered internally for each lane (orig, dispatch#) -> dup.
    */
  def unrollMemory(mem: Sym[_])(implicit ctx: SrcCtx): List[Sym[_]] = {
    lanes.duplicateMem(mem){_ => duplicateMemory(mem)}
    Nil // No correct default substitution for mem - have to know dispatch number of the access
  }

  /** Unrolls a global memory
    * Assumption: Global memories are never duplicated, since they correspond to external pins / memory spaces
    */
  def unrollGlobalMemory[A](mem: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): List[Sym[_]] = {
    val mem2 = lanes.mapFirst { mirror(mem, rhs) }
    memories += (mem,0) -> mem2
    lanes.unify(mem, mem2)
  }

  /** Unrolls a reset node. Assumes this unrolls to resetting ALL duplicates. */
  def unrollResetter(lhs: Sym[Void], rhs: Resetter[_])(implicit ctx: SrcCtx): List[Sym[_]] = {
    val mem = rhs.mem
    val duplicates = memories.keys.filter(_._1 == mem)
    val lhs2 = duplicates.map{dup =>
      isolateSubstWith(escape=Nil, mem -> memories(dup)){
        val lhs2 = lanes.mapFirst { mirror(lhs, rhs) }
        lhs2
      }
    }
    lanes.unify(lhs,lhs2.head)
  }

  def unrollShuffle[A](lhs: Sym[_], rhs: ShuffleOp[A])(implicit ctx: SrcCtx): List[Sym[_]] = {
    implicit val A: Bits[A] = rhs.A
    val in = lanes.map { i => f(rhs.in).asInstanceOf[Tup2[A,Bit]] }
    implicit val vT: Type[Vec[Tup2[A,Bit]]] = Vec.bits[Tup2[A,Bit]](in.length)
    val shuffle = stage(ShuffleCompressVec(in))
    lanes.unify(lhs, shuffle)
    val vec = lanes.map { is =>
      assert(is.size == 1, s"Unhandled vectorized shuffle for lanes $is")
      val i = is.head
      val elem: Sym[Tup2[A,Bit]] = shuffle(i)
      register(lhs -> elem)
      elem
    }
    vec.toList
  }

  case class UnrollInstance(
    memory:   Sym[_],
    dispIds:  Seq[Int],
    laneIds:  Seq[Int],
    port:     Port,
    vecOfs:   Seq[Int]
  )

  /** Unrolls a memory access.
    * NOTE: Three levels of indirection here:
    * Used to separate muxed accesses into individual accesses (select lanes -> chunk)
    * Then to make distinct addresses within each chunk (select chunk -> vector)
    *
    *                             laneIds
    *                          |-----------|
    * Addresses  A    B    C    D    D    E    F
    *
    * Lanes ID:  0    1    2    3    4    5    6   [Parallelized loop iterations]
    *            |    |    |    |         |    |
    *            V    V    V    V         V    V
    * Masters:                  3         5        [Broadcaster lanes in this chunk]
    *           |_|  |______|  |___________|  |_|
    * Chunk:     0    0    1    0    1    2    0   [Index of lane within chunk]
    *            |         |    |         |    |
    *            V         V    V         V    V
    *                          |0         2|   0
    * Vector:   |_|  |______|  |___________|  |_|  [Distinct addresses]
    *            0         0    0         1    0
    *                          |-----------|
    *                             vecIds
    */
  def unrollAccess[A](lhs: Sym[_], rhs: Accessor[A,_])(implicit ctx: SrcCtx): List[Sym[_]] = {
    implicit val A: Bits[A] = rhs.A
    if (!lhs.isUnusedAccess) {
      val mem  = rhs.mem
      val addr = if (rhs.addr.isEmpty) None else Some(rhs.addr)
      val data = rhs.dataOpt // Note that this is the OLD data symbol, if any
      val mems = getInstances(lhs, mem, isLoad = data.isEmpty, None)

      dbgs(s"Unrolling ${stm(lhs)}"); strMeta(lhs)

      mems.zipWithIndex.flatMap{case (UnrollInstance(mem2,dispIds,laneIds,port,_),i) =>
        dbgs(s"  Dispatch: $dispIds")
        dbgs(s"  Lane IDs: $laneIds")
        dbgs(s"  Port:     $port")

        case class NDAddressInLane(addr: Seq[Idx], lane: Int)
        case class NDAddressAcrossLanes(addr: Seq[Idx], lanes: Seq[Int])

        val inst  = mem2.instance
        val addrOpt = addr.map{a =>
          val a2 = lanes.inLanes(laneIds){p => NDAddressInLane(f(a),p) }  // lanes of ND addresses
          dbgs(s"a2 = $a2, from in lanes $laneIds in $a")
          val distinct = a2.groupBy{_.addr}
                           .mapValues{pairs => pairs.map(_.lane)}
                           .toSeq.map{case (adr, ls) => NDAddressAcrossLanes(adr, ls) }
                           .sortBy(_.lanes.last)                        // (ND address, lane IDs) pairs

          val addr: Seq[Seq[Idx]] = distinct.map(_.addr)                // Vector of ND addresses
          val masters: Seq[Int] = distinct.map(_.lanes.last)            // Lane ID for each distinct address
          val lane2Vec: Map[Int,Int] = distinct.zipWithIndex.flatMap{case (entry,aId) => entry.lanes.map{laneId => laneId -> aId }}.toMap
          val vec2Lane: Map[Int,Int] = distinct.zipWithIndex.flatMap{case (entry,aId) => entry.lanes.map{laneId => aId -> laneId }}.toMap
          (addr, masters, lane2Vec, vec2Lane)
        }
        val addr2      = addrOpt.map(_._1)                          // Vector of ND addresses
        val masters    = addrOpt.map(_._2).getOrElse(laneIds)       // List of broadcaster lane IDs
        val lane2Vec   = addrOpt.map(_._3)                          // Lane -> Vector ID
        val vec2Lane   = addrOpt.map(_._4)                          // Vector ID -> Lane ID
        val vecIds     = masters.indices                            // List of Vector IDs
        def vecLength: Int = addr2.map(_.length).getOrElse(laneIds.length)
        def laneIdToVecId(lane: Int): Int = lane2Vec.map(_.apply(lane)).getOrElse(laneIds.indexOf(lane))
        def laneIdToChunkId(lane: Int): Int = laneIds.indexOf(lane)
        def vecToLaneAddr(vec: Int): Int = vec2Lane.map(_.apply(vec)).getOrElse(laneIds.apply(vec))

        dbgs(s"  Masters: $masters // Non-duplicated lane indices")
        // Writing two different values to the same address currently just writes the last value
        // Note this defines a race condition, so its behavior is undefined by the language
        val data2 = data.map{d =>
          val d2 = lanes.inLanes(laneIds){_ => f(d).asInstanceOf[Bits[A]] }  // Chunk of data
          masters.map{t => d2(laneIdToChunkId(t)) }                          // Vector of data
        }
        val ens2   = masters.map{t => lanes.inLanes(laneIds){p => f(rhs.ens) ++ lanes.valids(p) }(laneIdToChunkId(t)) }


        val broadcast = port.broadcast.map(_ > 0)

        val bank = addr2.map{a => bankSelects(mem,rhs,a,inst) }
        val ofs  = addr2.map{a => bankOffset(mem,lhs,a,inst) }
        /** End withFlow **/

        val banked: Seq[(UnrolledAccess[A], List[Int], Int)] = if (lhs.segmentMapping.nonEmpty && {lhs.segmentMapping.groupBy(_._2).map{case (k,v) => k -> v.keys}}.size > 1) {
          val segmentMapping = lhs.segmentMapping.groupBy(_._2).map{case (k,v) => k -> v.keys}
          dbgs(s"Fracturing access $lhs into more than 1 segment:")
          segmentMapping.collect{case (segment, lanesInSegment) if (lanesInSegment.forall(laneIds.contains)) => 
            val vecsInSegment = lanesInSegment.map(laneIdToVecId)
            dbgs(s"Segment $segment contains lanes $lanesInSegment (vecs $vecsInSegment)")
            val data3 = if (data2.isDefined) vecsInSegment.map(data2.getOrElse(Nil)(_)) else Nil
            val bank3 = vecsInSegment.map(bank.getOrElse(Nil)(_))
            val ofs3 = if (ofs.getOrElse(Nil).nonEmpty) vecsInSegment.map(ofs.getOrElse(Nil)(_)) else Nil
            val ens3 = vecsInSegment.map(ens2(_))
            implicit val vT: Type[Vec[A]] = Vec.bits[A](vecsInSegment.size)
            (bankedAccess[A](rhs, mem2, data3.toSeq, bank3.toSeq, ofs3.toSeq, ens3.toSeq), vecsInSegment.toList, segment)
          }.toSeq
        } else {
          implicit val vT: Type[Vec[A]] = Vec.bits[A](vecLength)
          Seq((bankedAccess[A](rhs, mem2, data2.getOrElse(Nil), bank.getOrElse(Nil), ofs.getOrElse(Nil), ens2), vecIds.toList, 0))
        }

        // hack for issue #90
        val newOfs = mems.map{case UnrollInstance(m,_,_,p,_) => (m,p)}.take(i).count(_ == (mem2,port))
        
        banked.flatMap(_._1.s).zipWithIndex.foreach{case (s, i) =>
          val segmentBase = banked.flatMap(_._2).take(i).length
          val segment = if (lhs.segmentMapping.nonEmpty) banked.map(_._3).apply(i) else 0
          val castgroup2 = if (lhs.segmentMapping.nonEmpty) banked.map(_._2).apply(i).map(port.castgroup) else port.castgroup
          val broadcast2 = if (lhs.segmentMapping.nonEmpty) banked.map(_._2).apply(i).map(port.broadcast) else port.broadcast
          if (s.getPorts(0).isDefined) {
            val port2 = Port(port.bufferPort,port.muxPort, port.muxOfs + newOfs + segmentBase,castgroup2,s.port.broadcast.zip(broadcast2).map{case (a,b) => scala.math.min(a,b)})
            s.addPort(dispatch=0, Nil, port2)
          }
          else {
            val port2 = Port(port.bufferPort,port.muxPort, port.muxOfs + newOfs + segmentBase,castgroup2,broadcast2)
            s.addPort(dispatch=0, Nil, port2)
          }
          s.addDispatch(Nil, 0)
          s.segmentMapping = Map(0 -> segment)
          if (lhs.getIterDiff.isDefined) s.iterDiff = lhs.iterDiff
          dbgs(s"  ${stm(s)}"); strMeta(s)
        }

        banked.flatMap{
          case (UVecRead(vec), vecsInSegment, _) =>
            // val vecsInSegment = lanesInSegment.map(laneIdToVecId)
            val elems: Seq[A] = if (lhs.segmentMapping.values.toList.sorted.reverse.headOption.getOrElse(0) >= 1) vecsInSegment.indices.map{i => vec(i) }
                                else vecsInSegment.map{i => vec(i)}
            val thisLaneIds = laneIds.filter(vecsInSegment.map(vecToLaneAddr).contains)
            lanes.inLanes(thisLaneIds){p =>
              // Convert lane to vecId
              val vecId = laneIdToVecId(p)
              // Convert vecId to true index in this segment
              val id = vecsInSegment.indexOf(vecId)
              // Get this element
              val elem: Sym[A] = elems(id)
              register(lhs -> elem)
              elem
            }

          case (URead(v), _, _)        => lanes.unifyLanes(laneIds)(lhs, v)
          case (UWrite(write), _, _)   => lanes.unifyLanes(laneIds)(lhs, write)
          case (UMultiWrite(vs), _, _) => lanes.unifyLanes(laneIds)(lhs, vs.head.s.head)
        }
      }
    }
    else Nil
  }

  def bankSelects(
    mem:       Sym[_],
    node:      Op[_],               // Pre-unrolled access
    addr:      Seq[Seq[Idx]],       // Per-lane ND address (Lanes is outer Seq, ND is inner Seq)
    inst:      Memory               // Memory instance associated with this access
  )(implicit ctx: SrcCtx): Seq[Seq[Idx]] = node match {
    // LineBuffers are special in that their first dimension is always implicitly fully banked
    //case _:LineBufferRotateEnq[_]  => addr
    // The unrolled version of register file shifting currently doesn't take a banked address
    //case _:RegFileVectorShiftIn[_] => addr
    case _:LUTRead[_,_]         => addr
    case _:RegFileShiftIn[_,_]  => addr
    case _:RegFileRead[_,_]     => addr
    case _:RegFileWrite[_,_]    => addr
    case _:LineBufferEnq[_]     => addr.map{case laneAddr => Seq(laneAddr(0))}
    case _:LineBufferRead[_]    => addr.map{case laneAddr => 
      Seq(laneAddr(0)) ++ inst.bankSelects(mem, laneAddr).drop(1)
    }
    case _ => addr.map{case laneAddr =>
      inst.bankSelects(mem, laneAddr)
    }
  }

  def bankOffset(
    mem:    Sym[_],
    access: Sym[_],
    addr:   Seq[Seq[Idx]],
    inst:   Memory
  )(implicit ctx: SrcCtx): Seq[Idx] = access match {
    case Op(_:RegFileShiftIn[_,_])  => Nil
    case Op(_:RegFileRead[_,_])     => Nil
    case Op(_:RegFileWrite[_,_])    => Nil
    case Op(_:LineBufferEnq[_])     => addr.map{case laneAddr => laneAddr(0)}
    case Op(_:LineBufferRead[_])  => addr.map{case laneAddr => 
      inst.bankOffset(mem, Seq(0.to[I32]) ++ laneAddr.drop(1))
    }
    case _ => addr.map{case laneAddr =>
      inst.bankOffset(mem, laneAddr)
    }
  }



  /** Returns the instances required to unroll the given access
    * len: width of vector access for vectorized accesses, e.g. linebuffer(0, c::c+3)
    */
  def getInstances(access: Sym[_], mem: Sym[_], isLoad: Boolean, len: Option[Int]): List[UnrollInstance] = {
    // First, find all instances each unrolled access is being dispatched to
    val is = accessIterators(access, mem)
    dbgs(s"Access: $access")
    dbgs(s"Memory: $mem")
    dbgs(s"Iterators between $access and $mem: " + is.mkString(", "))

    val words = len.map{l => Range(0,l)}.getOrElse(Range(0,1)) // For vectors

    val mems = lanes.map {laneIds =>
      words.flatMap{w =>
        val uid = is.flatMap{i => unrollNum(i) }
        val wid = if (len.isDefined) Seq(w) else Nil
        val vid = uid ++ wid
        val dispatches = access.dispatch(vid)
        if (isLoad && dispatches.size > 1) {
          bug(s"Readers should have exactly one dispatch, $access had ${dispatches.size}.")
          bug(access.ctx)
        }
        dispatches.map{dispatchId =>
          if (!memories.contains((mem, dispatchId))) {
            bug(mem.ctx, s"Duplicate #$dispatchId for memory $mem was not available!")
          }
          UnrollInstance(
            memory  = memories((mem, dispatchId)),
            dispIds = Seq(dispatchId),
            laneIds = laneIds,
            port    = access.port(dispatchId, vid),
            vecOfs  = wid
          )
        }
      }
    }.flatten

    // Then group by which logical duplicate each access is connected to
    val duplicateGroups = mems.groupBy(_.memory).toList

    duplicateGroups.flatMap{case (mem2, vs) =>
      // Then group by which physical port (buffer + mux) these accesses are connected to
      val portGroups = vs.groupBy{v => (v.port.bufferPort, v.port.muxPort) }.toSeq

      portGroups.flatMap{case ((bufferPort,muxPort), muxVs) =>
        // Finally, merge contiguous vector sections together into single vector accesses
        val accesses = muxVs.sortBy(_.port.muxOfs)
        val vectors: ArrayBuffer[ArrayBuffer[UnrollInstance]] = ArrayBuffer.empty
        accesses.foreach{access =>
          if (vectors.nonEmpty && vectors.last.last.port.muxOfs == access.port.muxOfs - 1) {
            vectors.last += access
          }
          else vectors += ArrayBuffer(access)
        }
        vectors.map{vec =>
          val muxOfs = vec.head.port.muxOfs
          val castgroups = vec.flatMap(_.port.castgroup)
          val broadcasts = vec.flatMap(_.port.broadcast)
          UnrollInstance(
            memory  = mem2,
            dispIds = vec.flatMap(_.dispIds),
            laneIds = vec.flatMap(_.laneIds),
            port    = Port(bufferPort,muxPort,muxOfs,castgroups,broadcasts),
            vecOfs  = vec.flatMap(_.vecOfs)
          )
        }
      }
    }
  }

  /** Helper classes for unrolling */
  sealed abstract class UnrolledAccess[T] { def s: Seq[Sym[_]] }
  case class URead[T](v: Sym[T]) extends UnrolledAccess[T] { def s = Seq(v) }
  case class UVecRead[T](v: Vec[T])  extends UnrolledAccess[T] { def s = Seq(v) }
  case class UWrite[T](v: Void)  extends UnrolledAccess[T] { def s = Seq(v) }
  case class UMultiWrite[T](vs: Seq[UWrite[T]]) extends UnrolledAccess[T] { def s = vs.flatMap(_.s) }

  sealed abstract class DataOption { def s: Option[Sym[_]] }
  case object NoData extends DataOption { def s = None }
  case class VecData(v: Vec[_]) extends DataOption { def s = Some(v) }
  case class Data(v: Sym[_]) extends DataOption { def s = Some(v) }
  object DataOption {
    def apply(data: Option[Sym[_]]): DataOption = data match {
      case Some(d: Vec[_]) => VecData(d)
      case Some(d)         => Data(d)
      case None            => NoData
    }
  }

  def bankedAccess[A:Bits](
    node:   Op[_],
    mem:    Sym[_],
    data:   Seq[Bits[A]],
    bank:   Seq[Seq[Idx]],
    ofs:    Seq[Idx],
    enss:   Seq[Set[Bit]]
  )(implicit vT: Type[Vec[A]], ctx: SrcCtx): UnrolledAccess[A] = node match {
    case _:MergeBufferDeq[_] => UVecRead(stage(MergeBufferBankedDeq(mem.asInstanceOf[MergeBuffer[A]], enss)))
    case _:FIFODeq[_]       => UVecRead(stage(FIFOBankedDeq(mem.asInstanceOf[FIFO[A]], enss)))
    case _:LIFOPop[_]       => UVecRead(stage(LIFOBankedPop(mem.asInstanceOf[LIFO[A]], enss)))
    case _:LUTRead[_,_]     => UVecRead(stage(LUTBankedRead(mem.asInstanceOf[LUTx[A]], bank, ofs, enss)))
    case _:RegFileRead[_,_] => UVecRead(stage(RegFileVectorRead(mem.asInstanceOf[RegFilex[A]], bank, enss)))
    case _:SRAMRead[_,_]    => UVecRead(stage(SRAMBankedRead(mem.asInstanceOf[SRAMx[A]], bank, ofs, enss)))
    case _:StreamInRead[_]  => UVecRead(stage(StreamInBankedRead(mem.asInstanceOf[StreamIn[A]], enss)))

    case op:MergeBufferEnq[_] => UWrite[A](stage(MergeBufferBankedEnq(mem.asInstanceOf[MergeBuffer[A]], op.way, data, enss)))
    case _:FIFOEnq[_]        => UWrite[A](stage(FIFOBankedEnq(mem.asInstanceOf[FIFO[A]], data, enss)))
    case _:LIFOPush[_]       => UWrite[A](stage(LIFOBankedPush(mem.asInstanceOf[LIFO[A]], data, enss)))
    case _:RegFileWrite[_,_] => UWrite[A](stage(RegFileVectorWrite(mem.asInstanceOf[RegFilex[A]], data, bank, enss)))
    case _:SRAMWrite[_,_]    => UWrite[A](stage(SRAMBankedWrite(mem.asInstanceOf[SRAMx[A]], data, bank, ofs, enss)))
    case _:StreamOutWrite[_] => UWrite[A](stage(StreamOutBankedWrite(mem.asInstanceOf[StreamOut[A]], data, enss)))

    case _:FIFOPeek[_]       => URead(stage(FIFOPeek(mem.asInstanceOf[FIFO[A]], enss.flatten.toSet)))
    case _:LIFOPeek[_]       => URead(stage(LIFOPeek(mem.asInstanceOf[LIFO[A]], enss.flatten.toSet)))

    case _:RegRead[_]        => URead(stage(RegRead(mem.asInstanceOf[Reg[A]])))
    case _:FIFORegDeq[_]    => URead(stage(FIFORegDeq(mem.asInstanceOf[FIFOReg[A]], enss.head)))
    case _:RegWrite[_]       => UWrite[A](stage(RegWrite(mem.asInstanceOf[Reg[A]],data.head, enss.head)))
    case _:FIFORegEnq[_]   => UWrite[A](stage(FIFORegEnq(mem.asInstanceOf[FIFOReg[A]],data.head, enss.head)))
    case _:SetReg[_]         => UWrite[A](stage(SetReg(mem.asInstanceOf[Reg[A]], data.head)))
    case _:GetReg[_]         => URead(stage(GetReg(mem.asInstanceOf[Reg[A]])))

    case op:RegFileShiftIn[_,_] =>
      // Group by the axis being shifted across
      // e.g. If this is a shift through a row, the axis is 0
      // TODO[1]: This is close, but how to handle edge cases where the size of the shifted vec may change?
      /*val writes = (data, bank, enss).zipped.groupBy(_._2.apply(op.axis)).foreach{case (axisAddr,values) =>
        val data = values.map(_._1).toSeq
        val ens  = values.map(_._3)
        val vec = Vec.fromSeq[A](data.map(_.unbox))
        UWrite[A](stage(RegFileShiftInVector(mem.asInstanceOf[RegFilex[A]],data,addr,ens=????,op.axis)))
      }
      if (writes.length == 1) writes.head else UMultiWrite(writes)
      */

      UMultiWrite((data, bank, enss).zipped.map{case (dat,addr,ens) =>
        UWrite[A](stage(RegFileShiftIn(mem.asInstanceOf[RegFilex[A]], dat, addr, ens, op.axis)))
      })

    case _:LineBufferEnq[_]      => UWrite[A](stage(LineBufferBankedEnq(mem.asInstanceOf[LineBuffer[A]], data, bank(0), enss)))
    case _:LineBufferRead[_]     => UVecRead(stage(LineBufferBankedRead(mem.asInstanceOf[LineBuffer[A]], bank, ofs, enss)))

    case _ => throw new Exception(s"bankedAccess called on unknown access node ${node.productPrefix}")
  }


}
