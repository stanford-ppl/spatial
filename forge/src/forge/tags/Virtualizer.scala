package forge.tags

import utils.tags.MacroUtils

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
  * Converts Scala features that can not be overridden to method calls that can be given
  * arbitrary semantics.
  *
  * === Covered features ===
  * {{{
  *   val x = e              =>       val x = e; __valName(x, "x")
  *   var x = e              =>       var x = __newVar(e)
  *   if (c) t else e        =>       __ifThenElse(c, t, e)
  *   return t               =>       __return(t)
  *   x = t                  =>       __assign(x, t)
  *   while(c) b             =>       __whileDo(c, b)
  *   do b while c           =>       __doWhile(c, b)
  * }}}
  *
  * === Infix methods for `Any` methods ===
  * {{{
  *   t == t1                =>       infix_==(t, t1)
  *   t != t1                =>       infix_!=(t, t1)
  *   t.##                   =>       infix_##(t, t1)
  *   t.equals t1            =>       infix_equals(t, t1)
  *   t.hashCode             =>       infix_hashCode(t)
  *   t.asInstanceOf[T]      =>       infix_asInstanceOf[T](t)
  *   t.isInstanceOf[T]      =>       infix_isInstanceOf[T](t)
  *   t.toString             =>       infix_toString(t)
  *   t.getClass             =>       infix_getClass(t)
  * }}}
  *
  * === Infix methods for `AnyRef` methods ===
  * {{{
  *   t eq t1                =>       infix_eq(t, t1)
  *   t ne t1                =>       infix_ne(t, t1)
  *   t.clone                =>       infix_clone(t)
  *   t.notify               =>       infix_notify(t)
  *   t.notifyAll            =>       infix_notifyAll(t)
  *   t.synchronized[T](t1)  =>       infix_synchronized(t, t1)
  *   t.wait                 =>       infix_wait(t)
  *   t.wait(l)              =>       infix_wait(t, l)
  *   t.wait(t1, l)          =>       infix_wait(t, t1, l)
  *   t.finalize()           =>       infix_finalize(t)
  * }}}
  *
  * @todo
  * {{{
  *   try b catch c          =>       __tryCatch(b, c, f)
  *   throw e                =>       __throw(e)
  *   Nothing                =>       ???
  *   Null                   =>       ???
  * }}}
  *
  * === Unplanned/Unsupported Features ===
  * {{{
  *   a += b    Use implicit classes instead
  *   a -= b
  *   a *= b
  *   a /= b
  * }}}
  *
  */
class Virtualizer[Ctx <: blackbox.Context](override val __c: Ctx) extends MacroUtils[Ctx](__c) {
  import __c.universe._

  def virtualize(t: Tree): List[Tree] = VirtualizationTransformer(t)

  private object VirtualizationTransformer {
    def apply(tree: Tree) = new VirtualizationTransformer().apply(tree)
  }

  private class VirtualizationTransformer extends Transformer {
    def apply(tree: __c.universe.Tree): List[Tree] = transformStm(tree)

    def call(receiver: Option[Tree], name: String, args: List[Tree], targs: List[Tree] = Nil): Tree = {
      methodCall(receiver.map(transform), name, List(args.map(transform)), targs)
    }

    /** Call for transforming Blocks.
      *
      * Allows expanding a single statement to multiple statements within a given scope.
      */
    private def transformStm(tree: Tree): List[Tree] = tree match {
      case ValDef(mods, term@TermName(name), tpt, rhs) if mods.hasFlag(Flag.MUTABLE) && !mods.hasFlag(Flag.PARAMACCESSOR) =>
        tpt match {
          case EmptyTree =>
            __c.abort(__c.enclosingPosition, "Missing type for var declaration.")
          case _ =>
        }
        // Mangle Var name to make readVar calls happen explicitly
        val s = TermName(name+"$v")
        val vtyp = tq"forge.VarLike[$tpt]"
        val asgn = TermName(name+"_=")
        // leaving it a var makes it easier to revert when custom __newVar isn't supplied
        val v = ValDef(mods, s, vtyp, call(None, "__newVar", List(rhs)))
        val d = DefDef(mods, term, Nil, Nil, tpt, call(None, "__readVar", List(Ident(s))))
        val a = q"$mods def $asgn(v: $tpt) = __assign($s, v)"

        //DefDef(mods, asgn, Nil, Nil, EmptyTree, call(None, "__assign", List(Ident(s))))
        List(v, d, a)

      case v@ValDef(mods, term@TermName(name), _, _) if !mods.hasFlag(Flag.PARAMACCESSOR) =>
        val vdef = transform(v)
        val regv = methodCall(None, "__valName", List(List(Ident(term), Literal(Constant(name)))),Nil)
        List(vdef, regv)

      case _ => List(transform(tree))
    }


