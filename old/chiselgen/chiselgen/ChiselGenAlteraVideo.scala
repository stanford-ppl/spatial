package spatial.codegen.chiselgen

import argon.codegen.FileDependencies
import argon.core._

import spatial.nodes._

trait ChiselGenAlteraVideo extends ChiselCodegen with FileDependencies {

  var dmas: List[Exp[Any]] = List()
  var decoders: List[Exp[Any]] = List()

  // override protected def bitWidth(tp: Type[_]): Int = tp match {
  //     case Avalon  => 32
  //     case _ => super.bitWidth(tp)
  // }

//  dependencies ::= FileDep("chiselgen", "altera-goodies/address_map_arm.h")
//  dependencies ::= FileDep("chiselgen", "altera-goodies/interfaces.v")
//  dependencies ::= FileDep("chiselgen", "altera-goodies/StreamPixBuffer2ARM.scala")
//  dependencies ::= FileDep("chiselgen", "altera-goodies/altera_up_avalon_video_decoder")
//  dependencies ::= FileDep("chiselgen", "altera-goodies/altera_up_avalon_video_dma_controller")


  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case AxiMSNew() =>

    case DecoderTemplateNew(popFrom, pushTo) => 
      dmas = dmas :+ lhs
      emit(src"${pushTo}.io.in := io.video_decoder_ios.stream_out_data(0)")
      emit(src"${pushTo}.io.push := io.video_decoder_ios.stream_out_valid(0)")

