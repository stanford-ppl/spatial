package spatial.lib.metaprogramming

import forge.tags._
import spatial.dsl._
import spatial.rewrites._
import spatial.node._
import argon._

trait MetaProgramming extends SpatialApp { 

  /*
   * Pass explicit valids to all accessers in meta programming
   * */
  private val _ens = scala.collection.mutable.ListBuffer[Bit]()
  def withEns[T](ens:Bit*) (block: => T) = {
    _ens ++= ens
    val res = block
    _ens --= ens
    res
  }

  override def rewrites() = {
    implicit val state:State = IR 
    super.rewrites()

    IR.rewrites.addGlobal("EnableRewrite", {
      case _ if _ens.isEmpty => None
      case (op:Accessor[_,_], ctx, state) if _ens.forall { en => op.ens.contains(en) } => None
      case (op@FIFOEnq(mem,data,ens), ctx, state) => 
        implicit val A = op.A
        Some(stage(FIFOEnq(mem,data,ens ++ _ens)))
      case (op@FIFODeq(mem,ens), ctx, state) => 
        implicit val A = op.A
        Some(stage(FIFODeq(mem,ens ++ _ens)))
      case (op:SRAMRead[a,c], ctx, state) => 
        implicit val A = op.A
        Some(stage(SRAMRead[a,c](op.mem,op.addr,op.ens ++ _ens)))
      case (op:SRAMWrite[a,c], ctx, state) => 
        implicit val A = op.A
        Some(stage(SRAMWrite[a,c](op.mem,op.data,op.addr,op.ens ++ _ens)))
      case _ => None
    })
  }


  @virtualize
  def MForeach(series:argon.lang.Series[I32])(block: (Int,Int,Int,Bit) => Void) = {
    val start = series.start
    val end = series.end
    val step = series.step
    val parFactor = series.par
    val stride = step*parFactor
    Foreach(start until end by stride, parFactor by 1 par parFactor) { (i,p) =>
      val iter = i + p * step
      val valid = iter < end
      withEns(valid) {
        block(iter, i, p, valid)
      }
    }
  }

  @virtualize
  def MReduce[T:Bits](reg:Reg[T])(series:argon.lang.Series[I32])(block: (Int,Int,Int,Bit) => T)(reduceFunc: (T,T) => T) = {
    val start = series.start
    val end = series.end
    val step = series.step
    val parFactor = series.par
    val stride = step*parFactor
    Reduce[T](reg)(start until end by stride, parFactor by 1 par parFactor) { (i,p) =>
      val iter = i + p * step
      val valid = iter < end
      withEns(valid) {
        block(iter, i, p, valid)
      }
    } { reduceFunc }
  }

  @virtualize
  def ForeachWithLane(series:argon.lang.Series[I32])(block: (Int,Int) => Void) = {
    val start = series.start
    val end = series.end
    val step = series.step
    val parFactor = series.par
    val stride = step*parFactor
    Foreach(start until end by step par parFactor) { i =>
      val lane = ((i-start) / step) % parFactor
      block(i, lane)
    }
  }

  @virtualize
  def ReduceWithLane[T:Bits](reg:Reg[T])(series:argon.lang.Series[I32])(block: (Int,Int) => T)(reduceFunc: (T,T) => T) = {
    val start = series.start
    val end = series.end
    val step = series.step
    val parFactor = series.par
    val stride = step*parFactor
    Reduce[T](reg)(start until end by step par parFactor) { i =>
      val lane = ((i-start) / step) % parFactor
      block(i, lane)
    } { reduceFunc }
  }

  case class FIFOs[T:Num](dup:scala.Int,depth:scala.Int) {
    val fifos = List.fill(dup) { FIFO[T](depth) }
    def deq(i:Int, ens:Bit*) = {
      fifos.zipWithIndex.map { case (fifo, ii) => 
        val valid = i === ii
        val value = FIFO.deq(fifo,Set(valid) ++ ens)
        mux(valid, value, 0.to[T])
      }.reduce { _ + _ }
    }
    def enq(i:Int, data:T, ens:Bit*) = {
      fifos.zipWithIndex.foreach { case (fifo,ii) =>
        FIFO.enq(fifo,data,Set(i===ii) ++ ens)
      }
    }
  }

}
