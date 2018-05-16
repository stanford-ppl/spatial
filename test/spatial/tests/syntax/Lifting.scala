package spatial.tests.syntax

import spatial.dsl._

@test class LiftBoolean extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: scala.Boolean): A = x.to[A]

  def main(args: Array[String]): Void = {
    val x = true
    val bit = x.to[Bit]
    val i8  = x.to[I8]
    val i16 = x.to[I16]
    val i32 = x.to[I32]
    val i64 = x.to[I64]
    val f32 = x.to[F32]
    val f64 = x.to[F64]
    val fix = x.to[MyFix]
    val flt = x.to[MyFlt]
    val num = number[MyFix](x)
    println(r"bit: $bit, i8: $i8, i16: $i16, i32: $i32, i64: $i64, fix: $fix")
    println(r"f32: $f32, f64: $f64, flt: $flt, num: $num")
    assert(i8 == 1)
    assert(i16 == 1)
    assert(i32 == 1)
    assert(i64 == 1)
    assert(f32 == 1)
    assert(f64 == 1)
    assert(fix == 1)
    assert(flt == 1)
    assert(num == 1)
  }
}

@test class LiftByte extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: scala.Byte): A = x.to[A]

  def main(args: Array[String]): Void = {
    val x: scala.Byte = 32
    val bit = x.to[Bit]
    val i8  = x.to[I8]
    val i16 = x.to[I16]
    val i32 = x.to[I32]
    val i64 = x.to[I64]
    val f32 = x.to[F32]
    val f64 = x.to[F64]
    val fix = x.to[MyFix]
    val flt = x.to[MyFlt]
    val num = number[MyFix](x)
    println(r"bit: $bit, i8: $i8, i16: $i16, i32: $i32, i64: $i64, fix: $fix")
    println(r"f32: $f32, f64: $f64, flt: $flt, num: $num")
    assert(i8 == 32)
    assert(i16 == 32)
    assert(i32 == 32)
    assert(i64 == 32)
    assert(f32 == 32)
    assert(f64 == 32)
    assert(fix == 32)
    assert(flt == 32)
    assert(num == 32)
  }
}

@test class LiftShort extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: scala.Short): A = x.to[A]

  def main(args: Array[String]): Void = {
    val x: scala.Short = 32
    val bit = x.to[Bit]
    val i8  = x.to[I8]
    val i16 = x.to[I16]
    val i32 = x.to[I32]
    val i64 = x.to[I64]
    val f32 = x.to[F32]
    val f64 = x.to[F64]
    val fix = x.to[MyFix]
    val flt = x.to[MyFlt]
    val num = number[MyFix](x)
    println(r"bit: $bit, i8: $i8, i16: $i16, i32: $i32, i64: $i64, fix: $fix")
    println(r"f32: $f32, f64: $f64, flt: $flt, num: $num")
    assert(i8 == 32)
    assert(i16 == 32)
    assert(i32 == 32)
    assert(i64 == 32)
    assert(f32 == 32)
    assert(f64 == 32)
    assert(fix == 32)
    assert(flt == 32)
    assert(num == 32)
  }
}

@test class LiftInt extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: scala.Int): A = x.to[A]

  def main(args: Array[String]): Void = {
    val x: scala.Int = 32
    val bit = x.to[Bit]
    val i8  = x.to[I8]
    val i16 = x.to[I16]
    val i32 = x.to[I32]
    val i64 = x.to[I64]
    val f32 = x.to[F32]
    val f64 = x.to[F64]
    val fix = x.to[MyFix]
    val flt = x.to[MyFlt]
    val num = number[MyFix](x)
    println(r"bit: $bit, i8: $i8, i16: $i16, i32: $i32, i64: $i64, fix: $fix")
    println(r"f32: $f32, f64: $f64, flt: $flt, num: $num")
    assert(i8 == 32)
    assert(i16 == 32)
    assert(i32 == 32)
    assert(i64 == 32)
    assert(f32 == 32)
    assert(f64 == 32)
    assert(fix == 32)
    assert(flt == 32)
    assert(num == 32)
  }
}

@test class LiftLong extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: scala.Long): A = x.to[A]

  def main(args: Array[String]): Void = {
    val x: scala.Long = 32
    val bit = x.to[Bit]
    val i8  = x.to[I8]
    val i16 = x.to[I16]
    val i32 = x.to[I32]
    val i64 = x.to[I64]
    val f32 = x.to[F32]
    val f64 = x.to[F64]
    val fix = x.to[MyFix]
    val flt = x.to[MyFlt]
    val num = number[MyFix](x)
    println(r"bit: $bit, i8: $i8, i16: $i16, i32: $i32, i64: $i64, fix: $fix")
    println(r"f32: $f32, f64: $f64, flt: $flt, num: $num")
    assert(i8 == 32)
    assert(i16 == 32)
    assert(i32 == 32)
    assert(i64 == 32)
    assert(f32 == 32)
    assert(f64 == 32)
    assert(fix == 32)
    assert(flt == 32)
    assert(num == 32)
  }
}

@test class LiftFloat extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: scala.Float): A = x.to[A]

  def main(args: Array[String]): Void = {
    val x = 32.0f
    val bit = x.to[Bit]
    val i8  = x.to[I8]
    val i16 = x.to[I16]
    val i32 = x.to[I32]
    val i64 = x.to[I64]
    val f32 = x.to[F32]
    val f64 = x.to[F64]
    val fix = x.to[MyFix]
    val flt = x.to[MyFlt]
    val num = number[MyFix](x)
    println(r"bit: $bit, i8: $i8, i16: $i16, i32: $i32, i64: $i64, fix: $fix")
    println(r"f32: $f32, f64: $f64, flt: $flt, num: $num")
    assert(i8 == 32)
    assert(i16 == 32)
    assert(i32 == 32)
    assert(i64 == 32)
    assert(f32 == 32)
    assert(f64 == 32)
    assert(fix == 32)
    assert(flt == 32)
    assert(num == 32)
  }
}

@test class LiftDouble extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: scala.Double): A = x.to[A]

  def main(args: Array[String]): Void = {
    val x = 32.0
    val bit = x.to[Bit]
    val i8  = x.to[I8]
    val i16 = x.to[I16]
    val i32 = x.to[I32]
    val i64 = x.to[I64]
    val f32 = x.to[F32]
    val f64 = x.to[F64]
    val fix = x.to[MyFix]
    val flt = x.to[MyFlt]
    val num = number[MyFix](x)
    println(r"bit: $bit, i8: $i8, i16: $i16, i32: $i32, i64: $i64, fix: $fix")
    println(r"f32: $f32, f64: $f64, flt: $flt, num: $num")
    assert(i8 == 32)
    assert(i16 == 32)
    assert(i32 == 32)
    assert(i64 == 32)
    assert(f32 == 32)
    assert(f64 == 32)
    assert(fix == 32)
    assert(flt == 32)
    assert(num == 32)
  }
}
