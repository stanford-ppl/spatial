package fringe.targets.zynq

import chisel3._
import fringe._
import fringe.templates.math._
import fringe.utils.implicits._

import scala.collection.mutable.Set

trait ZynqBlackBoxes {

  // To avoid creating the same IP twice
  val createdIP = Set[String]()

  class Divider(val dividendWidth: Int, val divisorWidth: Int, val signed: Boolean, val latency: Int) extends Module {
    val io = IO(new Bundle {
      val dividend = Input(UInt(dividendWidth.W))
      val divisor = Input(UInt(divisorWidth.W))
      val flow = Input(Bool())
      val out = Output(UInt(dividendWidth.W))
    })

    val fractionBits = 2

    val m = Module(new DivModBBox(dividendWidth, divisorWidth, signed, false, fractionBits, latency))
    m.io.aclk := clock
    m.io.s_axis_dividend_tvalid := true.B
    m.io.s_axis_dividend_tdata := io.dividend
    m.io.s_axis_divisor_tvalid := true.B
    m.io.s_axis_divisor_tdata := io.divisor
    m.io.aclken := io.flow
    io.out := m.io.m_axis_dout_tdata(dividendWidth-1, fractionBits)
  }

  class Modulo(val dividendWidth: Int, val divisorWidth: Int, val signed: Boolean, val latency: Int) extends Module {
    val io = IO(new Bundle {
      val dividend = Input(UInt(dividendWidth.W))
      val divisor = Input(UInt(divisorWidth.W))
      val flow = Input(Bool())
      val out = Output(UInt(dividendWidth.W))
    })

    val fractionBits = dividendWidth

    val m = Module(new DivModBBox(dividendWidth, divisorWidth, signed, true, fractionBits, latency))
    m.io.aclk := clock
    m.io.s_axis_dividend_tvalid := true.B
    m.io.s_axis_dividend_tdata := io.dividend
    m.io.s_axis_divisor_tvalid := true.B
    m.io.s_axis_divisor_tdata := io.divisor
    m.io.aclken := io.flow
    io.out := m.io.m_axis_dout_tdata
  }

  class DivModBBox(val dividendWidth: Int, val divisorWidth: Int, val signed: Boolean, val isMod: Boolean, val fractionBits: Int, val latency: Int) extends BlackBox {
    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val aclken = Input(Bool())
      val s_axis_dividend_tvalid = Input(Bool())
      val s_axis_dividend_tdata = Input(UInt(dividendWidth.W))
      val s_axis_divisor_tvalid = Input(Bool())
      val s_axis_divisor_tdata = Input(UInt(divisorWidth.W))
      val m_axis_dout_tvalid = Output(Bool())
      val m_axis_dout_tdata = Output(UInt(dividendWidth.W))
    })

    val signedString = if (signed) "Signed" else "Unsigned"
    val modString = if (isMod) "Remainder" else "Fractional"
    val moduleName = s"div_${dividendWidth}_${divisorWidth}_${latency}_${signedString}_${modString}"
    override def desiredName = s"div_${dividendWidth}_${divisorWidth}_${latency}_${signedString}_${modString}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## Integer Divider
