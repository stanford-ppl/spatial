package spatial.node

import argon._
import forge.tags._
import spatial.lang._

@op case class OpenCSVFile(filename: Text, write: Boolean) extends Op[CSVFile] {
  override def effects: Effects = Effects.Mutable
}

@op case class CloseCSVFile(file: CSVFile) extends Op[Void] {
  override def effects: Effects = Effects.Writes(file)
}

@op case class ReadTokens(file: CSVFile, delim: Text) extends Op[Tensor1[Text]]

@op case class WriteTokens(
    file:  CSVFile,
    delim: Text,
    len:   I32,
    token: Lambda1[I32,Text])
  extends Op[Void] {
  override def effects: Effects = Effects.Writes(file)
}



@op case class OpenBinaryFile(filename: Text, write: Boolean) extends Op[BinaryFile] {
  override def effects: Effects = Effects.Mutable
}

@op case class CloseBinaryFile(file: BinaryFile) extends Op[Void]

@op case class ReadBinaryFile[A:Num](file: BinaryFile) extends Op2[A,Tensor1[A]] {
  override val A: Num[A] = Num[A]
}

@op case class WriteBinaryFile[A:Num](
    file:  BinaryFile,
    len:   I32,
    value: Lambda1[I32, A])
  extends Op2[A,Void]{
  override val A: Num[A] = Num[A]
  override def effects: Effects = Effects.Writes(file)
}


@op case class NumpyArray[A:Type](v: String) extends Op[Tensor1[A]]

@op case class NumpyMatrix[A:Type](v: String) extends Op[Tensor2[A]]

