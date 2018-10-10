package spatial.codegen.chiselgen

import argon._
import spatial.util.spatialConfig

trait ChiselFileGen extends ChiselCodegen {
  backend = "accel"

  // override def emitHeader(): Unit = {
  //   inAccel { // Guarantee prints for the following

  //     inGenn(out, "IOModule", ext) {
  //       emit ("package accel")
  //       emit ("import chisel3._")
  //       emit ("import chisel3.util._")
  //       emit ("import fringe._")
  //       emit ("import utils.implicits._")
  //       emit ("import fringe.templates.math._")
  //       if (spatialConfig.compressWires == 1) {
  //         emit ("import fringe.templates._")
  //         emit ("import fringe.utils._")
  //         emit ("import fringe.utils.implicits._")
  //         emit ("import fringe.utils._")
  //       }
  //       emit ("import fringe.templates.counters._")
  //       emit ("import fringe.templates.memory._")
  //       emit ("import fringe.templates.retiming._")

  //       emit ("class CustomAccelInterface(")
  //       emit ("  io_w: Int, ")
  //       emit ("  io_v: Int, ")
  //       emit ("  io_loadStreamInfo: List[StreamParInfo], ")
  //       emit ("  io_storeStreamInfo: List[StreamParInfo], ")
  //       emit ("  io_numAllocators: Int, ")
  //       emit ("  io_numArgIns: Int, ")
  //       emit ("  io_numArgOuts: Int, ")
  //       emit ("  io_numArgOutLoopbacks: Int")
  //       emit (") extends AccelInterface{")
  //       emit ("  // Control IO")
  //       emit ("  val enable = Input(Bool())")
  //       emit ("  val done = Output(Bool())")
  //       emit ("  val reset = Input(Bool())")
  //       emit ("  ")
  //       emit ("  // DRAM IO")
  //       emit ("  val memStreams = Flipped(new AppStreams(io_loadStreamInfo, io_storeStreamInfo))")
  //       emit ("  ")
  //       emit ("  // HEAP IO")
  //       emit ("  val heap = Flipped(new HeapIO(io_numAllocators))")
  //       emit ("  ")
  //       emit ("  // Scalar IO")
  //       emit ("  val argIns = Input(Vec(io_numArgIns, UInt(64.W)))")
  //       emit ("  val argOuts = Vec(io_numArgOuts, Decoupled((UInt(64.W))))")
  //       emit ("  val argOutLoopbacks = Input(Vec(io_numArgOutLoopbacks, UInt(64.W)))")
  //       emit ("}")
        
  //       open("trait IOModule_1 extends Module {")
  // 	    emit (s"""val io_w = if ("${spatialConfig.target.name}" == "VCS" || "${spatialConfig.target.name}" == "ASIC") 8 else 32 // TODO: How to generate these properly?""")
  // 	    emit (s"""val io_v = if ("${spatialConfig.target.name}" == "VCS" || "${spatialConfig.target.name}" == "ASIC") 64 else 16 // TODO: How to generate these properly?""")
  //     }

  //     inGenn(out, "BufferControlCxns", ext) {
  //       emit(s"package accel")
  // 	    emit("import chisel3._")
  //       emit("import fringe.utils._")
  //       emit("import fringe.utils.implicits._")
  //       open(s"""trait BufferControlCxns_1 extends RootController_1 {""")
  //     }


  //     inGenn(out, "RootController", ext) {
  //       emit(s"""package accel""")
  // 	    emit(s"import chisel3._")
  // 	    emit(s"import chisel3.util._")
  //       emit("import fringe.templates._")
  //       emit("import fringe.utils._")
  //       emit("import fringe.utils.implicits._")
  //       emit("import fringe.utils._")
  //       emit("import fringe.templates.math._")
  //       emit("import fringe.templates.counters._")
  //       emit("import fringe.templates.memory._")
  //       emit("import fringe.templates.retiming._")

  //       open(s"trait RootController_1 extends InstrumentationMixer {")

  //     }

  //     inGenn(out, "GlobalWires", ext) {
  //       emit(s"""package accel""")
  // 	    emit("import chisel3._")
  // 	    emit("import chisel3.util._")
  //       emit("import fringe.templates._")
  //       emit("import fringe.utils.implicits._")
  //       emit("import fringe.utils._")
  //       emit("import fringe.templates.math._")
  //       emit("import fringe.templates.counters._")
  //       emit("import fringe.templates.memory._")
  //       emit("import fringe.templates.retiming._")
  //       emit("import scala.collection.immutable._")

