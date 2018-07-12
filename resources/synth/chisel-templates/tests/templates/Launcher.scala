// See LICENSE.txt for license details.
package templates

import types._
import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import utils.TemplateRunner
import scala.reflect.runtime._ 
import scala.reflect.runtime.universe
import scala.collection.immutable.HashMap
import templates.Utils._

// Ripped from http://stackoverflow.com/questions/1469958/scala-how-do-i-dynamically-instantiate-an-object-and-invoke-a-method-using-refl
object Inst {

  def apply(className: String, arg: Any) = {
    val runtimeMirror: universe.Mirror = universe.runtimeMirror(getClass.getClassLoader)
    val classSymbol: universe.ClassSymbol = runtimeMirror.classSymbol(Class.forName(s"templates.${className}"))
    val classMirror: universe.ClassMirror = runtimeMirror.reflectClass(classSymbol)
    if (classSymbol.companion.toString() == "<none>") {
      println(s"Info: $className has no companion object")
      val constructors = classSymbol.typeSignature.members.filter(_.isConstructor).toList
      if (constructors.length > 1) { 
        println(s"Info: $className has several constructors")
      } else {
        val constructorMirror = classMirror.reflectConstructor(constructors.head.asMethod) // we can reuse it
        constructorMirror()
      }
    } else {
      val companionSymbol = classSymbol.companion
      println(s"Info: $className has companion object $companionSymbol")
      // TBD
    }
  }
}

