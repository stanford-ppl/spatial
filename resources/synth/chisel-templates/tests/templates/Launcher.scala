// See LICENSE.txt for license details.
package templates

import types._
import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import utils.TemplateRunner
import scala.reflect.runtime._ 
import scala.reflect.runtime.universe
import scala.collection.mutable.HashMap

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
  val UIntAccum = List(
    (32, "add"),
    (32, "min"),
    (32, "max")
  )
  val SpecialAccum = List(
    (0,"add","UInt",List(32)),
    (1,"add","FixedPoint",List(1,16,16))
    // (18,"max","FloatingPoint",List(24,8))
  )
  val FF = List(
    (16, HashMap(0 -> 1)),
    (32, HashMap(0 -> 1)),
    (64, HashMap(0 -> 1))
  )
  val FFNoInit = List(
    32
  )
  val FFNoInitNoReset = List(
    32
  )
  val FFNoReset = List(
    32
  )
  val TFF = List(
    "null"
  )
  val SRFF = List(
    "null"
  )
  val FIFO = List(
    (List(80), 16, List(1), HashMap(0 -> 1),HashMap(0 -> 1)),
    (List(80), 16, List(5), HashMap(0 -> 1),HashMap(0 -> 5)),
    (List(80), 16, List(8), HashMap(0 -> 1),HashMap(0 -> 8)),
    (List(80), 16, List(2), HashMap(0 -> 2),HashMap(0 -> 1)),
    (List(80), 16, List(5), HashMap(0 -> 5),HashMap(0 -> 1)),
    (List(80), 16, List(6), HashMap(0 -> 6),HashMap(0 -> 3)),
    (List(80), 16, List(2), HashMap(0 -> 1),HashMap(0 -> 2))
  )
  val FILO = List(
    (1,1,10,1,1),
    (2,2,30,1,1),
    (4,4,52,1,1),
    (4,1,56,1,1),
    (1,4,56,1,1),
    (3,6,48,1,1),
    (6,3,48,1,1)
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
  val OuterController = List(
    (Sequential, 1, false),
    (Sequential, 2, false),
    (Sequential, 5, false),
    (Sequential, 8, false),
    (Pipeline, 1, false),
    (Pipeline, 2, false),
    (Pipeline, 5, false),
    (Pipeline, 8, false),
    (ForkJoin, 1, false),
    (ForkJoin, 2, false),
    (ForkJoin, 5, false),
    (ForkJoin, 8, false)  
    // (1, Stream, false),
    // (2, Stream, false),
    // (5, Stream, false),
    // (8, Stream, false),
  )
  val InnerController = List(
    (Sequential, false, 32)
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
  val SRAM = List(
           ( List(1,16), 32, 
             List(1,1), List(1,1),
             HashMap[Int,Int](), HashMap[Int,Int](),
             HashMap(0 -> List(List(0,0))),  HashMap(0 -> List(List(0,0))),
             BankedMemory ),
           ( List(1,32), 32, 
             List(1,2), List(1,1),
             HashMap[Int,Int](), HashMap[Int,Int](),
             HashMap(0 -> List(List(0,0), List(0,1))),  HashMap(0 -> List(List(0,0),List(0,1))),
             BankedMemory ),
           ( List(32,32), 32, 
             List(2,2), List(1,1),
             HashMap[Int,Int](), HashMap[Int,Int](),
             HashMap(0 -> List(List(0,0), List(0,1), List(1,0), List(1,1))),  HashMap(0 -> List(List(0,0), List(0,1), List(1,0), List(1,1))),
             BankedMemory )
        )
  val NBufMem = List( 
           ( SRAMType, List(8,12), 2, 32, 
             List(1,2), List(1,1),
             HashMap[Int, HashMap[Int,Int]](), HashMap[Int, HashMap[Int,Int]](),
             HashMap(0 -> HashMap(0 -> List(List(0,0), List(0,1)))),  HashMap(1 -> HashMap(0 -> List(List(0,0),List(0,1)))),
             HashMap(0 -> 1),
             BankedMemory),
           ( SRAMType, List(8,12), 5, 32, 
             List(2,2), List(1,1),
             HashMap[Int, HashMap[Int,Int]](), HashMap[Int, HashMap[Int,Int]](),
             HashMap(0 -> HashMap(0 -> List(List(0,0), List(0,1), List(1,0), List(1,1)))),  HashMap(4 -> HashMap(0 -> List(List(0,0), List(0,1), List(1,0), List(1,1)))),
             HashMap(0 -> 1),
             BankedMemory),
           ( FFType, List(1), 2, 32, 
             List(1), List(1),
             HashMap(0 -> HashMap(0 -> 1)), HashMap(1 -> HashMap(0 -> 1)),
             HashMap[Int, HashMap[Int,List[List[Int]]]](), HashMap[Int, HashMap[Int,List[List[Int]]]](),
             HashMap(0 -> 1),
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
  val ShiftRegFile = List(
    (List(6),List(1),6,None,1,1,false,1,32,0),
    (List(6),List(1),6,None,2,1,false,1,32,0),
    (List(21),List(1),21,None,4,1,false,1,32,0),
    (List(1,3),List(1),3,None,1,1,false,1,32,0),
    (List(4,12),List(4),12,None,4,4,false,1,32,0),
    (List(3,7),List(3),7,None,1,3,false,1,32,0)
  )

  // Need to retest     (List(3,3,5),List(1),None,2,9,false,32,0) above
  
  val NBufShiftRegFile = List(
    (List(8),List(1),8,None,1,2,Map((0->1)),List(1,1),32,0),
    (List(8),List(1),8,None,2,2,Map((0->1)),List(1,1),32,0),
    (List(1,8),List(1),8,None,1,3,Map((0->1)),List(1,1,1),32,0),
    (List(3,7),List(3),7,None,1,3,Map((0->3)),List(1,1,1),32,0),
    (List(3,9),List(3),9,None,3,3,Map((0->3)),List(1,1,1),32,0),
    (List(3,3),List(3),3,None,1,3,Map((0->3)),List(1,1,1),32,0)
  )

    // (List(3,4,7),None,1,12,Map((0->12)),32,0)

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
  // templates = templates ++ Arguments.UIntAccum.zipWithIndex.map{ case(arg,i) => 
  //   (s"UIntAccum$i" -> { (backendName: String) =>
  //     Driver.execute(() => new UIntAccum(arg), optionsManager) {
  //         (c) => new UIntAccumTests(c)
  //       }
  //     }) 
  // }.toMap

  // Start launcher
  templates = templates ++ Arguments.UIntAccum.zipWithIndex.map{ case(arg,i) => 
    (s"UIntAccum$i" -> { (backendName: String) =>
    	Driver(() => new UIntAccum(arg), "verilator") {
          (c) => new UIntAccumTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.SpecialAccum.zipWithIndex.map{ case(arg,i) => 
    (s"SpecialAccum$i" -> { (backendName: String) =>
    	Driver(() => new SpecialAccum(arg), "verilator") {
          (c) => new SpecialAccumTests(c)
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

  templates = templates ++ Arguments.FFNoInit.zipWithIndex.map{ case(arg,i) => 
    (s"FFNoInit$i" -> { (backendName: String) =>
    	Driver(() => new FFNoInit(arg), "verilator") {
          (c) => new FFNoInitTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.FFNoInitNoReset.zipWithIndex.map{ case(arg,i) => 
    (s"FFNoInitNoReset$i" -> { (backendName: String) =>
    	Driver(() => new FFNoInitNoReset(arg), "verilator") {
          (c) => new FFNoInitNoResetTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.FFNoReset.zipWithIndex.map{ case(arg,i) => 
    (s"FFNoReset$i" -> { (backendName: String) =>
    	Driver(() => new FFNoReset(arg), "verilator") {
          (c) => new FFNoResetTests(c)
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

  templates = templates ++ Arguments.FILO.zipWithIndex.map{ case(arg,i) => 
    (s"FILO$i" -> { (backendName: String) =>
    	Driver(() => new FILO(arg), "verilator") {
          (c) => new FILOTests(c)
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

  templates = templates ++ Arguments.OuterController.zipWithIndex.map{ case(arg,i) => 
    (s"OuterController$i" -> { (backendName: String) =>
    	Driver(() => new OuterController(arg), "verilator") {
          (c) => new OuterControllerTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.InnerController.zipWithIndex.map{ case(arg,i) => 
    (s"InnerController$i" -> { (backendName: String) =>
    	Driver(() => new InnerController(arg), "verilator") {
          (c) => new InnerControllerTests(c)
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

  templates = templates ++ Arguments.ShiftRegFile.zipWithIndex.map{ case(arg,i) => 
    (s"ShiftRegFile$i" -> { (backendName: String) =>
    	Driver(() => new ShiftRegFile(arg), "verilator") {
          (c) => new ShiftRegFileTests(c)
        }
      }) 
  }.toMap

  templates = templates ++ Arguments.NBufShiftRegFile.zipWithIndex.map{ case(arg,i) => 
    (s"NBufShiftRegFile$i" -> { (backendName: String) =>
    	Driver(() => new NBufShiftRegFile(arg), "verilator") {
          (c) => new NBufShiftRegFileTests(c)
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