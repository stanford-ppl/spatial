package spatial.codegen.chiselgen


import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.metadata.bounds._
import spatial.metadata.access._
import spatial.metadata.retiming._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.metadata.types._
import spatial.util.modeling.scrubNoise
import spatial.util.spatialConfig


trait ChiselGenCounter extends ChiselGenCommon {


  private def emitCChainObject(lhs: Sym[_])(contents: => Unit): Unit = {
    inGen(out, "CounterChains.scala"){
      open(src"object $lhs extends CChainObject{")
        contents
      close("}")
    }
  }

  private def createCChain(lhs: Sym[_], ctrs: Seq[Sym[_]]): Unit = {
    var isForever = lhs.isForever
    val counter_data = ctrs.map{
      case c@Op(CounterNew(start, end, step, par)) => 
        val w = bitWidth(c.tp.typeArgs.head)
        val (start_wire, start_constr) = start match {case Final(s) => (src"${s}.FP(true, $w, 0)", src"Some($s)"); 
                                                      case Expect(s) => (src"${s}.FP(true, $w, 0)", src"Some($s)"); 
                                                      case _ => val n = quote(start); (n + {if (n.startsWith("x")) ".get" else ""}, "None")}
        val (end_wire, end_constr) = end match {case Final(e) => (src"${e}.FP(true, $w, 0)", src"Some($e)"); 
                                                      case Expect(e) => (src"${e}.FP(true, $w, 0)", src"Some($e)"); 
                                                      case _ => val n = quote(end); (n + {if (n.startsWith("x")) ".get" else ""}, "None")}
        val (stride_wire, stride_constr) = step match {case Final(st) => (src"${st}.FP(true, $w, 0)", src"Some($st)"); 
                                                      case Expect(st) => (src"${st}.FP(true, $w, 0)", src"Some($st)"); 
                                                      case _ => val n = quote(step); (n + {if (n.startsWith("x")) ".get" else ""}, "None")}
        val par_wire = {src"$par"}.split('.').take(1)(0).replaceAll("L","") // TODO: What is this doing?
        (start_wire, end_wire, stride_wire, par_wire, start_constr, end_constr, stride_constr, "Some(0)")
      case Op(ForeverNew()) => 
        isForever = true
        ("0.S", "999.S", "1.S", "1", "None", "None", "None", "Some(0)") 
    }
    val passValues = {counter_data.map(_._3) ++ counter_data.map(_._2) ++ counter_data.map(_._1)}.collect{case x if (x.startsWith("x")) => x}
    passValues.foreach{vv => 
      val v = vv.replace(".get","")
      emit(src"$lhs.set_$v($v)")
    }
    emitCChainObject(lhs) {
      emit(src"// Owner = ${lhs.owner}")
      passValues.foreach{vv => 
        val v = vv.replace(".get","")
        emit(src"var $v: Option[FixedPoint] = None")
        emit(src"def set_$v(x: FixedPoint): Unit = {$v = Some(x)}")
      }
      emit(src"""lazy val strides = List(${counter_data.map(_._3)})""")
      emit(src"""lazy val stops = List(${counter_data.map(_._2)})""")
      emit(src"""lazy val starts = List(${counter_data.map{_._1}}) """)
      emit(src"""val cchain = Module(new CounterChain(List(${counter_data.map(_._4)}), """ + 
                       src"""List(${counter_data.map(_._5)}), List(${counter_data.map(_._6)}), List(${counter_data.map(_._7)}), """ + 
                       src"""List(${counter_data.map(_._8)}), List(${ctrs.map(c => bitWidth(c.tp.typeArgs.head))}), myName = "${lhs}_cchain"))""")

      emit(src"""cchain.io.input.isStream := ${streamCopyWatchlist.contains(lhs)}.B""")
    }
    emit(src"${lhs}.configure()")

  }
  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case CounterNew(start,end,step,par) => 
    case CounterChainNew(ctrs) => createCChain(lhs,ctrs)
    case ForeverNew() => 
      emit("// $lhs = Forever")

	  case _ => super.gen(lhs, rhs)
  }


}