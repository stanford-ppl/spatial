package fringe.fringeASIC.bigIP
import fringe.FringeGlobals
import chisel3._
import chisel3.core.IntParam
import chisel3.util._
import scala.collection.mutable.Set

trait ASICBlackBoxes {

  // To avoid creating the same IP twice
  val createdIP = Set[String]()

  class Divider(val dividendWidth: Int, val divisorWidth: Int, val signed: Boolean, val latency: Int) extends Module {
    val io = IO(new Bundle {
      val dividend = Input(UInt(dividendWidth.W))
      val divisor  = Input(UInt(divisorWidth.W))
      val out      = Output(UInt(dividendWidth.W))
    })

    val fractionBits = 0

    val m = Module(new designware_divmod(dividendWidth, divisorWidth, signed, false, fractionBits, latency))
    m.io.clock    := clock
    m.io.reset    := reset.toBool
    m.io.dividend := io.dividend
    m.io.divisor  := io.divisor
    io.out        := m.io.quot_out(dividendWidth-1, fractionBits)
  }

  class Modulo(val dividendWidth: Int, val divisorWidth: Int, val signed: Boolean, val latency: Int) extends Module {
    val io = IO(new Bundle {
      val dividend = Input(UInt(dividendWidth.W))
      val divisor  = Input(UInt(divisorWidth.W))
      val out      = Output(UInt(dividendWidth.W))
    })

    val fractionBits = 0

    val m = Module(new designware_divmod(dividendWidth, divisorWidth, signed, true, fractionBits, latency))
    m.io.clock    := clock
    m.io.reset    := reset.toBool
    m.io.dividend := io.dividend
    m.io.divisor  := io.divisor
    io.out        := m.io.rem_out(dividendWidth-1, fractionBits)
  }

  class designware_divmod(val dividendWidth: Int, val divisorWidth: Int, val signed: Boolean, val isMod: Boolean, val fractionBits: Int, val latency: Int) extends BlackBox(
    Map("DIVIDEND_BIT_WIDTH" -> IntParam(dividendWidth),
        "DIVISOR_BIT_WIDTH"  -> IntParam(divisorWidth),
        "SIGNED"             -> IntParam(if (signed) 1 else 0),
        "IS_DIV"             -> IntParam(if (isMod) 0 else 1),
        "NUM_STAGES"         -> IntParam(latency))
    ) {
    override def desiredName = s"designware_divmod"
    val io = IO(new Bundle {
      val clock    = Input(Clock())
      val reset    = Input(Bool())
      val dividend = Input(UInt(dividendWidth.W))
      val divisor  = Input(UInt(divisorWidth.W))
      val quot_out = Output(UInt(dividendWidth.W))
      val rem_out  = Output(UInt(dividendWidth.W))
    })

  }