create_ip -name div_gen -vendor xilinx.com -library ip -version 5.1 -module_name $moduleName
set_property -dict [list CONFIG.latency_configuration {Manual} CONFIG.latency {$latency} CONFIG.aclken {true}] [get_ips $moduleName]
set_property -dict [list CONFIG.dividend_and_quotient_width {$dividendWidth} CONFIG.divisor_width {$divisorWidth} CONFIG.remainder_type {$modString} CONFIG.clocks_per_division {1} CONFIG.fractional_width {$fractionBits} CONFIG.operand_sign {$signedString}] [get_ips $moduleName]
set_property -dict [list CONFIG.ACLK_INTF.FREQ_HZ $$CLOCK_FREQ_HZ] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")
      globals.tclScript.flush()
      createdIP += moduleName
    }
  }

  class Multiplier(val aWidth: Int, val bWidth: Int, val outWidth: Int, val signed: Boolean, val latency: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt(aWidth.W))
      val b = Input(UInt(bWidth.W))
      val flow = Input(Bool())
      val out = Output(UInt(outWidth.W))
    })

    assert(latency > 0, "ERROR: Latency must be > 0 to use Multiplier IP")
    val m = Module(new MultiplierBBox(aWidth, bWidth, outWidth, signed, latency))
    m.io.CLK := clock
    m.io.A := io.a
    m.io.B := io.b
    m.io.CE := io.flow
    io.out := m.io.P
  }


  class MultiplierBBox(val aWidth: Int, val bWidth: Int, val outWidth: Int, val signed: Boolean, val latency: Int) extends BlackBox {
    val io = IO(new Bundle {
      val CLK = Input(Clock())
      val CE = Input(Bool())
      val A = Input(UInt(aWidth.W))
      val B = Input(UInt(bWidth.W))
      val P = Output(UInt(outWidth.W))
    })

    // From doc: https://www.xilinx.com/support/documentation/ip_documentation/mult_gen/v12_0/pg108-mult-gen.pdf
    // Use LUT-based mults when width <= 16 bits
    val dspThreshold = 16 // Use DSPs for bit widths >= 16
    val multConstruction = if ((aWidth > dspThreshold) | (bWidth > dspThreshold) | (outWidth > dspThreshold)) {
      "Use_Mults"
    } else {
      "Use_LUTs"
    }

    val signedString = if (signed) "Signed" else "Unsigned"
    val moduleName = s"mul_${aWidth}_${bWidth}_${outWidth}_${latency}_${signedString}_${multConstruction}"
    override def desiredName = s"mul_${aWidth}_${bWidth}_${outWidth}_${latency}_${signedString}_${multConstruction}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## Integer Multiplier
create_ip -name mult_gen -vendor xilinx.com -library ip -version 12.0 -module_name $moduleName
set_property -dict [list CONFIG.MultType {Parallel_Multiplier} CONFIG.PortAType {$signedString}  CONFIG.PortAWidth {$aWidth} CONFIG.PortBType {$signedString} CONFIG.PortBWidth {$bWidth} CONFIG.Multiplier_Construction {$multConstruction} CONFIG.OptGoal {Speed} CONFIG.Use_Custom_Output_Width {true} CONFIG.OutputWidthHigh {$outWidth} CONFIG.PipeStages {$latency} CONFIG.ClockEnable {true}] [get_ips $moduleName]
set_property -dict [list CONFIG.clk_intf.FREQ_HZ $$CLOCK_FREQ_HZ] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush()
      createdIP += moduleName
    }
  }

  class SquareRooter(val width: Int, val signed: Boolean, val latency: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt(width.W))
      val out = Output(UInt(width.W))
    })


    val m = Module(new SqrtBBox(width, signed, latency))
    m.io.aclk := clock
    m.io.s_axis_cartesian_tvalid := true.B
    m.io.s_axis_cartesian_tdata := io.a
    io.out := m.io.m_axis_dout_tdata
  }

  class SqrtBBox(val width: Int, val signed: Boolean, val latency: Int) extends BlackBox {
    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_cartesian_tvalid = Input(Bool())
      val s_axis_cartesian_tdata = Input(UInt(width.W))
      val m_axis_dout_tdata = Output(UInt(width.W))
    })

    val signedString = if (signed) "Signed" else "Unsigned"
    val moduleName = s"sqrt_${width}_${width}_${latency}_${signedString}"
    override def desiredName = s"sqrt_${width}_${width}_${latency}_${signedString}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## Integer SquareRooter
