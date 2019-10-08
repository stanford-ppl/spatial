package spatial.codegen.surfgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.util.spatialConfig

trait SurfGenAccel extends SurfGenCommon {

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
      emit(s"""print("Finished executing TopHost.py for ${spatialConfig.name}!""")


       if (earlyExits.nonEmpty) {
         emit("# Capture breakpoint-style exits")
         emit("early_exit = False;")
         emit("# early exits code TBD...")
       }

       if (spatialConfig.enableInstrumentation) {
         emit("# instrumentation hooks TBD...")
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
      val enable = if (en.toList.isEmpty) "true" else en.map(quote).mkString("&")
      emit(src"""if ($enable): assert($cond), "%s" % $lhs""")
      if (inHw) earlyExits = earlyExits :+ lhs

    case BreakpointIf(_) =>
      // Emits will only happen if outside the accel
      emit(src"exit(1);")
      if (inHw) earlyExits = earlyExits :+ lhs

    case _ => super.gen(lhs, rhs)
  }

}
