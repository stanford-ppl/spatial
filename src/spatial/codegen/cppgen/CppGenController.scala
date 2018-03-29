package spatial.codegen.cppgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._


trait CppGenController extends CppGenCommon {

  var instrumentCounters: List[(Sym[_], Int)] = List()
  var earlyExits: List[Sym[_]] = List()

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {

    case AccelScope(func) =>
      controllerStack.push(lhs)
      // Skip everything inside
      instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
       // toggleEn()
      //  emitBlock(func)
      //  toggleEn()
      //  emit(s"// Register ArgIns and ArgIOs in case some are unused")
      //  emit(s"c1->setNumArgIns(${argIns.length} + ${drams.length} + ${argIOs.length});")
      //  emit(s"c1->setNumArgIOs(${argIOs.length});")
      //  emit(s"c1->setNumArgOuts(${argOuts.length});")
      //  emit(s"c1->setNumArgOutInstrs(2*${if (spatialConfig.enableInstrumentation) instrumentCounters.length else 0});")
      //  emit(s"c1->setNumEarlyExits(${earlyExits.length});")
      //  emit(s"""c1->flushCache(1024);""")
      //  emit(s"time_t tstart = time(0);")
      //  val memlist = if (setMems.nonEmpty) {s""", ${setMems.mkString(",")}"""} else ""
      //  emit(s"c1->run();")
      //  emit(s"time_t tend = time(0);")
      //  emit(s"double elapsed = difftime(tend, tstart);")
      //  emit(s"""std::cout << "Kernel done, test run time = " << elapsed << " ms" << std::endl;""")
      //  emit(s"""c1->flushCache(1024);""")
      //  controllerStack.pop()
 
      //  if (earlyExits.length > 0) {
      //    emit("// Capture breakpoint-style exits")
      //    emit("bool early_exit = false;")
      //    val numInstrs = if (spatialConfig.enableInstrumentation) {2*instrumentCounters.length} else 0
      //    earlyExits.zipWithIndex.foreach{ case (b, i) =>
      //      emit(src"long ${b}_act = c1->getArg(${argIOs.length + argOuts.length + numInstrs + i}, false);")
      //      val msg = b match {
      //        case Def(AssertIf(_,_,m)) => 
      //          val mm = m match {
      //            case Some(Const(message)) => s"${message}".replace("\"","'")
      //            case _ => "No Assert Message :("
      //          }
      //          s"""${b.ctx} - """ + s"""${mm}"""
      //        case _ => s"${b.ctx}"
      //      }
      //      emit(s"""if (${b}_act) {std::cout << "===================\\n  Breakpoint $i triggered!\\n    ${msg} \\n===================" << std::endl; early_exit = true;}  """)
      //    }
      //    emit("""if (!early_exit) {std::cout << "No breakpoints triggered :)" << std::endl;} """)
      //  }
 
      //  if (spatialConfig.enableInstrumentation) {
      //    emit(src"""std::ofstream instrumentation ("./instrumentation.txt");""")
 
      //    emit(s"// Need to instrument ${instrumentCounters}")
      //    val instrumentStart = argIOs.length + argOuts.length // These "invisible" instrumentation argOuts start after the full range of IR interface args
      //    emit(s"// Detected ${argOuts.length} argOuts and ${argIOs.length} argIOs, start instrument indexing at ${instrumentStart}")
      //    // In order to get niter / parent execution, we need to know the immediate parent of each controller and divide out that guy's niter
      //    val immediate_parent_niter_hashmap = mutable.HashMap[Int, Exp[_]]()
      //    instrumentCounters.zipWithIndex.foreach{case (c, i) => 
      //      immediate_parent_niter_hashmap.update(c._2, c._1)
      //      emit(s""" // immediate parent hashmap now ${immediate_parent_niter_hashmap}, current node ${c._1} is at depth ${c._2}""")
      //      val indent = "  "*c._2
      //      emit(s"""long ${c._1}_cycles = c1->getArg(${instrumentStart}+2*${i}, false);""")
      //      emit(s"""long ${c._1}_iters = c1->getArg(${instrumentStart}+2*${i}+1, false);""")
      //      val immediate_parent = if (immediate_parent_niter_hashmap.get(c._2-1).isDefined) immediate_parent_niter_hashmap.get(c._2-1).get else c._1
      //      emit(s"""long ${c._1}_iters_per_parent = ${c._1}_iters / std::max((long)1,${immediate_parent}_iters);""")
      //      emit(s"""long ${c._1}_avg = ${c._1}_cycles / std::max((long)1,${c._1}_iters);""")
      //      emit(s"""std::cout << "${indent}${c._1} - " << ${c._1}_avg << " (" << ${c._1}_cycles << " / " << ${c._1}_iters << ") [" << ${c._1}_iters_per_parent << " iters/parent execution]" << std::endl;""")
      //      open(s"if (instrumentation.is_open()) {")
      //        emit(s"""instrumentation << "${indent}${c._1} - " << ${c._1}_avg << " (" << ${c._1}_cycles << " / " << ${c._1}_iters << ") [" << ${c._1}_iters_per_parent << " iters/parent execution]" << std::endl;""")
      //      close("}")
      //    }
      //    emit(src"""instrumentation.close();""")
      //  }
      //  emit(src"// $lhs $reg $v $en reg write")
      
    case _ => super.gen(lhs, rhs)
  }

}
