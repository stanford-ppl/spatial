package spatial.codegen.chiselgen

import argon.core._
import argon.nodes._
import spatial.aliases._
import spatial.metadata._
import spatial.nodes._
import spatial.utils._
import spatial.targets.DE1._


trait ChiselGenUnrolled extends ChiselGenController {

  override protected def spatialNeedsFPType(tp: Type[_]): Boolean = tp match { // FIXME: Why doesn't overriding needsFPType work here?!?!
    case FixPtType(s,d,f) => if (s) true else if (f == 0) false else true
    case IntType()  => false
    case LongType() => false
    case HalfType() => true
    case FloatType() => true
    case DoubleType() => true
    case _ => super.needsFPType(tp)
  }

  override protected def name(s: Dyn[_]): String = s match {
    case Def(_: UnrolledForeach)     => s"${s}_foreach${s.name.map(n => s"_$n").getOrElse("")}"
    case Def(_: UnrolledReduce[_,_]) => s"${s}_reduce${s.name.map(n => s"_$n").getOrElse("")}"
    case Def(_: ParSRAMLoad[_])      => s"""${s}_parLd${s.name.getOrElse("")}"""
    case Def(_: ParSRAMStore[_])     => s"""${s}_parSt${s.name.getOrElse("")}"""
    case Def(_: ParFIFODeq[_])       => s"${s}_parDeq"
    case Def(_: ParFIFOEnq[_])       => s"${s}_parEnq"
    case _ => super.name(s)
  } 

  private def flattenAddress(dims: Seq[Exp[Index]], indices: Seq[Exp[Index]]): String = {
    val strides = List.tabulate(dims.length){i => (dims.drop(i+1).map(quote) :+ "1").mkString("*-*") }
    indices.zip(strides).map{case (i,s) => src"$i*-*$s"}.mkString(" + ")
  }


  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case UnrolledForeach(ens,cchain,func,iters,valids) =>
      val parent_kernel = controllerStack.head
      controllerStack.push(lhs)
      if (levelOf(lhs) == OuterControl) {widthStats += childrenOf(lhs).length}
      else if (levelOf(lhs) == InnerControl) {depthStats += controllerStack.length}
      emitGlobalWireMap(src"${lhs}_II_done", "Wire(Bool())")
      emitGlobalWireMap(src"${lhs}_inhibitor", "Wire(Bool())")
      // Preallocate valid bound syms
      // if (styleOf(lhs) != StreamPipe) {
      //   allocateValids(lhs, cchain, iters, valids)
      // } else if (childrenOf(lhs).length > 0) {
      //   childrenOf(lhs).zipWithIndex.foreach { case (c, idx) =>
      //     allocateValids(lhs, cchain, iters, valids, src"_copy$c") // Must have visited func before we can properly run this method
      //   }          
      // } else {
      //   emitValidsDummy(iters, valids, src"_copy$lhs") // FIXME: Weird situation with nested stream ctrlrs, hacked quickly for tian so needs to be fixed
      // }
      emitController(lhs, Some(cchain), Some(iters.flatten)) // If this is a stream, then each child has its own ctr copy
      // if (styleOf(lhs) == MetaPipe & childrenOf(lhs).length > 1) allocateRegChains(lhs, iters.flatten, cchain) // Needed to generate these global wires before visiting children who may use them
      if (levelOf(lhs) == InnerControl) emitInhibitor(lhs, Some(cchain), None, None)

