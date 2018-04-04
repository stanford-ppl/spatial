package spatial.codegen.chiselgen

import argon.codegen.FileGen
import argon.core._

trait ChiselFileGen extends FileGen {

  override protected def emitMain[S:Type](b: Block[S]): Unit = emitBlock(b)

  override protected def process[S:Type](b: Block[S]): Block[S] = {
    // // Forcefully create the following streams
    // val baseSQtream = getStream("GlobalWires")
    // val ioModule = getStream("IOModule")
    // val AccelTop = getStream("AccelTop")
    // val bufferControl = getStream("BufferControlCxns")
    // val RootController = getStream("RootController")

    withStream(getStream("RootController")) {
      if (config.emitDevel > 0) { Console.println(s"[ ${lang}gen ] Begin!")}
      preprocess(b)
      toggleEn() // Turn off
      emitMain(b)
      toggleEn() // Turn on
      postprocess(b)
      if (config.emitDevel > 0) { Console.println(s"[ ${lang}gen ] Complete!")}
      b
    }
  }


  override protected def emitFileHeader() {

    withStream(getStream("IOModule")) {
      emit(s"""package accel
import chisel3._
import templates._
import templates.ops._
import chisel3.util._
import fringe._
import types._
""")
      open("trait IOModule extends Module {")
      emit("""val io_w = if (FringeGlobals.target == "vcs" || FringeGlobals.target == "asic") 8 else 32 // TODO: How to generate these properly?""")
      emit("""val io_v = if (FringeGlobals.target == "vcs" || FringeGlobals.target == "asic") 64 else 16 // TODO: How to generate these properly?""")
    }

    withStream(getStream("BufferControlCxns")) {
      emit(s"""package accel
import templates._
import templates.ops._
import fringe._
import chisel3._
""")
      open(s"""trait BufferControlCxns extends RootController {""")
    }


    withStream(getStream("RootController")) {
      emit(s"""package accel
import templates._
import templates.ops._
import fringe._
import types._
import chisel3._
import chisel3.util._
""")
      open(s"trait RootController extends InstrumentationMixer {")

    }

    withStream(getStream("GlobalWires")) {
      emit(s"""package accel
import templates._
import templates.ops._
import chisel3._
import chisel3.util._
import types._
""")
      open(s"""trait GlobalWires extends IOModule{""")
    }

    withStream(getStream("GlobalModules")) {
      emit(s"""package accel
import templates._
import templates.ops._
import chisel3._
import chisel3.util._
import types._
""")
      open(s"""trait GlobalModules extends GlobalWiresMixer {""")
    }

    withStream(getStream("Instrumentation")) {
      emit(s"""package accel
import templates._
import templates.ops._
import chisel3._
import chisel3.util._
import types._
""")
      open(s"""trait Instrumentation extends GlobalModulesMixer {""")
    }

//     withStream(getStream("GlobalRetiming")) {
//       emit(s"""package accel
// import templates._
// import templates.ops._
// import chisel3._
// import types._""")
//       open(s"""trait GlobalRetiming extends GlobalWiresMixer {""")
//     }


    withStream(getStream("Instantiator")) {
      emit("// See LICENSE for license details.")
      emit("")
      emit("package top")
      emit("")
      emit("import fringe._")
      emit("import accel._")
      emit("import chisel3.core.Module")
      emit("import chisel3._")
      emit("import chisel3.util._")
      emit("import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}")
      emit("")
      emit("import scala.collection.mutable.ListBuffer")

      emit("/**")
      emit(" * Top test harness")
      emit(" */")
      open("class TopUnitTester(c: Top)(implicit args: Array[String]) extends ArgsTester(c) {")
      close("}")
      emit("")
      open("object Instantiator extends CommonMain {")
        emit("type DUTType = Top")
        emit("")
        open("def dut = () => {")

    }


    super.emitFileHeader()
  }

