package spatial.lang.api

import argon._
import argon.node.TextConcat

import forge.tags._
import utils.implicits.collections._

trait DebuggingAPI_Internal extends argon.lang.api.DebuggingAPI_Internal 

trait DebuggingAPI_Shadowing extends DebuggingAPI_Internal
  with argon.lang.api.DebuggingAPI_Shadowing {
  this: StaticAPI_Shadowing =>

  // TODO: Does this still work?
  @api def sleep(cycles: I32): Void = Foreach(cycles by 1){_ =>  }

  /** Prints the given Array to the console, preceded by an optional heading. **/
  @virtualize
  @api def printArray[T:Type](array: Tensor1[T], header: Text = Text("")): Void = {
    println(header)
    (0 until array.length).foreach{i => print(array(i).toString + " ") }
    println("")
  }

  /** Prints the given Matrix to the console, preceded by an optional heading. **/
  @virtualize
  @api def printMatrix[T:Type](matrix: Tensor2[T], header: Text = Text("")): Void = {
    println(header)
    (0 until matrix.rows) foreach { i =>
      (0 until matrix.cols) foreach { j =>
        print(matrix(i, j).toString + "\t")
      }
      println("")
    }
  }

  /** Prints the given Tensor3 to the console, preceded by an optional heading. **/
  @virtualize
  @api def printTensor3[T:Type](tensor: Tensor3[T], header: Text = Text("")): Void = {
    println(header)
    (0 until tensor.dim0) foreach { i =>
      (0 until tensor.dim1) foreach { j =>
        (0 until tensor.dim2) foreach { k =>
          print(tensor(i, j, k).toString + "\t")
        }
        println("")
      }
      (0 until tensor.dim2) foreach {_ => print("--\t")}
      println("")
    }
  }

  /** Prints the given Tensor4 to the console, preceded by the an optional heading. **/
  @virtualize
  @api def printTensor4[T:Type](tensor: Tensor4[T], header: Text = Text("")): Void = {
    println(header)
    (0 until tensor.dim0) foreach { i =>
      (0 until tensor.dim1) foreach { j =>
        (0 until tensor.dim2) foreach { k =>
          (0 until tensor.dim3) foreach { l =>
            print(tensor(i, j, k, l).toString + "\t")
          }
          println("")
        }
        (0 until tensor.dim3) foreach {_ => print("--\t")}
        println("")
      }
      (0 until tensor.dim3) foreach {_ => print("--\t")}
      println("")
    }
  }

  /** Prints the given Tensor5 to the console, preceded by the an optional heading. **/
  @virtualize
  @api def printTensor5[T:Type](tensor: Tensor5[T], header: Text = Text("")): Void = {
    println(header)
    (0 until tensor.dim0) foreach { i =>
      (0 until tensor.dim1) foreach { j =>
        (0 until tensor.dim2) foreach { k =>
          (0 until tensor.dim3) foreach { l =>
            (0 until tensor.dim4) foreach { m =>
              print(tensor(i, j, k, l, m).toString + "\t")
            }
            println("")
          }
          (0 until tensor.dim4) foreach {_ => print("--\t")}
          println("")
        }
        (0 until tensor.dim4) foreach {_ => print("--\t")}
        println("")
      }
      (0 until tensor.dim4) foreach {_ => print("--\t")}
      println("")
    }
  }

  /** Prints the given SRAM1 to the console, preceded by an optional heading. **/
  @virtualize
  @api def printSRAM1[T:Type](array: SRAM1[T], header: Text = Text("")): Void = {
    println(header)
    Foreach(0 until array.length) { i => print(array(i).toString + " ") }
    println("")
  }

  /** Prints the given SRAM2 to the console, preceded by an optional heading. **/
  @virtualize
  @api def printSRAM2[T:Type](matrix: SRAM2[T], header: Text = Text("")): Void = {
    println(header)
    Foreach(0 until matrix.rows){ i =>
      Foreach(0 until matrix.cols){ j =>
        print(matrix(i, j).toString + "\t")
      }
      println("")
    }
  }

  /** Prints the given SRAM3 to the console, preceded by an optional heading. **/
  @virtualize
  @api def printSRAM3[T:Type](tensor: SRAM3[T], header: Text = Text("")): Void = {
    println(header)
    Foreach(0 until tensor.dim0) { i =>
      Foreach(0 until tensor.dim1) { j =>
        Foreach(0 until tensor.dim2) { k =>
          print(tensor(i, j, k).toString + "\t")
        }
        println("")
      }
      Foreach(0 until tensor.dim2) {_ => print("--\t")}
      println("")
    }
  }

  implicit class I32Range(x: Series[I32]) {
    @api def foreach(func: I32 => Void): Void = Foreach(x){i => func(i) }
  }


  implicit class Quoting(sc: StringContext) {
    @api def r(args: Any*): Text = {
      val quotedArgs = args.toArray.map{
        case t: Top[_] => t.toText
        case t         => Text(t.toString)
      }
      val quotedParts = sc.parts.map(Text.apply)

      val str = quotedParts.interleave(quotedArgs)
      stage(TextConcat(str))
    }
  }
}