// Start args
object Arguments {
  val FixFMAAccum = List(
    (7.0, 6.0, true, 32, 0, 0.0),
    (4.0, 3.0, true, 16, 0, 0.0)
  )
  val FF = List(
    (16, XMap((0,0) -> (1, None))),
    (32, XMap((0,0) -> (1, None))),
    (64, XMap((0,0) -> (1, None)))
  )
  val TFF = List(
    "null"
  )
  val SRFF = List(
    "null"
  )
  val FIFO = List(
    (List(80), 16, List(1), XMap((0,0) -> (1, None)),XMap((0,0) -> (1, None))),
    (List(80), 16, List(5), XMap((0,0) -> (1, None)),XMap((0,0) -> (5, None))),
    (List(80), 16, List(8), XMap((0,0) -> (1, None)),XMap((0,0) -> (8, None))),
    (List(80), 16, List(2), XMap((0,0) -> (2, None)),XMap((0,0) -> (1, None))),
    (List(80), 16, List(5), XMap((0,0) -> (5, None)),XMap((0,0) -> (1, None))),
    (List(80), 16, List(6), XMap((0,0) -> (6, None)),XMap((0,0) -> (3, None))),
    (List(80), 16, List(2), XMap((0,0) -> (1, None)),XMap((0,0) -> (2, None)))
  )
  val SingleCounter = List(
    (1,None, None, None, None, 8),(3,None, None, None, None, 9)
  )
  val CompactingCounter = List(
    (1,8,16),(3,12,16),(8, 25,16)
  )
  val FixedPointTester = List(
    (false,16,16)
    // (true, 8, 8)
  )
  val Counter = List(
    (List(1,1,1), List(None, None, None),List(None, None, None),List(None, None, None),List(None, None, None), List(10,9,8)),
    (List(2,2,2), List(None, None, None),List(None, None, None),List(None, None, None),List(None, None, None), List(10,9,8)),
    (List(4,1,1), List(None, None, None),List(None, None, None),List(None, None, None),List(None, None, None), List(10,9,8))
  )
  val OuterControl = List(
    (Sequenced, 1, false),
    (Sequenced, 2, false),
    (Sequenced, 5, false),
    (Sequenced, 8, false),
    (Pipelined, 1, false),
    (Pipelined, 2, false),
    (Pipelined, 5, false),
    (Pipelined, 8, false),
    (ForkJoin, 1, false),
    (ForkJoin, 2, false),
    (ForkJoin, 5, false),
    (ForkJoin, 8, false)  
    // (1, Stream, false),
    // (2, Stream, false),
    // (5, Stream, false),
    // (8, Stream, false),
  )
  val InnerControl = List(
    (Sequenced, false, 32)
  )
  val PRNG = List(
    1,
    77
  )
  val Mem1D = List(
    4,
    50,
    1024
  )
  val ShiftRegFile = List(
           ( List(16,16), 32, 
             XMap((0,0) -> (1,None)), XMap((0,0) -> (1,None)),
             DMap(),  DMap()
           ),
           ( List(6,10), 32, 
             XMap((0,0) -> (1,Some(1))), XMap((0,0) -> (1,Some(1))),
             DMap(),  DMap()
           )
        )
  val SRAM = List(
           ( List(1,16), 32, 
             List(1,1), List(1,1),
             XMap(), XMap(),
             DMap((0,0) -> (List(Banks(0,0)),None)),  DMap((0,0) -> (List(Banks(0,0)),None)),
             BankedMemory ),
           ( List(1,32), 32, 
             List(1,2), List(1,1),
             XMap(), XMap(),
             DMap((0,0) -> (List(Banks(0,0), Banks(0,1)),None)),  DMap((0,0) -> (List(Banks(0,0),Banks(0,1)),None)),
             BankedMemory ),
           ( List(32,32), 32, 
             List(2,2), List(1,1),
             XMap(), XMap(),
             DMap((0,0) -> (List(Banks(0,0), Banks(0,1), Banks(1,0), Banks(1,1)), None)),  DMap((0,0) -> (List(Banks(0,0), Banks(0,1), Banks(1,0), Banks(1,1)), None)),
             BankedMemory )
        )
  val NBufMem = List( 
           ( SRAMType, List(8,12), 2, 32, 
             List(1,2), List(1,1),
             NBufXMap(), NBufXMap(),
             NBufDMap(0 -> DMap((0,0) -> (List(Banks(0,0),Banks(0,1)),None))),  NBufDMap(1 -> DMap((0,0) -> (List(Banks(0,0),Banks(0,1)),None))),
             XMap((0,0) -> (1, None)), XMap(),
             BankedMemory),
           ( SRAMType, List(8,12), 5, 32, 
             List(2,2), List(1,1),
             NBufXMap(), NBufXMap(),
             NBufDMap(0 -> DMap((0,0) -> (List(Banks(0,0), Banks(0,1), Banks(1,0), Banks(1,1)), None))),  NBufDMap(4 -> DMap((0,0) -> (List(Banks(0,0), Banks(0,1), Banks(1,0), Banks(1,1)), None))),
             XMap((0,0) -> (1,None)), XMap(),
             BankedMemory),
           ( FFType, List(1), 2, 32, 
             List(1), List(1),
             NBufXMap(0 -> XMap((0,0) -> (1, None))), NBufXMap(1 -> XMap((0,0) -> (1, None))),
             NBufDMap(), NBufDMap(),
             XMap((0,0) -> (1,None)), XMap(),
             BankedMemory),
           ( ShiftRegFileType, List(16,16), 4, 32, 
             List(16,16), List(1),
             NBufXMap(0 -> XMap((0,0) -> (1, None))), NBufXMap(3 -> XMap((0,0) -> (1, None))),
             NBufDMap(), NBufDMap(),
             XMap((0,0) -> (1, None)), XMap(),
             BankedMemory)
        )
  val SystolicArray2D = List(
    (List(7,6), List(2,2), List(0,1.0,1.0,0), List(0,0,0,1), None, Sum, 32, 0),
    (List(4,4), List(2,2), List(1.0,1.0,1.0,0), List(0,0,0,1), None, Sum, 32, 0),
    (List(4,4), List(2,2), List(0,1.0,1.0,1.0), List(0,0,0,1), None, MAC, 32, 0),
    (List(5,5), List(3,3), List(0,1.0,0,1.0,0,1.0,0,1.0,0), List(0,0,0,0,1,0,0,0,0), None, Sum, 32, 0),
    (List(5,5), List(3,2), List(1.0,1.0,1.0,0,0,1.0), List(0,0,0,0,1,0,0,0,0), None, Sum, 32, 0),
    (List(5,5), List(3,3), List(0,1.0,0,1.0,0,1.0,0,1.0,0), List(0,0,0,0,1,0,0,0,0), None, Max, 32, 0)
  )

  val LineBuffer = List( 
    (3,10,1,1,1,1,1,1,3,0,2),
    (3,10,1,1,2,2,2,1,3,0,2),
    (5,10,2,1,1,1,1,1,5,0,2),
    (5,10,2,2,1,1,1,1,5,0,2),
    (5,10,1,3,1,1,1,1,5,0,2),
    (5,10,1,3,1,1,1,1,5,1,2)
  )
}
// End args


object Launcher {
  var templates:Map[String,String => Boolean] = Map() 
  // val optionsManager = new chisel3.iotesters.TesterOptionsManager {
  //   testerOptions = testerOptions.copy(backendName = "verilator") 
  //   commonOptions = commonOptions.copy(topName = "pipe", targetDirName = "./seqpipe")
  //   firrtlOptions = firrtlOptions.copy()
  // }

