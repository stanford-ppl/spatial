package spatial.node

import argon._
import forge.tags._
import spatial.lang._
import spatial.lang.host._

@op case class InputArguments() extends Op[Array[Text]]

@op case class ArrayNew[A:Type](size: I32) extends Op2[A,Array[A]] {
  override def effects: Effects = Effects.Mutable
}

@op case class ArrayFromSeq[A:Type](seq: Seq[Sym[A]]) extends Op2[A,Array[A]]

@op case class ArrayLength[A:Type](array: Array[A]) extends Op[I32]

@op case class ArrayApply[A:Type](coll: Array[A], i: I32) extends Op[A] with AtomicRead[Array[A]] {
  override def aliases = Nul
}

@op case class ArrayUpdate[A:Type](array: Array[A], i: I32, data: Sym[A]) extends Op[Void] {
  override def effects: Effects = Effects.Writes(array)
  override def contains = syms(data)
}

@op case class MapIndices[A:Type](
    size: I32,
    func: Lambda1[I32,A])
  extends Op2[A,Array[A]] {

  override def aliases: Set[Sym[_]] = Nul
  override def binds = super.binds + func.input
}

@op case class ArrayForeach[A:Type](
    array: Array[A],
    apply: Lambda2[Array[A],I32,A],
    func:  Lambda1[A,Void])
  extends Op2[A,Void] {

  override def binds = super.binds + apply.inputB
}

@op case class ArrayMap[A:Type,B:Type](
    array: Array[A],
    apply: Lambda2[Array[A],I32,A],
    func:  Lambda1[A,B])
  extends Op3[A,B,Array[B]] {
  override def aliases = Nul
  override def binds = super.binds + apply.inputB
}

@op case class ArrayZip[A:Type,B:Type,C:Type](
    arrayA: Array[A],
    arrayB: Array[B],
    applyA: Lambda2[Array[A],I32,A],
    applyB: Lambda2[Array[B],I32,B],
    func:   Lambda2[A,B,C])
  extends Op4[A,B,C,Array[C]] {
  override def aliases = Nul
  override def binds = super.binds + applyA.inputB
}

@op case class ArrayReduce[A:Type](
    array:  Array[A],
    apply:  Lambda2[Array[A],I32,A],
    reduce: Lambda2[A,A,A])
  extends Op[A] {

  override def aliases: Set[Sym[_]] = Nul
  override def binds = super.binds ++ Set(apply.inputB,reduce.inputA,reduce.inputB)
}

//@op case class ArrayGroupByReduce[A:Type, K:Type, B:Type](
    //array:  Array[A],
    //apply:  Lambda2[Array[A],I32,A],
    //key:    Lambda1[A,K],
    //map:    Lambda1[A,B],
    //reduce: Lambda2[B,B,B])
  //extends Op[Map[K,B]] {

  //override def aliases: Set[Sym[_]] = Nul
  //override def binds = super.binds ++ Set(apply.inputB,key.input,map.input,reduce.inputA,reduce.inputB)
//}

@op case class ArrayFold[A:Type](
    array:  Array[A],
    init:   Sym[A],
    apply:  Lambda2[Array[A],I32,A],
    reduce: Lambda2[A,A,A])
  extends Op[A] {

  override def aliases: Set[Sym[_]] = Nul
  override def binds = super.binds + apply.inputB
}

@op case class ArrayFilter[A:Type](
    array: Array[A],
    apply: Lambda2[Array[A],I32,A],
    cond:  Lambda1[A,Bit])
  extends Op2[A,Array[A]] {
  override def aliases = Nul
  override def binds = super.binds + apply.inputB
}

@op case class ArrayFlatMap[A:Type,B:Type](
    array: Array[A],
    apply: Lambda2[Array[A],I32,A],
    func:  Lambda1[A,Array[B]])
  extends Op3[A,B,Array[B]] {
  override def aliases = Nul
  override def binds = super.binds + apply.inputB
}

@op case class ArrayMkString[A:Type](
    array: Array[A],
    start: Text,
    delim: Text,
    end:   Text)
  extends Op2[A,Text]

@op case class CharArrayToText(array: Tensor1[U8]) extends Op[Text]
