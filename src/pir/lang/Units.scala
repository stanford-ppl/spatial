package pir.lang

import argon._
import forge.tags._

import spatial.lang._
import pir.node._

import scala.collection.mutable
import scala.language.existentials

abstract class PUPtr {
  protected[pir] def unit: Option[PU]

  private[pir] val _inputs = mutable.Map[Int,In[_]]()
  private[pir] val _outputs = mutable.Map[Int,Out[_]]()

  def inputs: mutable.Map[Int,In[_]] = unit.map(_.ins).getOrElse(_inputs)
  def outputs: mutable.Map[Int,Out[_]] = unit.map(_.outs).getOrElse(_outputs)

  @api def in[A:Bits](i: Int): In[A] = inputs.get(i) match {
    case Some(in) if in.tA =:= Bits[A] => in.asInstanceOf[In[A]]
    case Some(in) =>
      error(ctx, s"Cannot cast input $i as both ${Bits[A]} and ${in.tA}")
      error(ctx)
      boundVar[In[A]]
    case None =>
      val in = boundVar[In[A]]
      inputs += i -> in
      in
  }

  @api def out[A:Bits](i: Int): Out[A] = outputs.get(i) match {
    case Some(out) if out.tA =:= Bits[A] => out.asInstanceOf[Out[A]]
    case Some(out) =>
      error(ctx, s"Cannot cast output $i as both ${Bits[A]} and ${out.tA}")
      error(ctx)
      boundVar[Out[A]]
    case None =>
      val out = boundVar[Out[A]]
      outputs += i -> out
      out
  }
}

case class PCUPtr(protected[pir] var unit: Option[VPCU] = None) extends PUPtr {

}
case class PMUPtr(protected[pir] var unit: Option[VPMU] = None) extends PUPtr {
  @api def read(path: => I32): Void = this.read(Nil){_ => path}
  @api def read(r0: Series[I32])(path: I32 => I32): Void = this.read(Seq(r0)){i => path(i(0)) }
  @api def read(r0: Series[I32], r1: Series[I32])(path: (I32,I32) => I32): Void = this.read(Seq(r0,r1)){i => path(i(0),i(1)) }
  @api def read(r0: Series[I32], r1: Series[I32], r2: Series[I32])(path: (I32,I32,I32) => I32): Void = this.read(Seq(r0,r1,r2)){i => path(i(0),i(1),i(2)) }

  @api def read(ranges: Seq[Series[I32]])(path: Seq[I32] => I32): Void = {
    val u = unit.getOrElse{throw new Exception("Modified uninitialized PMUPtr") }
    curPtr.withPtr(this){
      val indices = ranges.map{_ => boundVar[I32] }
      val block = stageBlock{
        val ctrs = ranges.map{r => Counter.from(r) }
        CounterChain(ctrs)
        val addr = path(indices)
        stage(Addr(addr))
      }
      u.setRd(block, Seq(indices))
    }
    void
  }

  @api def write(path: => (I32,Bits[_])): Void = this.write(Nil){_ => path}
  @api def write(r0: Series[I32])(path: I32 => (I32,Bits[_])): Void = this.write(Seq(r0)){i => path(i(0)) }
  @api def write(r0: Series[I32], r1: Series[I32])(path: (I32,I32) => (I32,Bits[_])): Void = this.write(Seq(r0,r1)){i => path(i(0),i(1)) }
  @api def write(r0: Series[I32], r1: Series[I32], r2: Series[I32])(path: (I32,I32,I32) => (I32,Bits[_])): Void = this.write(Seq(r0,r1,r2)){i => path(i(0),i(1),i(2)) }

  @api def write(ranges: Seq[Series[I32]])(path: Seq[I32] => (I32,Bits[_])): Void = {
    val u = unit.getOrElse{throw new Exception("Modified uninitialized PMUPtr") }
    curPtr.withPtr(this){
      val indices = ranges.map{_ => boundVar[I32] }
      val block = stageBlock{
        val ctrs = ranges.map{r => Counter.from(r) }
        CounterChain(ctrs)
        val (addr,data) = path(indices)
        stage(Addr(addr))
        stage(Data(data))
      }
      u.setWr(block, Seq(indices))
    }
    void
  }
}

case class CurrentPtr(ptr: PUPtr) extends InputData[CurrentPtr]
@data object curPtr {
  def get: Option[PUPtr] = globals[CurrentPtr].map(_.ptr)
  def set(ptr: PUPtr): Unit = globals.add(CurrentPtr(ptr))
  def clear(): Unit = globals.clear[CurrentPtr]
  def withPtr[A](ptr: PUPtr)(scope: => A): A = {
    val prevPtr = curPtr.get
    curPtr.set(ptr)
    val result = scope
    curPtr.clear()
    prevPtr.foreach{ptr => curPtr.set(ptr) }
    result
  }
}

object PCU {
  @api def apply(datapath: => Void): PCUPtr = PCU.apply(Nil){_ => datapath }
  @api def apply(r0: Series[I32])(datapath: I32 => Void): PCUPtr = PCU.apply(Seq(r0)){is => datapath(is.head)}
  @api def apply(r0: Series[I32], r1: Series[I32])(datapath: (I32,I32) => Void): PCUPtr = PCU.apply(Seq(r0,r1)){is => datapath(is(0),is(1)) }
  @api def apply(r0: Series[I32], r1: Series[I32], r2: Series[I32])(datapath: (I32,I32,I32) => Void): PCUPtr = PCU.apply(Seq(r0,r1,r2)){is => datapath(is(0),is(1),is(2)) }

  @api def apply(ranges: Seq[Series[I32]])(datapath: Seq[I32] => Void): PCUPtr = {
    val ptr = PCUPtr()
    curPtr.withPtr(ptr) {
      val indices = ranges.map{_ => boundVar[I32] }
      val block = stageBlock {
        val ctrs = ranges.map{r => Counter.from(r) }
        CounterChain(ctrs)
        datapath(indices)
      }
      val pcu = VPCU(block, Seq(indices))
      stage(pcu)
      pcu.ins ++= ptr.inputs
      pcu.outs ++= ptr.outputs
      ptr.unit = Some(pcu)
      ptr
    }
  }
}

object PMU {
  @api def apply[C[A]](mem: SRAM[_,C]): PMUPtr = create(mem)
  @api def apply(mem: FIFO[_]): PMUPtr = create(mem)
  @api def apply(mem: LIFO[_]): PMUPtr = create(mem)

  @api private def create(mem: Sym[_]): PMUPtr = {
    val pmu = VPMU(Seq(mem))
    val ptr = PMUPtr(Some(pmu))
    stage(pmu)
    ptr
  }
}