    override def transform(tree: Tree): Tree = atPos(tree.pos) {
      tree match {
        /* Attempt to stage vars in both class bodies and blocks **/
        case Template(parents, selfType, bodyList) =>
          val body = bodyList.flatMap(transformStm)

          Template(parents, selfType, body)

        case Block(stms, ret) =>
          val stms2 = stms.flatMap(transformStm) ++ transformStm(ret)
          Block(stms2.dropRight(1), stms2.last)


        case Function(params,body) =>
          val named = params.collect{case ValDef(_,term@TermName(name),_,_) =>
            Apply(Ident(TermName("__valName")), List(Ident(term), Literal(Constant(name))))
          }
          Function(params, q"..$named; ${transform(body)}")

        // Don't rewrite +=, -=, *=, and /=. This restricts the return value to Unit
        // in the case where the compiler/DSL author chooses to use implicit classes rather
        // than infix_ methods
        /*
        case Apply(Select(qualifier, TermName("$plus$eq")), List(arg)) =>     // x += y
          liftFeature(None, "infix_$plus$eq", List(qualifier, arg))

        case Apply(Select(qualifier, TermName("$minus$eq")), List(arg)) =>    // x -= y
          liftFeature(None, "infix_$minus$eq", List(qualifier, arg))

        case Apply(Select(qualifier, TermName("$times$eq")), List(arg)) =>    // x *= y
          liftFeature(None, "infix_$times$eq", List(qualifier, arg))

        case Apply(Select(qualifier, TermName("$div$eq")), List(arg)) =>      // x /= y
          liftFeature(None, "infix_$div$eq", List(qualifier, arg))
        */


        /* Control structures (keywords) */

        case If(cond, thenBr, elseBr) => call(None, "__ifThenElse", List(cond, thenBr, elseBr))

        case Return(e) => call(None, "__return", List(e))

        case LabelDef(sym, List(), If(cond, Block(body :: Nil, Apply(Ident(label),
          List())), Literal(Constant(())))) if label == sym => // while(){}
          call(None, "__whileDo", List(cond, body))

        case LabelDef(sym, List(), Block(body :: Nil, If(cond, Apply(Ident(label),
          List()), Literal(Constant(()))))) if label == sym => // do while(){}
          call(None, "__doWhile", List(cond, body))

        case Try(block, catches, finalizer) =>
          __c.warning(tree.pos, "Staging of try/catch is not supported.")
          super.transform(tree)

        case Throw(expr) =>
          call(None, "__throw", List(expr))

        /* Special case + for String literals */

        // only stage `+` to `infix_+` if lhs is a String *literal* (we can't look at types!)
        // NOFIX: this pattern does not work for: `string + unstaged + staged`
        case Apply(Select(qual @ Literal(Constant(s: String)), TermName("$plus")), List(arg)) =>
          call(None, "infix_$plus", List(qual, arg))

        /* Methods defined on Any/AnyRef with arguments */

        case Apply(Select(qualifier, TermName("$eq$eq")), List(arg)) =>
          call(None, "infix_$eq$eq", List(qualifier, arg))

        case Apply(Select(qualifier, TermName("$bang$eq")), List(arg)) =>
          call(None, "infix_$bang$eq", List(qualifier, arg))

        case Apply(Select(qualifier, TermName("equals")), List(arg)) =>
          call(None, "infix_equals", List(qualifier, arg))

        case Apply(Select(qualifier, TermName("eq")), List(arg)) =>
          call(None, "infix_eq", List(qualifier, arg))

        case Apply(Select(qualifier, TermName("ne")), List(arg)) =>
          call(None, "infix_ne", List(qualifier, arg))

        case Apply(Select(qualifier, TermName("wait")), List(arg)) =>
          call(None, "infix_wait", List(qualifier, arg))

        case Apply(Select(qualifier, TermName("wait")), List(arg0, arg1)) =>
          call(None, "infix_wait", List(qualifier, arg0, arg1))

        case Apply(Select(qualifier, TermName("synchronized")), List(arg)) =>
          call(None, "infix_synchronized", List(qualifier, arg))

        case Apply(TypeApply(Select(qualifier, TermName("synchronized")), targs), List(arg)) =>
          call(None, "infix_synchronized", List(qualifier, arg), targs)

        case TypeApply(Select(qualifier, TermName("asInstanceOf")), targs) =>
          call(None, "infix_asInstanceOf", List(qualifier), targs)

        case TypeApply(Select(qualifier, TermName("isInstanceOf")), targs) =>
          call(None, "infix_isInstanceOf", List(qualifier), targs)

        /* Methods defined on Any/AnyRef without arguments */

        // For 0-arg methods we get a different tree depending on if the user writes empty parens 'x.clone()' or no parens 'x.clone'
        // We always match on the empty parens version first

        case Apply(Select(qualifier, TermName("toString")), List()) =>
          call(None, "infix_toString", List(qualifier))

        case Select(qualifier, TermName("toString")) =>
          call(None, "infix_toString", List(qualifier))

        case Apply(Select(qualifier, TermName("$hash$hash")), List()) =>
          call(None, "infix_$hash$hash", List(qualifier))

        case Select(qualifier, TermName("$hash$hash")) =>
          call(None, "infix_$hash$hash", List(qualifier))

        case Apply(Select(qualifier, TermName("hashCode")), List()) =>
          call(None, "infix_hashCode", List(qualifier))

        case Select(qualifier, TermName("hashCode")) =>
          call(None, "infix_hashCode", List(qualifier))

        case Apply(Select(qualifier, TermName("clone")), List()) =>
          call(None, "infix_clone", List(qualifier))

        case Select(qualifier, TermName("clone")) =>
          call(None, "infix_clone", List(qualifier))

        case Apply(Select(qualifier, TermName("notify")), List()) =>
          call(None, "infix_notify", List(qualifier))

        case Select(qualifier, TermName("notify")) =>
          call(None, "infix_notify", List(qualifier))

        case Apply(Select(qualifier, TermName("notifyAll")), List()) =>
          call(None, "infix_notifyAll", List(qualifier))

        case Select(qualifier, TermName("notifyAll")) =>
          call(None, "infix_notifyAll", List(qualifier))

        case Apply(Select(qualifier, TermName("wait")), List()) =>
          call(None, "infix_wait", List(qualifier))

        case Select(qualifier, TermName("wait")) =>
          call(None, "infix_wait", List(qualifier))

        case Apply(Select(qualifier, TermName("finalize")), List()) =>
          call(None, "infix_finalize", List(qualifier))

        case Select(qualifier, TermName("finalize")) =>
          call(None, "infix_finalize", List(qualifier))

        case Apply(Select(qualifier, TermName("getClass")), List()) =>
          call(None, "infix_getClass", List(qualifier))

        case Select(qualifier, TermName("getClass")) =>
          call(None, "infix_getClass", List(qualifier))

        // HACK: Transform into if-then-else for now. Better way?
        /*case Match(selector, cases) => tree

          def transformCase(cases: Seq[Tree]): Tree = {
            def transformSingleCase(cas: Tree, els: => Tree): Tree = cas match {
              // case name: Type if guard => body
              case CaseDef(Bind(name, Typed(typeTree)), EmptyTree, body) => If(q"$selector.isInstanceOf[$typeTree]", body, els)
              case CaseDef(Bind(name, Typed(typeTree)), guard, body) => If(q"$selector.isInstanceOf[$typeTree] && $guard", body, els)

              // case Unapply(args...) if guard => body
              case CaseDef(Apply(unapply, List(args)), _, _) => c.abort(c.enclosingPosition, "Virtualization of unapply methods is currently unsupported")
              case CaseDef(Bind(name, Apply(unapply, List(args))), _, _) => c.abort(c.enclosingPosition, "Virtualization of unapply methods is currently unsupported")

              // case name @ pattern if guard => body
              case CaseDef(Bind(name, pattern), EmptyTree, body) => If(q"$selector == $pattern", body, els)
              case CaseDef(Bind(name, pattern), guard, body) => If(q"$selector == $pattern && $guard", body, els)

              // case pattern if guard => body
              case CaseDef(pattern, EmptyTree, body) => If(q"$selector == $pattern", body, els)
              case CaseDef(pattern, guard, body) => If(q"$selector == $pattern && $guard", body, els)
            }

            if (cases.length == 2) (cases(0),cases(1)) match {
              case (c0, CaseDef(Ident(termNames.WILDCARD), EmptyTree, body)) => transformSingleCase(c0, body)
              case _ => transformSingleCase(cases.head, transformCase(cases.tail))
            }
            else if (cases.length > 2) {
              transformSingleCase(cases.head, transformCase(cases.tail))
            }
            else {
              transformSingleCase(cases.head, q"()")
            }
          }*/

        //case ClassDef(mods, name, tpt, body) if mods.hasFlag(Flag.CASE) =>
          // sstucki: there are issues with the ordering of
          // virtualization and expansion of case classes (i.e. some
          // of the expanded code might be virtualized even though it
          // should not be and vice-versa).  So until we have decided
          // how proper virtualization of case classes should be done,
          // any attempt to do so should fail.
          // TR: not 100% sure what the issue is (although i vaguely
          // remember that we had issues in Scala-virtualized with
          // auto-generated case class equality methods using virtualized
          // equality where it shouldn't). For the moment it seems like
          // just treating case classes as regular classes works fine.
          // __.warning(tree.pos, "virtualization of case classes is not fully supported.")
          //super.transform(tree) //don't virtualize the case class definition but virtualize its body


        // Argon-specific hack for changing T:Type to T<:MetaAny[T]:Type
//        case DefDef(mods,name,tparams,paramss,retTpe,body) =>
//          // HACK: Change return type from IR.Void to scala.Unit
//          // This is to allow both
//          //   def method() { } and
//          //   def method(): Unit = { } syntax
//          // Since lifting from scala.Unit to IR.Void is supported in argon,
//          // but not IR.Unit to scala.Unit, this is the only sane thing to do
//          val modifiedRetTpe = retTpe match {
//            case Ident(TypeName("Unit")) =>
//              Select(Ident(TermName("scala")),TypeName("Unit"))
//
//            case tp => tp
//          }
//
//          if (paramss.nonEmpty) {
//            val metaTypes = paramss.last.collect{
//              case ValDef(ms,_,AppliedTypeTree(Ident(TypeName("Meta")),List(typeTree)),rhs) if ms.hasFlag(Flag.IMPLICIT) => typeTree
//              case ValDef(ms,_,AppliedTypeTree(Ident(TypeName("Type")),List(typeTree)),rhs) if ms.hasFlag(Flag.IMPLICIT) => typeTree
//            }
//
//            object TypedTree {
//              def unapply(x: Tree): Option[String] = x match {
//                case Ident(TypeName(typeName)) => Some(typeName)
//                case AppliedTypeTree(TypedTree(typeName),args) => Some(typeName)
//                case ExistentialType(TypedTree(typeName),args) => Some(typeName)
//              }
//            }
//            val metaTypeNames = metaTypes.map{case TypedTree(typeName) => typeName }
//
//            val newTParams = tparams.map{
//              case TypeDef(ms,TypeName(typeName),targs,TypeBoundsTree(child,EmptyTree)) if metaTypeNames.contains(typeName) =>
//                val i = metaTypeNames.indexOf(typeName)
//                val superBound = AppliedTypeTree(Ident(TypeName("MetaAny")), List(metaTypes(i)))
//                TypeDef(ms,TypeName(typeName),targs,TypeBoundsTree(child,superBound))
//
//              case tp => tp
//            }
//
//            super.transform( DefDef(mods,name,newTParams,paramss,modifiedRetTpe,body) )
//
//          }
//          else super.transform(DefDef(mods,name,tparams,paramss,modifiedRetTpe,body))

        case _ =>
          super.transform(tree)
      }
    }
  }
}
