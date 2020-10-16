package spatial.tests.syntax

import spatial.dsl._

@struct case class MyStruct(x: I32, y: I32, z: I32) {
  @forge.tags.api def +(that: MyStruct): MyStruct = {
    MyStruct(x + that.x, y + that.y, z + that.z)
  }
}

@spatial class SimpleStructTest extends SpatialTest {

  def main(args: Array[String]): Void = {
    val x = random[MyStruct]
    val y = random[MyStruct]
    val z: MyStruct = x + y
    println(r"x: $x")
    println(r"y: $y")
    println(r"x + y: $z")
    assert(x.x + y.x == z.x)
    assert(x.y + y.y == z.y)
    assert(x.z + y.z == z.z)
  }
}

class GenericStruct[T](implicit val evidence$1: Num[T]) extends spatial.lang.Struct[GenericStruct[T]] with argon.Ref[Any, GenericStruct[T]] with Bits[GenericStruct[T]] {
  lazy val fields = Seq("x".->(argon.Type[T]), "y".->(argon.Type[T]), "z".->(argon.Type[T]));
  override val box = implicitly[<:<[GenericStruct[T], spatial.lang.Struct[GenericStruct[T]] with argon.lang.types.Bits[GenericStruct[T]]]];
  override protected val __typePrefix = "GenericStruct";
  override protected val __typeArgs = cargs.collect({ case (t@((_): argon.Type[(_)]
    ) ) => t
  });
  override protected val __typeParams = Seq(evidence$1).filter({ case (t@((_): argon.Type[(_)]
    ) ) => false
    case _ => true
  })

  def copy(x: T = null.asInstanceOf[T], y: T = null.asInstanceOf[T], z: T = null.asInstanceOf[T])(implicit ctx: forge.SrcCtx, state: argon.State, num: Num[T]): GenericStruct[T] = GenericStruct.apply(Option(x).getOrElse(this.x(ctx, state)), Option(y).getOrElse(this.y(ctx, state)), Option(z).getOrElse(this.z(ctx, state)))(ctx, state, num);

  def x(implicit ctx: forge.SrcCtx, state: argon.State): T = field[T]("x")(argon.Type[T], ctx, state);

  def y(implicit ctx: forge.SrcCtx, state: argon.State): T = field[T]("y")(argon.Type[T], ctx, state);

  def z(implicit ctx: forge.SrcCtx, state: argon.State): T = field[T]("z")(argon.Type[T], ctx, state);

  @forge.tags.api def +(that: GenericStruct[T]): GenericStruct[T] = GenericStruct(x.+(that.x), y.+(that.y), z.+(that.z));

  def nbits(implicit ctx: forge.SrcCtx, state: argon.State): scala.Int = {
    val bitsOpt = List(new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits], new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits], new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits]); if (bitsOpt.forall(((x$10) => x$10.isDefined)).`unary_!`) {
      argon.error(ctx, StringContext("nbits is not defined for ", "").s(this.tp))(state); argon.error(ctx)(state); 0
    } else List(new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits].get.nbits, new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits].get.nbits, new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits].get.nbits).sum
  };

  def zero(implicit ctx: forge.SrcCtx, state: argon.State): GenericStruct[T] = bitsCheck("zero")(GenericStruct.apply(new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits].get.zero, new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits].get.zero, new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits].get.zero));

  def one(implicit ctx: forge.SrcCtx, state: argon.State): GenericStruct[T] = bitsCheck("one")(GenericStruct.apply(new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits].get.one, new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits].get.one, new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits].get.one));

  private def bitsCheck(op: java.lang.String)(func: => GenericStruct[T])(implicit ctx: forge.SrcCtx, state: argon.State): GenericStruct[T] = {
    val bitsOpt = List(new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits], new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits], new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits]); if (bitsOpt.forall(((x$9) => x$9.isDefined)).`unary_!`) {
      argon.error(ctx, StringContext("", " not defined for ", "").s(op, this.tp))(state); argon.error(ctx)(state); argon.err[GenericStruct[T]](StringContext("", " not defined for ", "").s(op, this.tp))(argon.Type[GenericStruct[T]], state)
    } else func
  };

  def random(max: Option[GenericStruct[T]])(implicit ctx: forge.SrcCtx, state: argon.State): GenericStruct[T] = bitsCheck("random")(GenericStruct.apply(new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits].get.random(max.map(((x$6) => x$6.x))), new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits].get.random(max.map(((x$7) => x$7.y))), new argon.static.ExpTypeLowPriority(argon.Type[T]).getView[argon.lang.types.Bits].get.random(max.map(((x$8) => x$8.z)))));

  override protected def fresh = new GenericStruct[T]();

  private def cargs: Seq[Any] = Seq(evidence$1);
};

object GenericStruct {
  def apply[T](x: T, y: T, z: T)(implicit ctx: forge.SrcCtx, state: argon.State, evidence$1: Num[T]): GenericStruct[T] = spatial.lang.Struct[GenericStruct[T]]("x".->(x), "y".->(y), "z".->(z))(spatial.lang.Struct.tp[GenericStruct[T]], ctx, state);

  implicit def tp[T](implicit evidence$1: Num[T]): GenericStruct[T] = argon.proto(new GenericStruct[T]()(evidence$1))
};

//@struct case class GenericStruct[T: Num](x: T, y: T, z: T) {
//  @forge.tags.api def +(that: GenericStruct[T]): GenericStruct[T] = {
//    GenericStruct(x + that.x, y + that.y, z + that.z)
//  }
//}

@spatial class GenericStructTest extends SpatialTest {
  def main(args: Array[String]): Void = {
    val x = random[GenericStruct[I32]]
    val y = random[GenericStruct[I32]]
    val z = x + y
    println(r"x: $x")
    println(r"y: $y")
    println(r"x + y: $z")
    assert(x.x + y.x == z.x)
    assert(x.y + y.y == z.y)
    assert(x.z + y.z == z.z)
  }
}
