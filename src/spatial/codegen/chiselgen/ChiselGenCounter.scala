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

  private def createCtr(lhs: Sym[_], start: Sym[_], stop: Sym[_], step: Sym[_], par: I32): Unit = {
    val w = bitWidth(lhs.tp.typeArgs.head)
    start match {case Final(s) => emit(src"${lhs}.set_start($s)")
                 case Expect(s) => emit(src"${lhs}.set_start($s)")
                 case _ => emit(src"${lhs}.set_start(${appendSuffix(lhs.owner, start)})")
                }
    stop match {case Final(s) => emit(src"${lhs}.set_stop($s)")
                 case Expect(s) => emit(src"${lhs}.set_stop($s)")
                 case _ => emit(src"${lhs}.set_stop(${appendSuffix(lhs.owner, stop)})")
                }
    step match {case Final(s) => emit(src"${lhs}.set_step($s)")
                 case Expect(s) => emit(src"${lhs}.set_step($s)")
                 case _ => emit(src"${lhs}.set_step(${appendSuffix(lhs.owner, step)})")
                }
    val p = par match {case Final(s) => s"$s"; case Expect(s) => s"$s"; case _ => s"$par"}

    emitCtrObject(lhs) {
      emit(src"// Owner = ${lhs.owner}")
      emit(src"val par = $p")
      emit(src"val width = $w")
    }

  }
  private def createStreamCChain(lhs: Sym[_], ctrs: Seq[Sym[_]]): Unit = {
    forEachChild(lhs.owner){case (c,i) => 
      createCChain(lhs, ctrs, src"_copy${c}")
    }
  }

  private def createCChain(lhs: Sym[_], ctrs: Seq[Sym[_]], suffix: String = ""): Unit = {
    var isForever = lhs.isForever
    emitCChainObject(lhs, suffix) {
      emit(src"// Owner = ${lhs.owner}")
      emit(src"""override val ctrs = List[CtrObject](${ctrs.map(quote).mkString(",")})""")
      emit(src"""lazy val cchain = Module(new CounterChain(ctrs.map(_.par), ctrs.map(_.fixedStart), ctrs.map(_.fixedStop), ctrs.map(_.fixedStep), """ + 
                       src"""List.fill(${ctrs.size})(Some(0)), ctrs.map(_.isForever), ctrs.map(_.width), myName = "${lhs}${suffix}_cchain"))""")

    }
    emit(src"${lhs}${suffix}.configure()")
    emit(src"""${lhs}${suffix}.cchain.io.input.isStream := ${lhs.isOuterStreamLoop}.B""")

  }
  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case CounterNew(start,end,step,par) => createCtr(lhs,start,end,step,par)
    case CounterChainNew(ctrs) => if (lhs.owner.isOuterStreamLoop) createStreamCChain(lhs,ctrs) else createCChain(lhs,ctrs)
    case ForeverNew() => 
      emitCtrObject(lhs) {
        emit(src"// Owner = ${lhs.owner}")
        emit(src"val par = 1")
        emit(src"val width = 32")
        emit(src"override val isForever = true")
      }

	  case _ => super.gen(lhs, rhs)
  }


}