  //       open(s"""trait GlobalWires_1 extends IOModule_1 {""")
  //     }

  //     inGenn(out, "GlobalModules", ext) {
  //       emit(s"""package accel""")
  // 	    emit("import chisel3._")
  //       emit("import chisel3.util._")
  //       emit("import fringe.templates._")
  //       emit("import fringe.utils.implicits._")
  //       emit("import fringe.utils._")
  //       emit("import fringe.templates.math._")
  //       emit("import fringe.templates.counters._")
  //       emit("import fringe.templates.memory._")
  //       emit("import fringe.templates.retiming._")
  //       emit("import scala.collection.immutable._")

  //       open(s"""trait GlobalModules_1 extends GlobalWiresMixer {""")
  //     }

  //     inGenn(out, "Instrumentation", ext) {
  //       emit(s"""package accel""")
  // 	    emit("import chisel3._")
  // 	    emit("import chisel3.util._")
  //       emit("import fringe.templates._")
  //       emit("import fringe.utils.implicits._")
  //       emit("import fringe.utils._")
  //       emit("import fringe.templates.math._")
  //       emit("import fringe.templates.counters._")
  //       emit("import fringe.templates.memory._")
  //       emit("import fringe.templates.retiming._")

  //       open(s"""trait Instrumentation_1 extends GlobalModulesMixer {""")
  //     }

  // //     withStream(getStream("GlobalRetiming")) {
  // //       emit(s"""package accel
  // // import chisel3._
  // //       open(s"""trait GlobalRetiming extends GlobalWiresMixer {""")
  // //     }


  //     inGen(out, "Instantiator.scala") {
  //       emit("package top")
  //       emit("")
  //       emit("import accel._")
  //       emit("import fringe._")
  //       emit("import chisel3.core.Module")
  //       emit("import chisel3._")
  //       emit("import chisel3.util._")
  //       emit("import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}")
  //       emit("")
  //       emit("import scala.collection.mutable.ListBuffer")

  //       emit("/**")
  //       emit(" * Top test harness")
  //       emit(" */")
  //       open("class TopUnitTester(c: Top)(implicit args: Array[String]) extends ArgsTester(c) {")
  //       close("}")
  //       emit("")
  //       open("object Instantiator extends CommonMain {")
  //         emit("type DUTType = Top")
  //         emit("")
  //         open("def dut = () => {")

  //     }

  //   }
  //   super.emitHeader()
  // }

  // override protected def emitEntry(block: Block[_]): Unit = { gen(block) }

  // override def emitFooter(): Unit = {
  //   inAccel {// Guarantee prints for this section

  //     inGen(out, "Instantiator.scala") {
  //       emit (s"""val w = if ("${spatialConfig.target.name}" == "zcu") 32 else if ("${spatialConfig.target.name}" == "VCS" || "${spatialConfig.target.name}" == "ASIC") 8 else 32""")
  //       emit ("val numArgIns = numArgIns_mem  + numArgIns_reg + numArgIOs_reg")
  //       emit ("val numArgOuts = numArgOuts_reg + numArgIOs_reg + numArgOuts_instr + numArgOuts_breakpts")
  //       emit ("val numArgIOs = numArgIOs_reg")
  //       emit ("val numArgInstrs = numArgOuts_instr")
  //       emit ("val numArgBreakpts = numArgOuts_breakpts")
  //       emit (s"""new Top("${spatialConfig.target.name}", () => Module(new AccelTop(w, numArgIns, numArgOuts, numArgIOs, numArgOuts_instr + numArgBreakpts, io_argOutLoopbacksMap, numAllocators, loadStreamInfo, storeStreamInfo, streamInsInfo, streamOutsInfo)))""")
  //       // emit ("new Top(w, numArgIns, numArgOuts, numArgIOs, numArgOuts_instr + numArgBreakpts, io_argOutLoopbacksMap, loadStreamInfo, storeStreamInfo, streamInsInfo, streamOutsInfo, globals.target)")
  //       close("}")
  //       emit ("def tester = { c: DUTType => new TopUnitTester(c) }")
  //       close("}")
  //     }