create_ip -name cordic -vendor xilinx.com -library ip -version 6.0 -module_name $moduleName
set_property -dict [list CONFIG.Input_Width.VALUE_SRC USER] [get_bd_cells cordic_0]
set_property -dict [list CONFIG.Functional_Selection {Square_Root} CONFIG.Input_Width {$width} CONFIG.Output_Width {$width} CONFIG.Data_Format {UnsignedFraction} CONFIG.Output_Width {$width} CONFIG.Coarse_Rotation {false}] [get_bd_cells cordic_0]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]
""")

      globals.tclScript.flush()
      createdIP += moduleName
    }
  }


  class FAbs(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val out = Output(UInt((exp+frac).W))
    })

    val m = Module(new FAbsBBox(exp, frac, exp, frac))
    //m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  // fabs
  class FAbsBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      //val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val moduleName = s"Absolute_${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"Absolute_${inExp}_${inFrac}_${outExp}_${outFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## fabs
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Absolute} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {0} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class FExp(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val out = Output(UInt((exp+frac).W))
    })

    val m = Module(new FExpBBox(exp, frac, exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  // fexp: Supports only half, single, double precisions
  // Fixed latency of 20 cycles
  class FExpBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val precisionType = if ((inExp == 5) && (inFrac == 11)) {"Half"} else {"Single"}
    val Latency = if ((inExp == 5) && (inFrac == 11)) {13} else {20}
    val moduleName = s"Exponential_${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"Exponential_${inExp}_${inFrac}_${outExp}_${outFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## fexp
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Exponential} CONFIG.Flow_Control {NonBlocking} CONFIG.A_Precision_Type {$precisionType} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {$precisionType} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Mult_Usage {Medium_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {$Latency} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }


  class FLog(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val out = Output(UInt((exp+frac).W))
    })

    val m = Module(new FLogBBox(exp, frac, exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  // flog: Supports half, single, double
  // Set to max latency 22 cycles
  class FLogBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val moduleName = s"Logarithm_${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"Logarithm_${inExp}_${inFrac}_${outExp}_${outFrac}"
    val precisionType = if ((inExp == 5) && (inFrac == 11)) {"Half"} else {"Single"}
    val Latency = if ((inExp == 5) && (inFrac == 11)) {17} else {22}

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## flog
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Logarithm} CONFIG.Flow_Control {NonBlocking} CONFIG.Maximum_Latency {true} CONFIG.A_Precision_Type {$precisionType} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {$precisionType} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Mult_Usage {Medium_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {$Latency} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }


  class FSqrt(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val out = Output(UInt((exp+frac).W))
    })

    val m = Module(new FSqrtBBox(exp, frac, exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  // fsqrt: Supports custom
  // Set to max latency of 28 cycles
  class FSqrtBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val moduleName = s"Square_root_${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"Square_root_${inExp}_${inFrac}_${outExp}_${outFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## fsqrt
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Square_root} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {28} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class FAdd(val frac: Int, val exp: Int, val latency: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val b = Input(UInt((exp+frac).W))
      val out = Output(UInt((exp+frac).W))
    })

    val m = Module(new FAddBBox(exp, frac, exp, frac, latency))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    m.io.s_axis_b_tdata := io.b
    m.io.s_axis_b_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  // fadd: Supports custom
  // Set to max latency of 12 cycles
  class FAddBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int, val latency: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val s_axis_b_tvalid = Input(Bool())
      val s_axis_b_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val Mult_Usage = if (latency == 12) { "No_Usage" } else { "Full_Usage" }

    val moduleName = s"Add_${inExp}_${inFrac}_${outExp}_${outFrac}_${latency}cyc"
    override def desiredName = s"Add_${inExp}_${inFrac}_${outExp}_${outFrac}_${latency}cyc"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## fadd
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Add_Sub_Value {Add} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Mult_Usage {$Mult_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {$latency}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class FSub(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val b = Input(UInt((exp+frac).W))
      val out = Output(UInt((exp+frac).W))
    })

    val m = Module(new FSubBBox(exp, frac, exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    m.io.s_axis_b_tdata := io.b
    m.io.s_axis_b_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  // fsub: Supports custom
  // Set to max latency of 12 cycles
  class FSubBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val s_axis_b_tvalid = Input(Bool())
      val s_axis_b_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val moduleName = s"Subtract_${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"Subtract_${inExp}_${inFrac}_${outExp}_${outFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## fsub
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Add_Sub_Value {Subtract} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Mult_Usage {No_Usage} CONFIG.C_Latency {12}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class FMul(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val b = Input(UInt((exp+frac).W))
      val out = Output(UInt((exp+frac).W))
    })

    val m = Module(new FMulBBox(exp, frac, exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    m.io.s_axis_b_tdata := io.b
    m.io.s_axis_b_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  // fmul: Supports custom
  // Configured to latency of 8 cycles
  class FMulBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val s_axis_b_tvalid = Input(Bool())
      val s_axis_b_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val moduleName = s"Multiply_${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"Multiply_${inExp}_${inFrac}_${outExp}_${outFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## fmul
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Multiply} CONFIG.A_Precision_Type {Custom} CONFIG.C_Mult_Usage {Full_Usage} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {8} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class FDiv(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val b = Input(UInt((exp+frac).W))
      val out = Output(UInt((exp+frac).W))
    })

    val m = Module(new FDivBBox(exp, frac, exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    m.io.s_axis_b_tdata := io.b
    m.io.s_axis_b_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  // fdiv: Supports custom
  // 28 cycle latency
  class FDivBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val s_axis_b_tvalid = Input(Bool())
      val s_axis_b_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val moduleName = s"Divide_${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"Divide_${inExp}_${inFrac}_${outExp}_${outFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## fdiv
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Divide} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {$inExp} CONFIG.C_Result_Fraction_Width {$inFrac} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {28} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class FLt(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val b = Input(UInt((exp+frac).W))
      val out = Output(Bool())
    })

    val m = Module(new FLtBBox(exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    m.io.s_axis_b_tdata := io.b
    m.io.s_axis_b_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

// flt: supports custom
// 2 cycles
  class FLtBBox(val inExp: Int, val inFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val s_axis_b_tvalid = Input(Bool())
      val s_axis_b_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt(1.W))
    })

    val moduleName = s"CompareLT_${inExp}_${inFrac}"
    override def desiredName = s"CompareLT_${inExp}_${inFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## flt
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Compare} CONFIG.C_Compare_Operation {Less_Than} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {1} CONFIG.C_Result_Fraction_Width {0} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {2} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class FLe(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val b = Input(UInt((exp+frac).W))
      val out = Output(Bool())
    })

    val m = Module(new FLeBBox(exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    m.io.s_axis_b_tdata := io.b
    m.io.s_axis_b_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

// fle: supports custom
// 2 cycles
  class FLeBBox(val inExp: Int, val inFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val s_axis_b_tvalid = Input(Bool())
      val s_axis_b_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt(1.W))
    })

    val moduleName = s"CompareLE_${inExp}_${inFrac}"
    override def desiredName = s"CompareLE_${inExp}_${inFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## fle
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Compare} CONFIG.C_Compare_Operation {Less_Than_Or_Equal} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {1} CONFIG.C_Result_Fraction_Width {0} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {2} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class FEq(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val b = Input(UInt((exp+frac).W))
      val out = Output(Bool())
    })

    val m = Module(new FEqBBox(exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    m.io.s_axis_b_tdata := io.b
    m.io.s_axis_b_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

// feq: supports custom
// 2 cycles
  class FEqBBox(val inExp: Int, val inFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val s_axis_b_tvalid = Input(Bool())
      val s_axis_b_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt(1.W))
    })

    val moduleName = s"Eq_${inExp}_${inFrac}"
    override def desiredName = s"Eq_${inExp}_${inFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## feq
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Compare} CONFIG.C_Compare_Operation {Equal} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {1} CONFIG.C_Result_Fraction_Width {0} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {2} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class FNe(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val b = Input(UInt((exp+frac).W))
      val out = Output(Bool())
    })

    val m = Module(new FNeBBox(exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    m.io.s_axis_b_tdata := io.b
    m.io.s_axis_b_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

// fne: supports custom
// 2 cycles
  class FNeBBox(val inExp: Int, val inFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val s_axis_b_tvalid = Input(Bool())
      val s_axis_b_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt(1.W))
    })

    val moduleName = s"Ne_${inExp}_${inFrac}"
    override def desiredName = s"Ne_${inExp}_${inFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## fne
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Compare} CONFIG.C_Compare_Operation {Not_Equal} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {1} CONFIG.C_Result_Fraction_Width {0} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {2} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class FGt(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val b = Input(UInt((exp+frac).W))
      val out = Output(Bool())
    })

    val m = Module(new FGtBBox(exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    m.io.s_axis_b_tdata := io.b
    m.io.s_axis_b_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

// fgt: supports custom
// 2 cycles
  class FGtBBox(val inExp: Int, val inFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val s_axis_b_tvalid = Input(Bool())
      val s_axis_b_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt(1.W))
    })

    val moduleName = s"Gt_${inExp}_${inFrac}"
    override def desiredName = s"Gt_${inExp}_${inFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## fgt
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Compare} CONFIG.C_Compare_Operation {Greater_Than} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {1} CONFIG.C_Result_Fraction_Width {0} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {2} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class FGe(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val b = Input(UInt((exp+frac).W))
      val out = Output(Bool())
    })

    val m = Module(new FGeBBox(exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    m.io.s_axis_b_tdata := io.b
    m.io.s_axis_b_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

// fge: supports custom
// 2 cycles
  class FGeBBox(val inExp: Int, val inFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val s_axis_b_tvalid = Input(Bool())
      val s_axis_b_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt(1.W))
    })

    val moduleName = s"Ge_${inExp}_${inFrac}"
    override def desiredName = s"Ge_${inExp}_${inFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## fgt
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Compare} CONFIG.C_Compare_Operation {Greater_Than_Or_Equal} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {1} CONFIG.C_Result_Fraction_Width {0} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {2} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class Fix2Float(val dec: Int, val pt: Int, val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((dec+pt).W))
      val out = Output(UInt((exp+frac).W))
    })

    val m = Module(new Fix2FloatBBox(dec, pt, exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  // Fix2Float: Supports custom fixed point and custom floating point widths
  // 6 cycle latency
  class Fix2FloatBBox(val inIntWidth: Int, val inFracWidth: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inIntWidth+inFracWidth).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val moduleName = s"Fix2Float_${inIntWidth}_${inFracWidth}_${outExp}_${outFrac}"
    override def desiredName = s"Fix2Float_${inIntWidth}_${inFracWidth}_${outExp}_${outFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## fix2float
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Fixed_to_float} CONFIG.A_Precision_Type {Custom} CONFIG.Result_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inIntWidth} CONFIG.C_A_Fraction_Width {$inFracWidth} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Accum_Msb {32} CONFIG.C_Accum_Lsb {-31} CONFIG.C_Accum_Input_Msb {32} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {6} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  // TODO: Sign isn't used anywhere
  class Float2Fix(val frac: Int, val exp: Int, val sign: Boolean, val dec: Int, val pt: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val out = Output(UInt((dec+pt).W))
    })

    val m = Module(new Float2FixBBox(exp, frac, dec, pt))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  // Float2Fix: Supports custom fixed point and custom floating point widths
  // 6 cycle latency
  class Float2FixBBox(val inExp: Int, val inFrac: Int, val outIntWidth: Int, val outFracWidth: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outIntWidth+outFracWidth).W))
    })

    val moduleName = s"Float2Fix_${inExp}_${inFrac}_${outIntWidth}_${outFracWidth}"
    override def desiredName = s"Float2Fix_${inExp}_${inFrac}_${outIntWidth}_${outFracWidth}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## float2fix
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Float_to_fixed} CONFIG.A_Precision_Type {Custom} CONFIG.Result_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.C_Result_Exponent_Width {$outIntWidth} CONFIG.C_Result_Fraction_Width {$outFracWidth} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {6} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class Float2Float(val exp1: Int, val frac1: Int, val exp2: Int, val frac2: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp1+frac1).W))
      val out = Output(UInt((exp2+frac2).W))
    })

    val m = Module(new Float2FloatBBox(exp1, frac1, exp2, frac2))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  // Float2Float: Supports custom fixed point and custom floating point widths
  // 2 cycle latency
  class Float2FloatBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val moduleName = s"Float2Float_${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"Float2Float_${inExp}_${inFrac}_${outExp}_${outFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## float2float
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Float_to_float} CONFIG.A_Precision_Type {Custom} CONFIG.Result_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {2} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }


  class FRec(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val out = Output(UInt((exp+frac).W))
    })

    val m = Module(new FRecBBox(exp, frac, exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  class FRecBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val moduleName = s"Reciprocal_${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"Reciprocal_${inExp}_${inFrac}_${outExp}_${outFrac}"
    val precisionType = if ((inExp == 5) && (inFrac == 11)) {"Half"} else {"Single"}
    val Latency = if ((inExp == 5) && (inFrac == 11)) {4} else {29}

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## frec
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Reciprocal} CONFIG.Flow_Control {NonBlocking} CONFIG.A_Precision_Type {$precisionType} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {$precisionType} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Mult_Usage {Full_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {$Latency} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }


  class FRSqrt(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val out = Output(UInt((exp+frac).W))
    })

    val m = Module(new FRSqrtBBox(exp, frac, exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  class FRSqrtBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val moduleName = s"RSqrt_${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"RSqrt_${inExp}_${inFrac}_${outExp}_${outFrac}"
    val precisionType = if ((inExp == 5) && (inFrac == 11)) {"Half"} else {"Single"}
    val Latency = if ((inExp == 5) && (inFrac == 11)) {4} else {32}

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## frsqrt
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Rec_Square_Root} CONFIG.Flow_Control {NonBlocking} CONFIG.A_Precision_Type {$precisionType} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {$precisionType} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Mult_Usage {Full_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {$Latency} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class FFma(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val b = Input(UInt((exp+frac).W))
      val c = Input(UInt((exp+frac).W))
      val out = Output(UInt((exp+frac).W))
    })

    val m = Module(new FFmaBBox(exp, frac, exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    m.io.s_axis_b_tdata := io.b
    m.io.s_axis_b_tvalid := true.B
    m.io.s_axis_c_tdata := io.c
    m.io.s_axis_c_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  // fmul: Supports custom
  // Configured to latency of 8 cycles
  class FFmaBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val s_axis_b_tvalid = Input(Bool())
      val s_axis_b_tdata = Input(UInt((inExp+inFrac).W))
      val s_axis_c_tvalid = Input(Bool())
      val s_axis_c_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val moduleName = s"Float_Mult_Add${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"Float_Mult_Add${inExp}_${inFrac}_${outExp}_${outFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## ffma
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {FMA} CONFIG.A_Precision_Type {Custom} CONFIG.C_Mult_Usage {Full_Usage} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {19} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

  class FAccum(val frac: Int, val exp: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val en = Input(Bool())
      val last  = Input(Bool())
      val out = Output(UInt((exp+frac).W))
    })

    val m = Module(new FAccumBBox(exp, frac, exp, frac))
    m.io.aclk := clock
    m.io.aresetn := ~reset.toBool
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := io.en
    m.io.s_axis_a_tlast := io.last
    io.out := m.io.m_axis_result_tdata
  }

  class FAccumBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val aresetn = Input(Bool())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tlast = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tlast = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val moduleName = s"Accum_${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"Accum_${inExp}_${inFrac}_${outExp}_${outFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      globals.tclScript.println(
s"""
## faccum
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Accumulator} CONFIG.Add_Sub_Value {Add} CONFIG.Has_ARESETn {true} CONFIG.Flow_Control {NonBlocking} CONFIG.A_Precision_Type {Single} CONFIG.C_A_Exponent_Width {8} CONFIG.C_A_Fraction_Width {24} CONFIG.Result_Precision_Type {Single} CONFIG.C_Result_Exponent_Width {8} CONFIG.C_Result_Fraction_Width {24} CONFIG.C_Mult_Usage {Medium_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {22} CONFIG.C_Rate {1} CONFIG.Has_A_TLAST {true} CONFIG.RESULT_TLAST_Behv {Pass_A_TLAST}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      globals.tclScript.flush
      createdIP += moduleName
    }
  }

}