    case DMATemplateNew(popFrom, loadIn) => 
      decoders = decoders :+ lhs
      emit(src"io.dma_tomem_ios.stream_data(0) := ${popFrom}.io.out")
      emit(src"${popFrom}.io.pop := io.dma_tomem_ios.stream_valid(0)")
    case _ => super.gen(lhs, rhs)
  }

  override protected def emitFileFooter() {
    // withStream(getStream("IOModule")) {

    //   emit("// Bundles for altera video decoder/dma bus")
    //   open(s"""class DMA_tomem_io() extends Bundle{""")

    //   emit(s"val DW          =  15; // Frame's datawidth")
    //   emit(s"val EW          =   0; // Frame's empty width")
    //   emit(s"val WIDTH       = 640; // Frame's width in pixels")
    //   emit(s"val HEIGHT        = 480; // Frame's height in lines")
    //   emit(s"val AW          =  17; // Frame's address width")
    //   emit(s"val WW          =   8; // Frame width's address width")
    //   emit(s"val HW          =   7; // Frame height's address width")
    //   emit(s"val MDW         =  15; // Avalon master's datawidth")
    //   emit(s"val DEFAULT_BUFFER_ADDRESS    = 32 //32'h00000000;")
    //   emit(s"val DEFAULT_BACK_BUF_ADDRESS          = 32//32'h00000000;")
    //   emit(s"val ADDRESSING_BITS     = 16//16'h0809;")
    //   emit(s"val COLOR_BITS        = 4//4'h7;")
    //   emit(s"val COLOR_PLANES      = 2//2'h2;")
    //   emit(s"val DEFAULT_DMA_ENABLED     = 1//1'b1; // 0: OFF or 1: ON")
    //   emit(s"val reset = Vec(0, Input(UInt(1.W))) // Probably based on wrong length")
    //   emit(s"val stream_data = Vec(0, Input(UInt((DW+1).W))) // Probably based on wrong length")
    //   emit(s"val stream_startofpacket = Vec(0, Input(UInt(1.W))) // Probably based on wrong length")
    //   emit(s"val stream_endofpacket = Vec(0, Input(UInt(1.W))) // Probably based on wrong length")
    //   emit(s"val stream_empty = Vec(0, Input(UInt((EW + 1).W))) // Probably based on wrong length")
    //   emit(s"val stream_valid = Vec(0, Input(UInt(1.W))) // Probably based on wrong length")
    //   emit(s"val master_waitrequest = Vec(0, Input(UInt(1.W))) // Probably based on wrong length")
    //   emit(s"val slave_address = Vec(0, Input(UInt(2.W))) // Probably based on wrong length")
    //   emit(s"val slave_byteenable = Vec(0, Input(UInt(4.W))) // Probably based on wrong length")
    //   emit(s"val slave_read = Vec(0, Input(UInt(1.W))) // Probably based on wrong length")
    //   emit(s"val slave_write = Vec(0, Input(UInt(1.W))) // Probably based on wrong length")
    //   emit(s"val slave_writedata = Vec(0, Input(UInt(32.W))) // Probably based on wrong length")
    //   emit(s"val stream_ready = Vec(0, Output(UInt(1.W))) // Probably based on wrong length")
    //   emit(s"val master_address = Vec(0, Output(UInt(32.W))) // Probably based on wrong length")
    //   emit(s"val master_write = Vec(0, Output(UInt(1.W))) // Probably based on wrong length")
    //   emit(s"val master_writedata = Vec(0, Output(UInt((MDW + 1).W))) // Probably based on wrong length")
    //   emit(s"val slave_readdata = Vec(0, Output(UInt(32.W))) // Probably based on wrong length")
    //   close("}")
    //   open(s"""class DMA_frommem_io() extends Bundle{""")


    //   emit(s"val DW          =  15; // Frame's datawidth")
    //   emit(s"val EW          =   0; // Frame's empty width")
    //   emit(s"val WIDTH       = 640; // Frame's width in pixels")
    //   emit(s"val HEIGHT        = 480; // Frame's height in lines")
    //   emit(s"val AW          =  17; // Frame's address width")
    //   emit(s"val WW          =   8; // Frame width's address width")
    //   emit(s"val HW          =   7; // Frame height's address width")
    //   emit(s"val MDW         =  15; // Avalon master's datawidth")
    //   emit(s"val DEFAULT_BUFFER_ADDRESS    = 32 //32'h00000000;")
    //   emit(s"val DEFAULT_BACK_BUF_ADDRESS          = 32//32'h00000000;")
    //   emit(s"val ADDRESSING_BITS     = 16//16'h0809;")
    //   emit(s"val COLOR_BITS        = 4//4'h7;")
    //   emit(s"val COLOR_PLANES      = 2//2'h2;")
    //   emit(s"val DEFAULT_DMA_ENABLED     = 1//1'b1; // 0: OFF or 1: ON")
    //   emit(s"val reset = Vec(0,  Input(UInt(1.W)))")
    //   emit(s"val stream_ready = Vec(0,  Input(UInt(1.W)))")
    //   emit(s"val master_readdata = Vec(0,  Input(UInt((MDW + 1).W)))")
    //   emit(s"val master_readdatavalid = Vec(0,  Input(UInt((MDW + 1).W)))")
    //   emit(s"val master_waitrequest = Vec(0,  Input(UInt(1.W)))")
    //   emit(s"val slave_address = Vec(0,  Input(UInt(2.W)))")
    //   emit(s"val slave_byteenable = Vec(0,  Input(UInt(4.W)))")
    //   emit(s"val slave_read = Vec(0,  Input(UInt(1.W)))")
    //   emit(s"val slave_write = Vec(0,  Input(UInt(1.W)))")
    //   emit(s"val slave_writedata = Vec(0,  Input(UInt(32.W)))")
    //   emit(s"val stream_data = Vec(0,  Output(UInt((DW + 1).W)))")
    //   emit(s"val stream_startofpacket = Vec(0,  Output(UInt(1.W)))")
    //   emit(s"val stream_endofpacket = Vec(0,  Output(UInt(1.W)))")
    //   emit(s"val stream_empty = Vec(0,  Output(UInt((EW + 1).W)))")
    //   emit(s"val stream_valid = Vec(0,  Output(UInt(1.W)))")
    //   emit(s"val master_address = Vec(0,  Output(UInt(32.W)))")
    //   emit(s"val master_arbiterlock = Vec(0,  Output(UInt(1.W)))")
    //   emit(s"val master_read = Vec(0,  Output(UInt(1.W)))")
    //   emit(s"val slave_readdata = Vec(0,  Output(UInt(32.W)))")
    //   close("}")

    //   open(s"""class  Video_Decoder_io() extends Bundle{""")

    //   emit(s"val IW    = 9; ")
    //   emit(s"val OW    = 15;")
    //   emit(s"val FW    = 17;")
    //   emit(s"val PIXELS  = 1280;")
    //   emit(s"val reset = Vec(0, Input(UInt(1.W)))")
    //   emit(s"val TD_CLK27 = Vec(0, Input(UInt(1.W)))")
    //   emit(s"val TD_DATA = Vec(0, Input(UInt(8.W)))")
    //   emit(s"val TD_HS = Vec(0, Input(UInt(1.W)))")
    //   emit(s"val TD_VS = Vec(0, Input(UInt(1.W)))")
    //   emit(s"val clk27_reset = Vec(0, Input(UInt(1.W)))")
    //   emit(s"val stream_out_ready = Vec(0, Input(UInt(1.W)))")
    //   emit(s"val TD_RESET = Vec(0, Output(UInt(1.W)))")
    //   emit(s"val overflow_flag = Vec(0, Output(UInt(1.W)))")
    //   emit(s"val stream_out_data = Vec(0, Output(UInt((OW + 1).W)))")
    //   emit(s"val stream_out_startofpacket = Vec(0, Output(UInt(1.W)))")
    //   emit(s"val stream_out_endofpacket = Vec(0, Output(UInt(1.W)))")
    //   emit(s"val stream_out_empty = Vec(0, Output(UInt(1.W)))")
    //   emit(s"val stream_out_valid = Vec(0, Output(UInt(1.W)))")
    //   close("}")
    // }

    super.emitFileFooter()

  }
}
