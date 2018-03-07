package nova.test

import spatial.dsl._
import utest._

@spatial object LiftBoolean {
  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: Boolean): A = x.to[A]

  def main(): Void = {
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
  }
}

@spatial object LiftByte {
  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: Byte): A = x.to[A]

  def main(): Void = {
    val x: Byte = 32
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
  }
}

@spatial object LiftShort {
  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: Short): A = x.to[A]

  def main(): Void = {
    val x: Short = 32
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
  }
}

@spatial object LiftInt {
  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: Int): A = x.to[A]

  def main(): Void = {
    val x: Int = 32
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
  }
}

@spatial object LiftLong {
  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: Long): A = x.to[A]

  def main(): Void = {
    val x: Long = 32
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
  }
}

@spatial object LiftFloat {
  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: Float): A = x.to[A]

  def main(): Void = {
    val x: Float = 32
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
  }
}

@spatial object LiftDouble {
  type MyFix = FixPt[TRUE,_16,_16]
  type MyFlt = FltPt[_32,_32]

  def number[A:Num](x: Double): A = x.to[A]

  def main(): Void = {
    val x: Double = 32
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
  }
}



object LiftTests extends Testbench { val tests = Tests {
  'LiftBoolean - test(LiftBoolean)
  'LiftByte - test(LiftByte)
  'LiftShort - test(LiftShort)
  'LiftInt - test(LiftInt)
  'LiftLong - test(LiftLong)
  'LiftFloat - test(LiftFloat)
  'LiftDouble - test(LiftDouble)
}}
