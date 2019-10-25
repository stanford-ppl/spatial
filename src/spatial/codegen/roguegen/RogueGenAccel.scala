package spatial.codegen.roguegen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.util.spatialConfig

trait RogueGenAccel extends RogueGenCommon {

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {

    case AccelScope(func) =>
      controllerStack.push(lhs)
      // Skip everything inside
      instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
      inAccel{
        visitBlock(func)
      }
      controllerStack.pop()
      emit(s"done = accel.Done.get()")
      emit(s"ctr = 0")
      emit(s"accel.Enable.set(1)")
      open("while (done == 0):")
        emit("done = accel.Done.get()")
        emit("time.sleep(0.01)")
        emit("ctr = ctr + 1")
        emit("""if (ctr % 75 == 0): print("  Polled flag %d times..." % ctr)""")
      close("")
      emit(s"""print("Finished executing TopHost.py for ${spatialConfig.name}!") """)

       if (earlyExits.nonEmpty) {
         emit("# Capture breakpoint-style exits")
         emit("early_exit = False;")
         emit("# early exits code TBD...")
       }


       if (spatialConfig.enableInstrumentation) {
         emit(src"""instrumentation = open('./instrumentation.txt','w+')""")

         emit(s"# Need to instrument $instrumentCounters")
         // In order to get niter / parent execution, we need to know the immediate parent of each controller and divide out that guy's niter
         val immediate_parent_niter_hashmap = scala.collection.mutable.HashMap[Int, Sym[_]]()
         val ranWithArgIns = if (argIns.nonEmpty) argIns.mkString(" << \" \" << ") else " \" \" "
         val ranWithArgIOs = if (argIOs.nonEmpty) argIOs.mkString(" << \" \" << ") else " \" \" "
         emit(s"""print('ArgIns: %d, ArgIOs: %d' % (1,2)) #($ranWithArgIns, $ranWithArgIOs))""")

         instrumentCounters.foreach{c =>
           immediate_parent_niter_hashmap.update(c._2, c._1)
           emit(s""" # immediate parent hashmap now $immediate_parent_niter_hashmap, current node ${c._1} is at depth ${c._2}""")
           val indent = "  "*c._2
           emit(s"""${c._1}_cycles = accel.${quote(c._1).toUpperCase}_cycles_arg.get()""")
           emit(src"time.sleep(0.0001)")
           emit(s"""${c._1}_iters = accel.${quote(c._1).toUpperCase}_iters_arg.get()""")
           emit(src"time.sleep(0.0001)")
           val immediate_parent = if (immediate_parent_niter_hashmap.get(c._2-1).isDefined) immediate_parent_niter_hashmap(c._2 - 1) else c._1
           emit(s"""${c._1}_iters_per_parent = ${c._1}_iters / max(1,${immediate_parent}_iters)""")
           emit(src"time.sleep(0.0001)")
           emit(s"""${c._1}_avg = ${c._1}_cycles / max(1,${c._1}_iters)""")
           emit(src"time.sleep(0.0001)")
           emit(s"""print('$indent${c._1} - %d ( %d / %d ) [%d iters/parent execution]' % (${c._1}_avg,${c._1}_cycles,${c._1}_iters,${c._1}_iters_per_parent), end='')""")
           emit(s"""#instrumentation.write('$indent${c._1} - %d ( %d / %d ) [%d iters/parent execution]' % (${c._1}_avg,${c._1}_cycles,${c._1}_iters,${c._1}_iters_per_parent), end='')""")
           if (hasBackPressure(c._1.toCtrl) || hasForwardPressure(c._1.toCtrl)) {
             emit(s"""${c._1}_stalled = accel.${quote(c._1).toUpperCase}_stalled_arg.get()""")
             emit(s"""${c._1}_idle = accel.${quote(c._1).toUpperCase}_idle_arg.get()""")
             emit(s"""print(' <# stalled: %d, #idle: %d>' % (${c._1}_stalled,${c._1}_idle), end='')""")
             emit(s"""#instrumentation.write(' <# stalled: %d, #idle: %d>' % (${c._1}_stalled,${c._1}_idle), end='')""")
           }
           emit("print('')")
           emit("instrumentation.write('')")
         }
         emit(src"""instrumentation.close();""")
       }

    case UnitPipe(_,func,_) =>
      controllerStack.push(lhs)
      if (inHw) instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
      visitBlock(func)
      controllerStack.pop()

    case UnrolledForeach(_, _,func, _, _,_) if inHw =>
      controllerStack.push(lhs)
      if (inHw) instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
      visitBlock(func)
      controllerStack.pop()

    case UnrolledReduce(_, _,func, _, _,_) =>
      controllerStack.push(lhs)
      if (inHw) instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
      visitBlock(func)
      controllerStack.pop()

    case ParallelPipe(_,func) =>
      controllerStack.push(lhs)
      if (inHw) instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
      visitBlock(func)
      controllerStack.pop()

    case Switch(_, body) if inHw =>
      controllerStack.push(lhs)
      if (inHw) instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
      visitBlock(body)
      controllerStack.pop()

    case SwitchCase(body) if inHw =>
      controllerStack.push(lhs)
      if (inHw) instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
      visitBlock(body)
      controllerStack.pop()

    case StateMachine(_, _,notDone,action,nextState) =>
      controllerStack.push(lhs)
      if (inHw) instrumentCounters = instrumentCounters :+ (lhs, controllerStack.length)
      visitBlock(notDone)
      visitBlock(action)
      visitBlock(nextState)
      controllerStack.pop()

    case ExitIf(_) =>
      // Emits will only happen if outside the accel
      emit(src"exit(1);")
      if (inHw) earlyExits = earlyExits :+ lhs

    case AssertIf(en, cond, m) =>
      // Emits will only happen if outside the accel
      val str = src"""${m.getOrElse("\"API assert failed with no message provided\"")}"""
      emit(src"""$lhs = ("\n=================\n" + ($str + "\n=================\n"));""")
      val enable = if (en.toList.isEmpty) "True" else en.map(quote).mkString("&")
      emit(src"""if ($enable): assert($cond), "%s" % $lhs""")
      if (inHw) earlyExits = earlyExits :+ lhs

    case BreakpointIf(_) =>
      // Emits will only happen if outside the accel
      emit(src"exit(1);")
      if (inHw) earlyExits = earlyExits :+ lhs

    case _ => super.gen(lhs, rhs)
  }

}