      // Console.println(src"""II of $lhs is ${iiOf(lhs)}""")
      if (iiOf(lhs) <= 1) {
        emit(src"""${swap(lhs, IIDone)} := true.B""")
      } else {
        emitGlobalModule(src"""val ${lhs}_IICtr = Module(new RedxnCtr(2 + Utils.log2Up(${swap(lhs, Retime)})));""")
        emit(src"""${swap(lhs, IIDone)} := ${lhs}_IICtr.io.output.done | ${swap(lhs, CtrTrivial)}""")
        emit(src"""${lhs}_IICtr.io.input.enable := ${swap(lhs, DatapathEn)}""")
        emit(src"""${lhs}_IICtr.io.input.stop := ${swap(lhs, Retime)}.S //${iiOf(lhs)}.S""")
        emit(src"""${lhs}_IICtr.io.input.reset := accelReset | ${DL(swap(lhs, IIDone), 1, true)}""")
        emit(src"""${lhs}_IICtr.io.input.saturate := false.B""")       
      }
      if (styleOf(lhs) != StreamPipe) { 
        if (styleOf(lhs) == MetaPipe) createValidsPassMap(lhs, cchain, iters, valids)
        withSubStream(src"${lhs}", src"${parent_kernel}", levelOf(lhs) == InnerControl) {
          emit(s"// Controller Stack: ${controllerStack.tail}")
          emitParallelizedLoop(iters, cchain)
          allocateValids(lhs, cchain, iters, valids)
          if (styleOf(lhs) == MetaPipe & childrenOf(lhs).length > 1) allocateRegChains(lhs, iters.flatten, cchain) // Needed to generate these global wires before visiting children who may use them
          emitBlock(func)
        }
        emitValids(lhs, cchain, iters, valids)
      } else {
        // Not sure if these layer slicers belong at beginning or end of this else
        valids.foreach{ layer =>
          layer.foreach{ v =>
            streamCtrCopy = streamCtrCopy :+ v
          }
        }
        iters.foreach{ is =>
          is.foreach{ iter =>
            streamCtrCopy = streamCtrCopy :+ iter
          }
        }

        // if (childrenOf(lhs).length > 0) {
        //   childrenOf(lhs).zipWithIndex.foreach { case (c, idx) =>
        //     createValidsPassMap(lhs, cchain, iters, valids, src"_copy$c")
        //   }
        // }
        emitGlobalWire(s"// passmap: $validPassMap ")
        emitGlobalWire(s"// cchainpassmap: $cchainPassMap ")
        withSubStream(src"${lhs}", src"${parent_kernel}", levelOf(lhs) == InnerControl) {
          emit(s"// Controller Stack: ${controllerStack.tail}")
          childrenOf(lhs).zipWithIndex.foreach { case (c, idx) =>
            emitParallelizedLoop(iters, cchain, src"_copy$c")
          }
          if (styleOf(lhs) == MetaPipe & childrenOf(lhs).length > 1) allocateRegChains(lhs, iters.flatten, cchain) // Needed to generate these global wires before visiting children who may use them
          if (childrenOf(lhs).length > 0) {
            childrenOf(lhs).zipWithIndex.foreach { case (c, idx) =>
              allocateValids(lhs, cchain, iters, valids, src"_copy$c") // Must have visited func before we can properly run this method
            }          
          } else {
            emitValidsDummy(iters, valids, src"_copy$lhs") // FIXME: Weird situation with nested stream ctrlrs, hacked quickly for tian so needs to be fixed
          }
          // Register the remapping for bound syms in children
          emitBlock(func)
        }
        if (childrenOf(lhs).length > 0) {
          childrenOf(lhs).zipWithIndex.foreach { case (c, idx) =>
            emitValids(lhs, cchain, iters, valids, src"_copy$c") // Must have visited func before we can properly run this method
          }          
        }
      }
      emitChildrenCxns(lhs, Some(cchain), Some(iters.flatten))
      emitCopiedCChain(lhs)
      if (!(styleOf(lhs) == StreamPipe && childrenOf(lhs).length > 0)) {
        connectCtrTrivial(cchain)
      }
      val en = if (ens.isEmpty) "true.B" else ens.map(quote).mkString(" && ")
      emit(src"${swap(lhs, Mask)} := $en")
      controllerStack.pop()