  override protected def emitFileFooter() {
    // emitBufferControlCxns()

    withStream(getStream("Instantiator")) {
          emit("""val w = if (target == "zcu") 32 else if (target == "vcs" || target == "asic") 8 else 32""")
          emit("val numArgIns = numArgIns_mem  + numArgIns_reg + numArgIOs_reg")
          emit("val numArgOuts = numArgOuts_reg + numArgIOs_reg + numArgOuts_instr + numArgOuts_breakpts")
          emit("val numArgIOs = numArgIOs_reg")
          emit("val numArgInstrs = numArgOuts_instr")
          emit("val numArgBreakpts = numArgOuts_breakpts")
          emit("new Top(w, numArgIns, numArgOuts, numArgIOs, numArgOuts_instr + numArgBreakpts, io_argOutLoopbacksMap, loadStreamInfo, storeStreamInfo, streamInsInfo, streamOutsInfo, target)")
        close("}")
        emit("def tester = { c: DUTType => new TopUnitTester(c) }")
      close("}")

    }
    // for (i <- 0 until numGlobalFiles) {
    //   withStream(getStream("GlobalWires"+ i)) {
    //   // // Get each all unique reg strings
    //   // emitted_argins.toList.map{a=>a._2}.distinct.foreach{ a =>
    //   //   emit(s"""val ${a} = io.ArgIn.ports(${argInsByName.indexOf(a)})""")
    //   // }

    //   // emitted_argins.toList.foreach {
    //   //   case (sym, regStr) =>
    //   //     emit(s"""val ${quote(sym)} = $regStr""")
    //   // }
    //     emit("}")
    //   }
    // }

    withStream(getStream("IOModule")) {
      emit("// Combine values")
      emit("val io_numArgIns = math.max(1, io_numArgIns_reg + io_numArgIns_mem + io_numArgIOs_reg)")
      emit("val io_numArgOuts = math.max(1, io_numArgOuts_reg + io_numArgIOs_reg + io_numArgOuts_instr + io_numArgOuts_breakpts)")
      emit("val io_numArgOutLoopbacks = math.max(1, io_argOutLoopbacksMap.toList.length)")
      emit("val io_numArgIOs = io_numArgIOs_reg")
      emit("val io_numArgInstrs = io_numArgOuts_instr")
      emit("val io_numArgBreakpts = io_numArgOuts_breakpts")
      open("val io = IO(new Bundle {")
        emit("// Control IO")
        emit("val enable = Input(Bool())")
        emit("val done = Output(Bool())")
        emit("val reset = Input(Bool())")
        emit("")
        emit("// DRAM IO")
        emit("val memStreams = Flipped(new AppStreams(")
        emit("  io_loadStreamInfo ++ (if (io_loadStreamInfo.size == 0) List(StreamParInfo(io_w, io_v, 0, false)) else List[StreamParInfo]()),")
        emit("  io_storeStreamInfo ++ (if (io_storeStreamInfo.size == 0) List(StreamParInfo(io_w, io_v, 0, false)) else List[StreamParInfo]()))")
        emit(")")
        emit("")
        emit("// Scalar IO")
        emit("val argIns = Input(Vec(io_numArgIns, UInt(64.W)))")
        emit("val argOuts = Vec(io_numArgOuts, Decoupled((UInt(64.W))))")
        emit("val argOutLoopbacks = Input(Vec(io_numArgOutLoopbacks, UInt(64.W)))")
        emit("")
        emit("// Stream IO")
        emit("// val genericStreams = new GenericStreams(io_streamInsInfo, io_streamOutsInfo)")
        emit("// Video Stream Inputs ")
        emit("val stream_in_data            = Input(UInt(16.W))")
        emit("val stream_in_startofpacket   = Input(Bool())")
        emit("val stream_in_endofpacket     = Input(Bool())")
        emit("val stream_in_empty           = Input(UInt(2.W))")
        emit("val stream_in_valid           = Input(Bool()) ")
        emit("val stream_out_ready          = Input(Bool())")
        emit(" ")
        emit("// Video Stream Outputs")
        emit("val stream_in_ready           = Output(Bool())")
        emit("val stream_out_data           = Output(UInt(16.W))")
        emit("val stream_out_startofpacket  = Output(Bool())")
        emit("val stream_out_endofpacket    = Output(Bool())")
        emit("val stream_out_empty          = Output(UInt(1.W))")
        emit("val stream_out_valid          = Output(Bool())")
        emit("")
        emit("// LED Stream Outputs ")
        emit("val led_stream_out_data       = Output(UInt(32.W))")
        emit("")
        emit("// Slider Switches Stream Inputs ")
        emit("val switch_stream_in_data     = Input(UInt(32.W))")
        emit("")
        emit("// BufferedOut Outputs (Avalon mmapped master)")
        emit("val buffout_address           = Output(UInt(32.W))")
        emit("val buffout_write             = Output(UInt(1.W))")
        emit("val buffout_writedata         = Output(UInt(16.W))")
        emit("val buffout_waitrequest       = Input(UInt(1.W))")
        emit("")
        emit("// GPI1 Read (Avalon mmapped master)")
//	      emit("val gpi1_streamin_chipselect  = Output(UInt(1.W))")
	      emit("val gpi1_streamin_readdata    = Input(UInt(32.W))")
//	      emit("val gpi1_streamin_address     = Output(UInt(4.W))")
//	      emit("val gpi1_streamin_read        = Output(UInt(1.W))")
        emit("")
        emit("// GPO1 Write (Avalon mmapped master), write_n")
//	      emit("val gpo1_streamout_chipselect = Output(UInt(1.W))")
//	      emit("val gpo1_streamout_address    = Output(UInt(4.W))")
	      emit("val gpo1_streamout_writedata  = Output(UInt(32.W))")
//	      emit("val gpo1_streamout_write      = Output(UInt(1.W))")
        emit("")
        emit("// GPI2 Read (Avalon mmapped master)")
//	      emit("val gpi2_streamin_chipselect  = Output(UInt(1.W))")
	      emit("val gpi2_streamin_readdata    = Input(UInt(32.W))")
//	      emit("val gpi2_streamin_address     = Output(UInt(4.W))")
//	      emit("val gpi2_streamin_read        = Output(UInt(1.W))")
        emit("")
        emit("// GPO1 Write (Avalon imapped master), write_n")
//	      emit("val gpo2_streamout_chipselect = Output(UInt(1.W))")
//	      emit("val gpo2_streamout_address    = Output(UInt(4.W))")
	      emit("val gpo2_streamout_writedata  = Output(UInt(32.W))")
//	      emit("val gpo2_streamout_write      = Output(UInt(1.W))")
        emit("")
      close("})")
      emit("var outArgMuxMap: scala.collection.mutable.Map[Int, Int] = scala.collection.mutable.Map[Int,Int]()")
      emit("(0 until io_numArgOuts).foreach{ i => outArgMuxMap += (i -> 0) }")
      open("def getArgOutLane(id: Int): Int = {")
        emit("val lane = outArgMuxMap(id)")
        emit("outArgMuxMap += (id -> {lane + 1})")
        emit("lane")
      close("}")
      emit("var outStreamMuxMap: scala.collection.mutable.Map[String, Int] = scala.collection.mutable.Map[String,Int]()")
      open("def getStreamOutLane(id: String): Int = {")
        emit("val lane = outStreamMuxMap.getOrElse(id, 0)")
        emit("outStreamMuxMap += (id -> {lane + 1})")
        emit("lane")
      close("}")
      emit("var outBuffMuxMap: scala.collection.mutable.Map[String, Int] = scala.collection.mutable.Map[String,Int]()")
      open("def getBuffOutLane(id: String): Int = {")
        emit("val lane = outBuffMuxMap.getOrElse(id, 0)")
        emit("outBuffMuxMap += (id -> {lane + 1})")
        emit("lane")
      close("}")
      emit("var inStreamMuxMap: scala.collection.mutable.Map[String, Int] = scala.collection.mutable.Map[String,Int]()")
      open("def getStreamInLane(id: String): Int = {")
        emit("val lane = inStreamMuxMap.getOrElse(id, 0)")
        emit("inStreamMuxMap += (id -> {lane + 1})")
        emit("lane")
      close("}")
      // emit("var loadMuxMap: scala.collection.mutable.Map[Int, Int] = scala.collection.mutable.Map[Int,Int]()")
      // emit("(0 until io_streamInsInfo.length).foreach{ i => loadMuxMap += (i -> 0) }")
      // open("def getLoadLane(id: Int): Int = {")
      //   emit("val lane = loadMuxMap(id)")
      //   emit("loadMuxMap += (id -> {lane + 1})")
      //   emit("lane")
      // close("}")
      // emit("var storeMuxMap: scala.collection.mutable.Map[Int, Int] = scala.collection.mutable.Map[Int,Int]()")
      // emit("(0 until io_streamOutsInfo.length).foreach{ i => storeMuxMap += (i -> 0) }")
      // open("def getStoreLane(id: Int): Int = {")
      //   emit("val lane = storeMuxMap(id)")
      //   emit("storeMuxMap += (id -> {lane + 1})")
      //   emit("lane")
      // close("}")
      close("}")
    }

    streamExtensions("RootController").foreach{i =>
      val fname = if (i == 0) "RootController" else src"RootController_${i}"
      withStream(getStream(fname)) { stream.println("}")}
    }

    streamExtensions("BufferControlCxns").foreach{i =>
      val fname = if (i == 0) "BufferControlCxns" else src"BufferControlCxns_${i}"
      withStream(getStream(fname)) { stream.println("}")}
    }

    val gms = streamExtensions("GlobalModules").map{i =>
      val fname = if (i == 0) "GlobalModules" else src"GlobalModules_${i}"
      withStream(getStream(fname)) { stream.println("}")}
      fname
    }

    val instruments = streamExtensions("Instrumentation").map{i =>
      val fname = if (i == 0) "Instrumentation" else src"Instrumentation_${i}"
      withStream(getStream(fname)) { stream.println("}")}
      fname
    }

    val gws = streamExtensions("GlobalWires").map{i =>
      val fname = if (i == 0) "GlobalWires" else src"GlobalWires_${i}"
      withStream(getStream(fname)) { stream.println("}")}
      fname
    }

    withStream(getStream("GlobalWiresMixer")) {
        emit(s"""package accel
import templates._
import fringe._
import chisel3._
import chisel3.util._
trait GlobalWiresMixer extends ${gws.mkString("\n with ")}""")
      }

    withStream(getStream("GlobalModulesMixer")) {
        emit(s"""package accel
import templates._
import fringe._
import chisel3._
import chisel3.util._
trait GlobalModulesMixer extends ${gms.mkString("\n with ")}""")
      }

    withStream(getStream("InstrumentationMixer")) {
        emit(s"""package accel
import templates._
import fringe._
import chisel3._
import chisel3.util._
trait InstrumentationMixer extends ${instruments.mkString("\n with ")}""")
      }

    // withStream(getStream("GlobalRetiming")) {
    //   close("}")
    // }

    if (config.multifile == 6) {
      var methodList = scala.collection.mutable.ListBuffer[String]()
      val methodized_trait_pattern = "^x[0-9]+".r
      val traits = (streamMapReverse.keySet.toSet.map{
        f:String => f.split('.').dropRight(1).mkString(".")  /*strip extension */
      }.toSet - "AccelTop" - "Instantiator" - "Mapping").toList

      var numMixers = 0
      (0 until traits.length by numTraitsPerMixer).foreach { i =>
        val numLocalTraits = {traits.length - i} min numTraitsPerMixer
        val thisTraits = (0 until numLocalTraits).map { j =>
          if (methodized_trait_pattern.findFirstIn(traits(i+j)).isDefined) {methodList += traits(i+j)}
          traits(i+j)
        }
        withStream(getStream("Mixer"+numMixers)) {
          emit(s"""package accel
import templates._
import fringe._
import chisel3._
import chisel3.util._""")
          emit(s"""trait Mixer$numMixers extends ${thisTraits.mkString("\n with ")} {}""")

        }
        numMixers = numMixers + 1
      }

      withStream(getStream("AccelTop")) {
        emit(s"""package accel
import templates._
import fringe._
import chisel3._
import chisel3.util._
class AccelTop(
  val top_w: Int,
  val numArgIns: Int,
  val numArgOuts: Int,
  val numArgIOs: Int,
  val numArgInstrs: Int,
  val argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int],
  val loadStreamInfo: List[StreamParInfo],
  val storeStreamInfo: List[StreamParInfo],
  val streamInsInfo: List[StreamParInfo],
  val streamOutsInfo: List[StreamParInfo]
) extends ${(0 until numMixers).map{i => "Mixer" + i}.mkString("\n with ")} {
${methodList.map{i => src"method_${i}()"}.mkString("\n")}
  // TODO: Figure out better way to pass constructor args to IOModule.  Currently just recreate args inside IOModule redundantly
}""")
      }
    } else if (config.multifile == 3 | config.multifile == 4) {
      val traits = (streamMapReverse.keySet.toSet.map{
        f:String => f.split('.').dropRight(1).mkString(".")  /*strip extension */
      }.toSet - "AccelTop" - "Instantiator" - "Mapping").toList

      var numMixers = 0
      (0 until traits.length by numTraitsPerMixer).foreach { i =>
        val numLocalTraits = {traits.length - i} min numTraitsPerMixer
        val thisTraits = (0 until numLocalTraits).map { j => traits(i+j) }
        withStream(getStream("Mixer"+numMixers)) {
          emit(s"""package accel
import templates._
import fringe._
import chisel3._
import chisel3.util._""")
          emit(s"""trait Mixer$numMixers extends ${thisTraits.mkString("\n with ")} {}""")

        }
        numMixers = numMixers + 1
      }

      withStream(getStream("AccelTop")) {
        emit(s"""package accel
import templates._
import fringe._
import chisel3._
import chisel3.util._
class AccelTop(
  val top_w: Int,
  val numArgIns: Int,
  val numArgOuts: Int,
  val numArgIOs: Int,
  val numArgInstrs: Int,
  val argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int],
  val loadStreamInfo: List[StreamParInfo],
  val storeStreamInfo: List[StreamParInfo],
  val streamInsInfo: List[StreamParInfo],
  val streamOutsInfo: List[StreamParInfo]
) extends ${(0 until numMixers).map{i => "Mixer" + i}.mkString("\n with ")} {

  // TODO: Figure out better way to pass constructor args to IOModule.  Currently just recreate args inside IOModule redundantly

}""")
      }
    } else {
    // Get traits that need to be mixed in
      val traits = streamMapReverse.keySet.toSet.map{
        f:String => f.split('.').dropRight(1).mkString(".")  /*strip extension */
      }.toSet - "AccelTop" - "Instantiator" - "Mapping"
      withStream(getStream("AccelTop")) {
        emit(s"""package accel
import templates._
import fringe._
import chisel3._
import chisel3.util._

class AccelTop(
  val top_w: Int,
  val numArgIns: Int,
  val numArgOuts: Int,
  val numArgIOs: Int,
  val numArgInstrs: Int,
  val argOutLoopbacksMap: scala.collection.immutable.Map[Int,Int],
  val loadStreamInfo: List[StreamParInfo],
  val storeStreamInfo: List[StreamParInfo],
  val numStreamIns: List[StreamParInfo],
  val numStreamOuts: List[StreamParInfo]
) extends ${traits.mkString("\n with ")} {

}
  // AccelTop class mixes in all the other traits and is instantiated by tester""")
      }

    }


    super.emitFileFooter()
  }

}