  //     inGenn(out, "IOModule", ext) {
  //       emit ("// Combine values")
  //       emit ("val io_numArgIns = scala.math.max(1, io_numArgIns_reg + io_numArgIns_mem + io_numArgIOs_reg)")
  //       emit ("val io_numArgOuts = scala.math.max(1, io_numArgOuts_reg + io_numArgIOs_reg + io_numArgOuts_instr + io_numArgOuts_breakpts)")
  //       emit ("val io_numArgOutLoopbacks = scala.math.max(1, io_argOutLoopbacksMap.toList.length)")
  //       emit ("val io_numArgIOs = io_numArgIOs_reg")
  //       emit ("val io_numArgInstrs = io_numArgOuts_instr")
  //       emit ("val io_numArgBreakpts = io_numArgOuts_breakpts")
  //       emit ("globals.numArgIns = io_numArgIns")
  //       emit ("globals.numArgOuts = io_numArgOuts")
  //       emit ("globals.numArgIOs = io_numArgIOs")
  //       emit ("globals.numArgInstrs = io_numArgInstrs")
  //       emit ("globals.argOutLoopbacksMap = io_argOutLoopbacksMap")
  //       emit ("globals.loadStreamInfo = io_loadStreamInfo")
  //       emit ("globals.storeStreamInfo = io_storeStreamInfo")
  //       emit ("globals.streamInsInfo = io_streamInsInfo")
  //       emit ("globals.streamOutsInfo = io_streamOutsInfo")
  //       emit ("globals.numAllocators = io_numAllocators")

