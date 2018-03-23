package argon.codegen.cppgen

import argon.core._
import argon.nodes._
import argon.emul.FixedPoint

trait CppGenBool extends CppCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case BooleanType => "bool"
    case _ => super.remap(tp)
  }

  override protected def quoteConst(c: Const[_]): String = c match {
    case Const(c: Boolean) => c.toString
    case _ => super.quoteConst(c)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case Not(x)       => emit(src"bool $lhs = !$x;")
    case And(x,y)     => emit(src"bool $lhs = $x && $y;")
    case Or(x,y)      => emit(src"bool $lhs = $x || $y;")
    case XOr(x,y)     => emit(src"bool $lhs = $x != $y;")
    case XNor(x,y)    => emit(src"bool $lhs = $x == $y;")
    case RandomBoolean(x) => emit(src"bool $lhs = java.util.concurrent.ThreadLocalRandom.current().nextBoolean();")
    case StringToBoolean(x) => 
      emit(src"bool $lhs = atoi(${x}.c_str()) != 0;")
      x match {
        case Def(ArrayApply(array, i)) => 
          array match {
            case Def(InputArguments()) => 
              val ii = i match {case c: Const[_] => c match {case Const(c: FixedPoint) => c.toInt; case _ => -1}; case _ => -1}
              if (cliArgs.contains(ii)) cliArgs += (ii -> s"${cliArgs(ii)} / ${lhs.name.getOrElse(s"${lhs.ctx}")}")
              else cliArgs += (ii -> lhs.name.getOrElse(s"${lhs.ctx}"))
            case _ =>
          }
        case _ =>          
      }

    case _ => super.emitNode(lhs, rhs)
  }
}
