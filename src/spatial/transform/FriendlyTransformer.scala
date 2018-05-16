package spatial.transform

import argon._
import argon.transform.MutateTransformer
import spatial.traversal.AccelTraversal
import spatial.data._
import spatial.lang._
import spatial.node._
import spatial.util._
import spatial.internal._

case class FriendlyTransformer(IR: State) extends MutateTransformer with AccelTraversal {
  private var dimMapping: Map[I32,I32] = Map.empty
  private var addedArgIns: Seq[(Sym[_],Sym[_])] = Nil
  private var mostRecentWrite: Map[Reg[_], Sym[_]] = Map.empty

  def argIn[A](x: Bits[A]): Sym[A] = {
    implicit val bA: Bits[A] = x.selfType
    val arg: Reg[A] = stage(ArgInNew[A](bA.zero))
    dbg(s"Inserted ArgIn $arg for value $x")
    setArg(arg,x.unbox)
    arg.value
  }

  def extract[A:Type](lhs: Sym[A], rhs: Op[A], reg: Reg[A], tp: String): Sym[A] = mostRecentWrite.get(reg) match {
    case Some(data) if lhs.parent.hasAncestor(data.parent) =>
      // Don't get rid of reads being used for DRAM allocations
      if (lhs.consumers.exists{case Op(DRAMNew(_, _)) => true; case _ => false }) {
        dbg(s"Node $lhs ($rhs) has a dram reading its most recent write")
        super.transform(lhs, rhs)
      }
      else {
        dbg(s"Node $lhs ($rhs) has data that can be directly extracted ($data -> ${f(data)})")
        f(data).asInstanceOf[Sym[A]]
      }

    case Some(data) => super.transform(lhs,rhs)

    case None =>
      warn(lhs.ctx, s"$tp ($lhs) was used before being set. This will result in 0 at runtime.")
      warn(lhs.ctx)
      reg.A.zero
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(block) => inAccel {
      val rawInputs = block.nestedInputs
      val dataInputs = rawInputs.filterNot(_.isRemoteMem).filterNot(addedArgIns.contains)
      val bitsInputs = dataInputs.collect{case b: Bits[_] => b}

      // Add ArgIns for arbitrary bit inputs
      addedArgIns ++= bitsInputs.map{s => s -> argIn(f(s)) }

      isolateSubstWith(addedArgIns:_*){ super.transform(lhs,rhs) }
    }

    // Add ArgIns for DRAM dimensions
    case DRAMNew(F(dims),_) =>
      dimMapping ++= dims.distinct.map{
        case d @ Op(RegRead(reg)) if reg.isArgIn  => dbg(s"DRAM: $lhs ($dims), dim: $d = ArgIn, mapping to $d");                         d -> d
        case d @ Op(RegRead(reg)) if reg.isHostIO => dbg(s"DRAM: $lhs ($dims), dim: $d = HostIO, mapping to $d");                        d -> d   // TODO[3]: Allow DRAM dims with HostIO?
        case d if d.isValue                       => dbg(s"DRAM: $lhs ($dims), dim: $d = isValue, mapping to $d");                       d -> d
        case d if dimMapping.contains(d)          => dbg(s"DRAM: $lhs ($dims), dim: $d = previously used, mapping to ${dimMapping(d)}"); d -> dimMapping(d)
        case d                                    => dbg(s"DRAM: $lhs ($dims), dim: $d = other, mapping to ${argIn(d).unbox}");          d -> argIn(d).unbox
      }
      val dims2 = dims.map{d => dimMapping(d) }
      addedArgIns ++= dims.zip(dims2)
      isolateSubstWith(dims.zip(dims2):_*){ super.transform(lhs,rhs) }

    case GetReg(F(reg)) =>
      def read(tp: String): Sym[A] = {
        warn(ctx, s"Use register read syntax (.value) for reading $tp inside Accel.")
        warn(ctx)
        reg.value
      }

      def noHostGet(tp: String): Sym[A] = {
        error(ctx, s"Getting $tp outside Accel is disallowed. Use a HostIO or ArgOut.")
        error(ctx)
        err[A](s"Get $tp in host")
      }

      if (!inHw) {
        if       (reg.isArgIn) extract(lhs,rhs,reg,"ArgIn")
        else if (reg.isHostIO) extract(lhs,rhs,reg,"HostIO")
        else if (reg.isArgOut) super.transform(lhs,rhs)
        else                   noHostGet("registers")
      }
      else {
        if       (reg.isArgIn) read("ArgIn registers")
        else if (reg.isHostIO) read("HostIO registers")
        else if (reg.isArgOut) read("ArgOut registers")
        else                   read("registers")
      }


    case RegRead(F(reg)) =>
      def get(tp: String): Sym[A] = getArg(reg)

      def noHostRead(tp: String): Sym[A] = {
        error(ctx, s"Reading $tp outside Accel is disallowed. Use a HostIO or ArgOut.")
        error(ctx)
        err[A](s"Read $tp in host")
      }

      if (!inHw) {
        if       (reg.isArgIn) extract(lhs,rhs,reg,"ArgIn")
        else if (reg.isHostIO) extract(lhs,rhs,reg,"HostIO")
        else if (reg.isArgOut) get("ArgOut")
        else                   noHostRead("registers")
      }
      else {
        if       (reg.isArgIn) super.transform(lhs,rhs)
        else if (reg.isHostIO) super.transform(lhs,rhs)
        else if (reg.isArgOut) super.transform(lhs,rhs)
        else                   super.transform(lhs,rhs)
      }

    // reg := data
    case RegWrite(F(reg),data,_) =>
      mostRecentWrite += reg -> data
      dbg(s"Adding $reg -> $data to mostRecentWrite map ($mostRecentWrite)")

      def set(tp: String): Sym[A] = {
        warn(ctx, s"Use setArg for setting $tp outside Accel.")
        warn(ctx)
        setArg(reg, f(data)).asInstanceOf[Sym[A]]
      }
      def noHostWrite(tp: String): Sym[A] = {
        error(ctx, s"Writing $tp outside Accel is disallowed. Use a HostIO or ArgIn.")
        error(ctx)
        err[A](s"Write $tp in host")
      }
      def noAccelWrite(tp: String): Sym[A] = {
        error(ctx, s"Writing $tp inside Accel is disallowed. Use a HostIO or ArgOut.")
        error(ctx)
        err[A](s"Write $tp in Accel")
      }
      if (!inHw) {
        if       (reg.isArgIn) set("ArgIn registers")
        else if (reg.isHostIO) set("HostIO registers")
        else if (reg.isArgOut) noHostWrite("ArgOut registers")
        else                   noHostWrite("registers")
      }
      else {
        if       (reg.isArgIn) noAccelWrite("ArgIn registers")
        else if (reg.isHostIO) super.transform(lhs,rhs)
        else if (reg.isArgOut) super.transform(lhs,rhs)
        else                   super.transform(lhs,rhs)
      }


    case SetReg(F(reg),data) =>
      mostRecentWrite += reg -> data
      dbg(s"Adding $reg -> $data to mostRecentWrite map ($mostRecentWrite)")

      def write(tp: String): Sym[A] = {
        warn(ctx, s"Use register assignment syntax, :=, for writing $tp within Accel.")
        warn(ctx)
        (reg := f(data)).asInstanceOf[Sym[A]]
      }
      def noHostWrite(tp: String): Sym[A] = {
        error(ctx, s"Setting $tp outside Accel is disallowed. Use a HostIO or ArgIn.")
        error(ctx)
        err[A](s"Write $tp in host")
      }
      def noAccelWrite(tp: String): Sym[A] = {
        error(ctx, s"Setting $tp inside Accel is disallowed. Use a HostIO or ArgOut.")
        error(ctx)
        err[A](s"Write $tp in Accel")
      }

      if (!inHw) {
        if       (reg.isArgIn) super.transform(lhs,rhs)
        else if (reg.isHostIO) super.transform(lhs,rhs)
        else if (reg.isArgOut) noHostWrite("ArgOut registers")
        else                   noHostWrite("registers")
      }
      else {
        if       (reg.isArgIn) noAccelWrite("ArgIn registers")
        else if (reg.isHostIO) write("HostIO registers")
        else if (reg.isArgOut) write("ArgOut registers")
        else                   write("registers")
      }

    case AssertIf(F(ens),F(cond),None) =>
      assertIf(ens, cond, Some(s"$ctx: Assertion failure")).asInstanceOf[Sym[A]]

    case AssertIf(F(ens),F(cond),Some(msg)) =>
      assertIf(ens, cond, Some(Text(s"$ctx:") + msg)).asInstanceOf[Sym[A]]

    case _ => super.transform(lhs,rhs)
  }
}
