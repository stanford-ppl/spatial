package spatial.test.feature

import spatial.dsl._
import spatial.test.Testbench


@spatial object LiftBoolean {
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
    println("PASS: true")
  }
}

@spatial object LiftByte {
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
    println("PASS: true")
  }
}

@spatial object LiftShort {
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
    println("PASS: true")
  }
}

@spatial object LiftInt {
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
    println("PASS: true")
  }
}

@spatial object LiftLong {
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
    println("PASS: true")
  }
}

@spatial object LiftFloat {
  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: scala.Float): A = x.to[A]

  def main(args: Array[String]): Void = {
    val x = 32
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
    println("PASS: true")
  }
}

@spatial object LiftDouble {
  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: scala.Double): A = x.to[A]

  def main(args: Array[String]): Void = {
    val x = 32
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
    println("PASS: true")
  }
}



class Lifting extends Testbench {
  test(LiftBoolean)
  test(LiftByte)
  test(LiftShort)
  test(LiftInt)
  test(LiftLong)
  test(LiftFloat)
  test(LiftDouble)
}
