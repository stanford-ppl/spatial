package spatial.util

import core._
import forge.tags._
import spatial.lang._
import spatial.data._

trait UtilsIR {
  /** Returns the number of bits of data the given symbol represents. */
  def nbits(e: Sym[_]): Int = e.tp match {case Bits(bT) => bT.nbits; case _ => 0 }


  @stateful def canMotion(stms: Seq[Sym[_]]): Boolean = {
    stms.forall{s => !takesEnables(s) && effectsOf(s).isIdempotent }
  }
  @stateful def shouldMotion(stms: Seq[Sym[_]], inHw: Boolean): Boolean = {
    canMotion(stms) && (inHw || stms.length == 1)
  }

  implicit class SymUtils[A](x: Sym[A]) {
    def consumers: Set[Sym[_]] = consumersOf(x)

    def isIdx:  Boolean = x.tp match {
      case FixPtType(_,_,0) => true
      case _ => false
    }
    def isNum:  Boolean = x.isInstanceOf[Num[_]]
    def isBits: Boolean = x.isInstanceOf[Bits[_]]
    def isVoid: Boolean = x.isInstanceOf[Void]

    def nestedInputs: Set[Sym[_]] = {
      x.inputs.toSet ++ x.op.map{o =>
        val outs = o.blocks.flatMap(_.nestedStms)
        val used = outs.flatMap{s => s.nestedInputs }
        used diff outs
      }.getOrElse(Set.empty)
    }

    def getNodesBetween(y: Sym[_], scope: Set[Sym[_]]): Set[Sym[_]] = {
      def dfs(frontier: Seq[Sym[_]], nodes: Set[Sym[_]]): Set[Sym[_]] = frontier.toSet.flatMap{x: Sym[_] =>
        if (scope.contains(x)) {
          if (x == y) nodes + x
          else dfs(x.allDeps, nodes + x)
        }
        else Set.empty[Sym[_]]
      }
      dfs(Seq(x),Set(x))
    }
  }

  implicit class ParamHelpers(x: Sym[_]) {
    def toInt: Int = x match {
      case Expect(c) => c
      case _ => throw new Exception(s"Cannot convert symbol $x to a constant Int")
    }
  }
}