  //       open("val io = IO(new CustomAccelInterface(io_w, io_v, globals.LOAD_STREAMS, globals.STORE_STREAMS, globals.numAllocators, io_numArgIns, io_numArgOuts, io_numArgOutLoopbacks))")
  // //         emit ("")
  // //         emit ("// Stream IO")
  // //         emit ("// val genericStreams = new GenericStreams(io_streamInsInfo, io_streamOutsInfo)")
  // //         emit ("// Video Stream Inputs ")
  // //         emit ("val stream_in_data            = Input(UInt(16.W))")
  // //         emit ("val stream_in_startofpacket   = Input(Bool())")
  // //         emit ("val stream_in_endofpacket     = Input(Bool())")
  // //         emit ("val stream_in_empty           = Input(UInt(2.W))")
  // //         emit ("val stream_in_valid           = Input(Bool()) ")
  // //         emit ("val stream_out_ready          = Input(Bool())")
  // //         emit (" ")
  // //         emit ("// Video Stream Outputs")
  // //         emit ("val stream_in_ready           = Output(Bool())")
  // //         emit ("val stream_out_data           = Output(UInt(16.W))")
  // //         emit ("val stream_out_startofpacket  = Output(Bool())")
  // //         emit ("val stream_out_endofpacket    = Output(Bool())")
  // //         emit ("val stream_out_empty          = Output(UInt(1.W))")
  // //         emit ("val stream_out_valid          = Output(Bool())")
  // //         emit ("")
  // //         emit ("// LED Stream Outputs ")
  // //         emit ("val led_stream_out_data       = Output(UInt(32.W))")
  // //         emit ("")
  // //         emit ("// Slider Switches Stream Inputs ")
  // //         emit ("val switch_stream_in_data     = Input(UInt(32.W))")
  // //         emit ("")
  // //         emit ("// BufferedOut Outputs (Avalon mmapped master)")
  // //         emit ("val buffout_address           = Output(UInt(32.W))")
  // //         emit ("val buffout_write             = Output(UInt(1.W))")
  // //         emit ("val buffout_writedata         = Output(UInt(16.W))")
  // //         emit ("val buffout_waitrequest       = Input(UInt(1.W))")
  // //         emit ("")
  // //         emit ("// GPI1 Read (Avalon mmapped master)")
  // // //	      emit ("val gpi1_streamin_chipselect  = Output(UInt(1.W))")
  // // 	      emit ("val gpi1_streamin_readdata    = Input(UInt(32.W))")
  // // //	      emit ("val gpi1_streamin_address     = Output(UInt(4.W))")
  // // //	      emit ("val gpi1_streamin_read        = Output(UInt(1.W))")
  // //         emit ("")
  // //         emit ("// GPO1 Write (Avalon mmapped master), write_n")
  // // //	      emit ("val gpo1_streamout_chipselect = Output(UInt(1.W))")
  // // //	      emit ("val gpo1_streamout_address    = Output(UInt(4.W))")
  // // 	      emit ("val gpo1_streamout_writedata  = Output(UInt(32.W))")
  // // //	      emit ("val gpo1_streamout_write      = Output(UInt(1.W))")
  // //         emit ("")
  // //         emit ("// GPI2 Read (Avalon mmapped master)")
  // // //	      emit ("val gpi2_streamin_chipselect  = Output(UInt(1.W))")
  // // 	      emit ("val gpi2_streamin_readdata    = Input(UInt(32.W))")
  // // //	      emit ("val gpi2_streamin_address     = Output(UInt(4.W))")
  // // //	      emit ("val gpi2_streamin_read        = Output(UInt(1.W))")
  // //         emit ("")
  // //         emit ("// GPO1 Write (Avalon imapped master), write_n")
  // // //	      emit ("val gpo2_streamout_chipselect = Output(UInt(1.W))")
  // // //	      emit ("val gpo2_streamout_address    = Output(UInt(4.W))")
  // // 	      emit ("val gpo2_streamout_writedata  = Output(UInt(32.W))")
  // // //	      emit ("val gpo2_streamout_write      = Output(UInt(1.W))")
  // //         emit ("")
  //       // close("})")
  //       // emit ("var outArgMuxMap: scala.collection.mutable.Map[Int, Int] = scala.collection.mutable.Map[Int,Int]()")
  //       // emit ("(0 until io_numArgOuts).foreach{ i => outArgMuxMap += (i -> 0) }")
  //       // open("def getArgOutLane(id: Int): Int = {")
  //       //   emit ("val lane = outArgMuxMap(id)")
  //       //   emit ("outArgMuxMap += (id -> {lane + 1})")
  //       //   emit ("lane")
  //       // close("}")
  //       emit ("var outStreamMuxMap: scala.collection.mutable.Map[String, Int] = scala.collection.mutable.Map[String,Int]()")
  //       open("def getStreamOutLane(id: String): Int = {")
  //         emit ("val lane = outStreamMuxMap.getOrElse(id, 0)")
  //         emit ("outStreamMuxMap += (id -> {lane + 1})")
  //         emit ("lane")
  //       close("}")
  //       emit ("var outBuffMuxMap: scala.collection.mutable.Map[String, Int] = scala.collection.mutable.Map[String,Int]()")
  //       open("def getBuffOutLane(id: String): Int = {")
  //         emit ("val lane = outBuffMuxMap.getOrElse(id, 0)")
  //         emit ("outBuffMuxMap += (id -> {lane + 1})")
  //         emit ("lane")
  //       close("}")
  //       emit ("var inStreamMuxMap: scala.collection.mutable.Map[String, Int] = scala.collection.mutable.Map[String,Int]()")
  //       open("def getStreamInLane(id: String): Int = {")
  //         emit ("val lane = inStreamMuxMap.getOrElse(id, 0)")
  //         emit ("inStreamMuxMap += (id -> {lane + 1})")
  //         emit ("lane")
  //       close("}")
  //       // emit ("var loadMuxMap: scala.collection.mutable.Map[Int, Int] = scala.collection.mutable.Map[Int,Int]()")
  //       // emit ("(0 until io_streamInsInfo.length).foreach{ i => loadMuxMap += (i -> 0) }")
  //       // open("def getLoadLane(id: Int): Int = {")
  //       //   emit ("val lane = loadMuxMap(id)")
  //       //   emit ("loadMuxMap += (id -> {lane + 1})")
  //       //   emit ("lane")
  //       // close("}")
  //       // emit ("var storeMuxMap: scala.collection.mutable.Map[Int, Int] = scala.collection.mutable.Map[Int,Int]()")
  //       // emit ("(0 until io_streamOutsInfo.length).foreach{ i => storeMuxMap += (i -> 0) }")
  //       // open("def getStoreLane(id: Int): Int = {")
  //       //   emit ("val lane = storeMuxMap(id)")
  //       //   emit ("storeMuxMap += (id -> {lane + 1})")
  //       //   emit ("lane")
  //       // close("}")
  //       close("}")
  //     }

  //     inGennAll(out, "RootController", ext){ close("}") }
  //     inGennAll(out, "BufferControlCxns", ext){ close("}") }
  //     inGennAll(out, "GlobalModules", ext){ close("}") }
  //     inGennAll(out, "GlobalWires", ext){ close("}") }
  //     inGennAll(out, "Instrumentation", ext){ close("}") }