    case UnrolledReduce(ens,cchain,accum,func,iters,valids) =>
      val parent_kernel = controllerStack.head
      controllerStack.push(lhs)
      if (levelOf(lhs) == OuterControl) {widthStats += childrenOf(lhs).length}
      else if (levelOf(lhs) == InnerControl) {depthStats += controllerStack.length}
      emitGlobalWireMap(src"${lhs}_II_done", "Wire(Bool())")
      emitController(lhs, Some(cchain), Some(iters.flatten))
      // allocateValids(lhs, cchain, iters, valids)
      if (levelOf(lhs) == InnerControl) emitInhibitor(lhs, Some(cchain), None, None)
      // if (styleOf(lhs) == MetaPipe & childrenOf(lhs).length > 1) allocateRegChains(lhs, iters.flatten, cchain) // Needed to generate these global wires before visiting children who may use them
      // if (styleOf(lhs) == MetaPipe) createValidsPassMap(lhs, cchain, iters, valids)
      if (iiOf(lhs) <= 1) {
        emit(src"""${swap(lhs, IIDone)} := true.B""")
      } else {
        emitGlobalModule(src"""val ${lhs}_IICtr = Module(new RedxnCtr(2 + Utils.log2Up(${swap(lhs, Retime)})));""")
        emit(src"""${swap(lhs, IIDone)} := ${lhs}_IICtr.io.output.done | ${swap(lhs, CtrTrivial)}""")
        emit(s"""${quote(lhs)}_IICtr.io.input.enable := ${swap(lhs, DatapathEn)}""")
        emit(s"""${quote(lhs)}_IICtr.io.input.stop := ${swap(lhs, Retime)}.S""")
        emit(s"""${quote(lhs)}_IICtr.io.input.reset := accelReset | ${DL(swap(lhs, IIDone), 1, true)}""")
        emit(s"""${quote(lhs)}_IICtr.io.input.saturate := false.B""")       
      }
      val dlay = bodyLatency.sum(lhs)
      accumsWithIIDlay += accum.asInstanceOf[Exp[_]]
      if (levelOf(lhs) == InnerControl) {
        emitGlobalWire(src"val ${accum}_II_dlay = 0 // Hack to fix Arbitrary Lambda")
        emitGlobalWireMap(s"${quote(accum)}_wren", "Wire(Bool())")
        emit(s"${swap(quote(accum), Wren)} := ${swap(lhs, IIDone)} & ${swap(lhs, DatapathEn)} & ~${swap(lhs, Done)} & ~${swap(lhs, Inhibitor)}")
        emitGlobalWireMap(src"${accum}_resetter", "Wire(Bool())")
        val rstr = wireMap(src"${accum}_resetter")
        // Need to delay reset by controller retime if not specialized reduction
        // if (isSpecializedReduce(accum)) {
        emit(src"$rstr := ${swap(lhs, RstEn)}")
        // } else {
        //   emit(src"$rstr := ${DL(swap(lhs, RstEn), swap(lhs, Retime), true)} // Delay was added on 12/5/2017, not sure why it wasn't there before")
        // }
      } else {
        if (spatialConfig.enableRetiming) {
          emitGlobalWire(src"val ${accum}_II_dlay = /*${iiOf(lhs)} +*/ 1 // un-hack to fix Arbitrary Lambda")
        } else {
          emitGlobalWire(src"val ${accum}_II_dlay = 0 // Hack to fix Arbitrary Lambda")        
        }
        emitGlobalWireMap(src"${accum}_wren", "Wire(Bool())")
        emit(src"// Used to be this, but not sure why for outer reduce: val ${accum}_resetter = Utils.delay(${swap(parentOf(lhs).get, Done)}, 2)")
        emitGlobalWireMap(src"${accum}_resetter", "Wire(Bool())")
        val rstr = wireMap(src"${accum}_resetter")
        emit(src"$rstr := ${swap(lhs, RstEn)}")
      }
      // Create SRFF to block destructive reads after the cchain hits the max, important for retiming
      emit(src"//val ${accum}_initval = 0.U // TODO: Get real reset value.. Why is rV a tuple?")
      withSubStream(src"${lhs}", src"${parent_kernel}", levelOf(lhs) == InnerControl) {
        emit(s"// Controller Stack: ${controllerStack.tail}")
        emitParallelizedLoop(iters, cchain)
        allocateValids(lhs, cchain, iters, valids)
        if (styleOf(lhs) == MetaPipe & childrenOf(lhs).length > 1) allocateRegChains(lhs, iters.flatten, cchain) // Needed to generate these global wires before visiting children who may use them
        if (styleOf(lhs) == MetaPipe) createValidsPassMap(lhs, cchain, iters, valids)
        emitBlock(func)
      }
      if (levelOf(lhs) != InnerControl) {
        accum match { 
          case Def(_:RegNew[_]) => 
            // if (childrenOf(lhs).length == 1) {
            emitGlobalWireMap(src"${childrenOf(lhs).last}_done", "Wire(Bool())") // Risky
            emit(src"${swap(accum, Wren)} := ${swap(childrenOf(lhs).last, SM)}.io.output.done //(${swap(childrenOf(lhs).last, Done)}) // TODO: Skeptical these codegen rules are correct ???")
          case Def(_:SRAMNew[_,_]) =>
            emitGlobalWireMap(src"${childrenOf(lhs).last}_done", "Wire(Bool())") // Risky
            emit(src"${swap(accum, Wren)} := ${swap(childrenOf(lhs).last, Done)} // TODO: SRAM accum is managed by SRAM write node anyway, this signal is unused")
          case Def(_:RegFileNew[_,_]) =>
            emitGlobalWireMap(src"${childrenOf(lhs).last}_done", "Wire(Bool())") // Risky
            emit(src"${swap(accum, Wren)} := ${swap(childrenOf(lhs).last, Done)} // TODO: SRAM accum is managed by SRAM write node anyway, this signal is unused")
        }
      }
      emitValids(lhs, cchain, iters, valids)
      emitChildrenCxns(lhs, Some(cchain), Some(iters.flatten))
      emitCopiedCChain(lhs)
      connectCtrTrivial(cchain)
      val en = if (ens.isEmpty) "true.B" else ens.map(quote).mkString(" && ")
      emit(src"${swap(lhs, Mask)} := $en")
      controllerStack.pop()


    case _ => super.emitNode(lhs, rhs)
  }
}