  class Multiplier(val aWidth: Int, val bWidth: Int, val outWidth: Int, val signed: Boolean, val latency: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt(aWidth.W))
      val b = Input(UInt(bWidth.W))
      val out = Output(UInt(outWidth.W))
    })

    val m = Module(new designware_mult(aWidth, bWidth, outWidth, signed, latency))
    m.io.clock := clock
    m.io.reset := reset.toBool
    m.io.in0 := io.a
    m.io.in1 := io.b
    io.out := m.io.out
  }


  class designware_mult(val aWidth: Int, val bWidth: Int, val outWidth: Int, val signed: Boolean, val latency: Int) extends BlackBox (
    Map("IN0_BIT_WIDTH"      -> IntParam(aWidth),
        "IN1_BIT_WIDTH"      -> IntParam(bWidth),
        "OUT_BIT_WIDTH"      -> IntParam(outWidth),
        "SIGNED"             -> IntParam(if (signed) 1 else 0),
        "NUM_STAGES"         -> IntParam(latency))
    ) {
    override def desiredName = s"designware_mult"

    val io = IO(new Bundle {
      val clock = Input(Clock())
      val reset = Input(Bool())
      val in0 = Input(UInt(aWidth.W))
      val in1 = Input(UInt(bWidth.W))
      val out = Output(UInt(outWidth.W))
    })

  }


  // fabs
  class FAbsBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val moduleName = s"Absolute_${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"Absolute_${inExp}_${inFrac}_${outExp}_${outFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      FringeGlobals.tclScript.println(
s"""
## fabs
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Absolute} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {0} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
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

    val moduleName = s"Exponential_${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"Exponential_${inExp}_${inFrac}_${outExp}_${outFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      FringeGlobals.tclScript.println(
s"""
## fexp
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Exponential} CONFIG.Flow_Control {NonBlocking} CONFIG.A_Precision_Type {Single} CONFIG.C_A_Exponent_Width {8} CONFIG.C_A_Fraction_Width {24} CONFIG.Result_Precision_Type {Single} CONFIG.C_Result_Exponent_Width {8} CONFIG.C_Result_Fraction_Width {24} CONFIG.C_Mult_Usage {Medium_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {20} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
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

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      FringeGlobals.tclScript.println(
s"""
## flog
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Logarithm} CONFIG.Flow_Control {NonBlocking} CONFIG.Maximum_Latency {true} CONFIG.A_Precision_Type {Single} CONFIG.C_A_Exponent_Width {8} CONFIG.C_A_Fraction_Width {24} CONFIG.Result_Precision_Type {Single} CONFIG.C_Result_Exponent_Width {8} CONFIG.C_Result_Fraction_Width {24} CONFIG.C_Mult_Usage {Medium_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {22} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
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
      FringeGlobals.tclScript.println(
s"""
## fsqrt
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Square_root} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {28} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
	}

  class FAdd(val exp: Int, val frac: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt((exp+frac).W))
      val b = Input(UInt((exp+frac).W))
      val out = Output(UInt((exp+frac).W))
    })

    val m = Module(new FAddBBox(exp, frac, exp, frac))
    m.io.aclk := clock
    m.io.s_axis_a_tdata := io.a
    m.io.s_axis_a_tvalid := true.B
    m.io.s_axis_b_tdata := io.b
    m.io.s_axis_b_tvalid := true.B
    io.out := m.io.m_axis_result_tdata
  }

  // fadd: Supports custom
  // Set to max latency of 12 cycles
  class FAddBBox(val inExp: Int, val inFrac: Int, val outExp: Int, val outFrac: Int) extends BlackBox {

    val io = IO(new Bundle {
      val aclk = Input(Clock())
      val s_axis_a_tvalid = Input(Bool())
      val s_axis_a_tdata = Input(UInt((inExp+inFrac).W))
      val s_axis_b_tvalid = Input(Bool())
      val s_axis_b_tdata = Input(UInt((inExp+inFrac).W))
      val m_axis_result_tvalid = Output(Bool())
      val m_axis_result_tdata = Output(UInt((outExp+outFrac).W))
    })

    val moduleName = s"Add_${inExp}_${inFrac}_${outExp}_${outFrac}"
    override def desiredName = s"Add_${inExp}_${inFrac}_${outExp}_${outFrac}"

    // Print required stuff into a tcl file
    if (!createdIP.contains(moduleName)) {
      FringeGlobals.tclScript.println(
s"""
## fadd
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Add_Sub_Value {Add} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {12}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
	}

  class FSub(val exp: Int, val frac: Int) extends Module {
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
      FringeGlobals.tclScript.println(
s"""
## fsub
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Add_Sub_Value {Subtract} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Mult_Usage {No_Usage} CONFIG.C_Latency {12}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
	}

  class FMul(val exp: Int, val frac: Int) extends Module {
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
      FringeGlobals.tclScript.println(
s"""
## fmul
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Multiply} CONFIG.A_Precision_Type {Custom} CONFIG.C_Mult_Usage {Full_Usage} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {8} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
	}

  class FDiv(val exp: Int, val frac: Int) extends Module {
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
      FringeGlobals.tclScript.println(
s"""
## fdiv
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Divide} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {$inExp} CONFIG.C_Result_Fraction_Width {$inFrac} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {28} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
	}

  class FLt(val exp: Int, val frac: Int) extends Module {
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
      FringeGlobals.tclScript.println(
s"""
## flt
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Compare} CONFIG.C_Compare_Operation {Less_Than} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {1} CONFIG.C_Result_Fraction_Width {0} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {2} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
  }

  class FLe(val exp: Int, val frac: Int) extends Module {
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
      FringeGlobals.tclScript.println(
s"""
## fle
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Compare} CONFIG.C_Compare_Operation {Less_Than_Or_Equal} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {1} CONFIG.C_Result_Fraction_Width {0} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {2} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
  }

  class FEq(val exp: Int, val frac: Int) extends Module {
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
      FringeGlobals.tclScript.println(
s"""
## feq
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Compare} CONFIG.C_Compare_Operation {Equal} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {1} CONFIG.C_Result_Fraction_Width {0} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {2} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
  }

  class FNe(val exp: Int, val frac: Int) extends Module {
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
      FringeGlobals.tclScript.println(
s"""
## fne
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Compare} CONFIG.C_Compare_Operation {Not_Equal} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {1} CONFIG.C_Result_Fraction_Width {0} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {2} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
  }

  class FGt(val exp: Int, val frac: Int) extends Module {
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
      FringeGlobals.tclScript.println(
s"""
## fgt
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Compare} CONFIG.C_Compare_Operation {Greater_Than} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {1} CONFIG.C_Result_Fraction_Width {0} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {2} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
  }

  class FGe(val exp: Int, val frac: Int) extends Module {
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
      FringeGlobals.tclScript.println(
s"""
## fgt
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Compare} CONFIG.C_Compare_Operation {Greater_Than_Or_Equal} CONFIG.A_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.Result_Precision_Type {Custom} CONFIG.C_Result_Exponent_Width {1} CONFIG.C_Result_Fraction_Width {0} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {2} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
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
      FringeGlobals.tclScript.println(
s"""
## fix2float
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Fixed_to_float} CONFIG.A_Precision_Type {Custom} CONFIG.Result_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inIntWidth} CONFIG.C_A_Fraction_Width {$inFracWidth} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Accum_Msb {32} CONFIG.C_Accum_Lsb {-31} CONFIG.C_Accum_Input_Msb {32} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {6} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
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
      FringeGlobals.tclScript.println(
s"""
## float2fix
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Float_to_fixed} CONFIG.A_Precision_Type {Custom} CONFIG.Result_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.C_Result_Exponent_Width {$outIntWidth} CONFIG.C_Result_Fraction_Width {$outFracWidth} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {6} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
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
      FringeGlobals.tclScript.println(
s"""
## float2float
create_ip -name floating_point -vendor xilinx.com -library ip -version 7.1 -module_name $moduleName
set_property -dict [list CONFIG.Operation_Type {Float_to_float} CONFIG.A_Precision_Type {Custom} CONFIG.Result_Precision_Type {Custom} CONFIG.Flow_Control {NonBlocking} CONFIG.C_A_Exponent_Width {$inExp} CONFIG.C_A_Fraction_Width {$inFrac} CONFIG.C_Result_Exponent_Width {$outExp} CONFIG.C_Result_Fraction_Width {$outFrac} CONFIG.C_Mult_Usage {No_Usage} CONFIG.Has_RESULT_TREADY {false} CONFIG.C_Latency {2} CONFIG.C_Rate {1}] [get_ips $moduleName]
set_property generate_synth_checkpoint false [get_files $moduleName.xci]
generate_target {all} [get_ips $moduleName]

""")

      FringeGlobals.tclScript.flush
      createdIP += moduleName
		}
  }
}