  //     inGen(out, s"GlobalWiresMixer.$ext") {
  //       emit(s"package accel")
  //       emit("import chisel3._")
  //       emit("import chisel3.util._")
  //       val traits = List.tabulate(streamExtensions("GlobalWires")){i => "GlobalWires_" + {i+1}}
  //       emit(s"trait GlobalWiresMixer extends ${traits.mkString("\n with ")}")
  //     }

  //     inGen(out, s"GlobalModulesMixer.$ext") {
  //       emit(s"package accel")
  //       emit("import chisel3._")
  //       emit("import chisel3.util._")
  //       val traits = List.tabulate(streamExtensions("GlobalModules")){i => "GlobalModules_" + {i+1}}
  //       emit(s"trait GlobalModulesMixer extends ${traits.mkString("\n with ")}")
  //     }

  //     inGen(out, s"InstrumentationMixer.$ext") {
  //       emit(s"package accel")
  //       emit("import chisel3._")
  //       emit("import chisel3.util._")
  //       val traits = List.tabulate(streamExtensions("Instrumentation")){i => "Instrumentation_" + {i+1}}
  //       emit(s"trait InstrumentationMixer extends ${traits.mkString("\n with ")}")
  //     }

  //     if (spatialConfig.compressWires == 2) {
  //       // TODO
  //     } else if (spatialConfig.compressWires == 0 | spatialConfig.compressWires == 1) {
  //       val allTraits = streamExtensions.flatMap{s => List.tabulate(s._2){i => src"${s._1}_${i+1}"}}.toList
  //                                       .filterNot{a => a.contains("Instantiator") | a.contains("AccelTop") | a.contains("Mapping")}

  //       val mixers = (0 until allTraits.length by 50).map { i =>
  //         val numLocalTraits = {allTraits.length - i} min 50
  //         val thisTraits = (0 until numLocalTraits).map { j => allTraits(i+j) }
  //         inGen(out, "Mixer"+{i/50} + "." + ext) {
  //           emit(s"""package accel""")
  //           emit("import chisel3._")
  //           emit("import chisel3.util._")
  //           emit(s"""trait Mixer${i/50} extends ${thisTraits.mkString("\n with ")} {}""")
  //         }
  //         "Mixer" + {i/50}
  //       }

        // inGen(out, "AccelTop.scala") {
        //   emit(s"""package accel""")
        //   emit("import chisel3._")
        //   emit("import chisel3.util._")
        //   emit("import fringe._")
        //   emit("import utils.implicits._")
        //   emit("import fringe.templates.math._")
        //   emit("import fringe.templates.counters._")
        //   emit("import fringe.templates.memory._")
        //   emit("import fringe.templates.retiming._")
        //   open("class AccelTop(")
        //     emit("val top_w: Int,")
        //     emit("val numArgIns: Int,")
        //     emit("val numArgOuts: Int,")
        //     emit("val numArgIOs: Int,")
        //     emit("val numArgInstrs: Int,")
        //     emit("val argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int],")
        //     emit("val numAllocators: Int,")
        //     emit("val loadStreamInfo: List[StreamParInfo],")
        //     emit("val storeStreamInfo: List[StreamParInfo],")
        //     emit("val streamInsInfo: List[StreamParInfo],")
        //     emit("val streamOutsInfo: List[StreamParInfo]")
        //   close(s") extends AbstractAccelTop with ${mixers.mkString("\n with ")} {}")
        // }
  //     } else {
  // //     // Get traits that need to be mixed in
  // //       val traits = streamMapReverse.keySet.toSet.map{
  // //         f:String => f.split('.').dropRight(1).mkString(".")  /*strip extension */
  // //       }.toSet - "AccelTop" - "Instantiator" - "Mapping"
  // //       withStream(getStream("AccelTop")) {
  // //         emit(s"""package accel
  // // import chisel3._
  // // import chisel3.util._

  // // class AccelTop(
  // //   val top_w: Int,
  // //   val numArgIns: Int,
  // //   val numArgOuts: Int,
  // //   val numArgIOs: Int,
  // //   val numArgInstrs: Int,
  // //   val argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int],
  // //   val loadStreamInfo: List[StreamParInfo],
  // //   val storeStreamInfo: List[StreamParInfo],
  // //   val numStreamIns: List[StreamParInfo],
  // //   val numStreamOuts: List[StreamParInfo]
  // // ) extends ${traits.mkString("\n with ")} {

  // // }
  // //   // AccelTop class mixes in all the other traits and is instantiated by tester""")
  // //       }

  //     }

  //   }
  //   super.emitFooter()
  // }

}