  // Start launcher
  templates = templates ++ Arguments.FixFMAAccum.zipWithIndex.map{ case(arg,i) => 
    (s"FixFMAAccum$i" -> { (backendName: String) =>
    	Driver(() => new FixFMAAccum(arg), "verilator") {
          (c) => new FixFMAAccumTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.FF.zipWithIndex.map{ case(arg,i) => 
    (s"FF$i" -> { (backendName: String) =>
    	Driver(() => new FF(arg), "verilator") {
          (c) => new FFTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.TFF.zipWithIndex.map{ case(arg,i) => 
    (s"TFF$i" -> { (backendName: String) =>
    	Driver(() => new TFF(arg), "verilator") {
          (c) => new TFFTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.SRFF.zipWithIndex.map{ case(arg,i) => 
    (s"SRFF$i" -> { (backendName: String) =>
    	Driver(() => new SRFF(arg), "verilator") {
          (c) => new SRFFTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.FIFO.zipWithIndex.map{ case(arg,i) => 
    (s"FIFO$i" -> { (backendName: String) =>
    	Driver(() => new FIFO(arg), "verilator") {
          (c) => new FIFOTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.SingleCounter.zipWithIndex.map{ case(arg,i) => 
    (s"SingleCounter$i" -> { (backendName: String) =>
    	Driver(() => new SingleCounter(arg), "verilator") {
          (c) => new SingleCounterTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.CompactingCounter.zipWithIndex.map{ case(arg,i) => 
    (s"CompactingCounter$i" -> { (backendName: String) =>
    	Driver(() => new CompactingCounter(arg), "verilator") {
          (c) => new CompactingCounterTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.FixedPointTester.zipWithIndex.map{ case(arg,i) => 
    (s"FixedPointTester$i" -> { (backendName: String) =>
    	Driver(() => new FixedPointTester(arg), "verilator") {
          (c) => new FixedPointTesterTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.Counter.zipWithIndex.map{ case(arg,i) => 
    (s"Counter$i" -> { (backendName: String) =>
    	Driver(() => new Counter(arg), "verilator") {
          (c) => new CounterTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.OuterControl.zipWithIndex.map{ case(arg,i) => 
    (s"OuterControl$i" -> { (backendName: String) =>
    	Driver(() => new OuterControl(arg), "verilator") {
          (c) => new OuterControlTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.InnerControl.zipWithIndex.map{ case(arg,i) => 
    (s"InnerControl$i" -> { (backendName: String) =>
    	Driver(() => new InnerControl(arg), "verilator") {
          (c) => new InnerControlTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.PRNG.zipWithIndex.map{ case(arg,i) => 
    (s"PRNG$i" -> { (backendName: String) =>
    	Driver(() => new PRNG(arg), "verilator") {
          (c) => new PRNGTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.Mem1D.zipWithIndex.map{ case(arg,i) => 
    (s"Mem1D$i" -> { (backendName: String) =>
    	Driver(() => new Mem1D(arg), "verilator") {
          (c) => new Mem1DTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.ShiftRegFile.zipWithIndex.map{ case(arg,i) => 
    (s"ShiftRegFile$i" -> { (backendName: String) =>
    	Driver(() => new ShiftRegFile(arg), "verilator") {
          (c) => new ShiftRegFileTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.SRAM.zipWithIndex.map{ case(arg,i) => 
    (s"SRAM$i" -> { (backendName: String) =>
    	Driver(() => new SRAM(arg), "verilator") {
          (c) => new SRAMTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.NBufMem.zipWithIndex.map{ case(arg,i) => 
    (s"NBufMem$i" -> { (backendName: String) =>
    	Driver(() => new NBufMem(arg), "verilator") {
          (c) => new NBufMemTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.SystolicArray2D.zipWithIndex.map{ case(arg,i) => 
    (s"SystolicArray2D$i" -> { (backendName: String) =>
    	Driver(() => new SystolicArray2D(arg), "verilator") {
          (c) => new SystolicArray2DTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.LineBuffer.zipWithIndex.map{ case(arg,i) => 
    (s"LineBuffer$i" -> { (backendName: String) =>
    	Driver(() => new LineBuffer(arg), "verilator") {
          (c) => new LineBufferTests(c)
        }
      }) 
  }.toMap

// End launcher             

  def main(args: Array[String]): Unit = {
    TemplateRunner(templates, args)
  }
}

//lastline