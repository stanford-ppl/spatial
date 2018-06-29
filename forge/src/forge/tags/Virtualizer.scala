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
  *   x                      =>       __use(x)
  *   val x = e              =>       val x = e; __valName(x, "x")
  *   var x = e              =>       val x = __newVar(e); __valName(x, "x")
  *   if (c) t else e        =>       __ifThenElse(c, t, e)
  *   return t               =>       __return(t)
  *   x = t                  =>       __assign(x, t)
  *   while(c) b             =>       __whileDo(c, b)
  *   do b while c           =>       __doWhile(c, b)
  * }}}
  *
  * === Infix methods for `Any` methods ===
  * {{{
  *   t == t1                =>       t.infix_==(t1)
  *   t != t1                =>       t.infix_!=(t1)
  *   t.##                   =>       t.infix_##
  *   t.equals t1            =>       t.infix_equals(t1)
  *   t.hashCode             =>       t.infix_hashCode
  *   t.asInstanceOf[T]      =>       t.infix_asInstanceOf[T]
  *   t.isInstanceOf[T]      =>       t.infix_isInstanceOf[T]
  *   t.toString             =>       t.infix_toString
  *   t.getClass             =>       t.infix_getClass
  * }}}
  *
  * === Infix methods for `AnyRef` methods ===
  * {{{
  *   t eq t1                =>       t.infix_eq(t1)
  *   t ne t1                =>       t.infix_ne(t1)
  *   t.clone                =>       t.infix_clone
  *   t.notify               =>       t.infix_notify
  *   t.notifyAll            =>       t.infix_notifyAll
  *   t.synchronized(t1)     =>       t.infix_synchronized(t1)
  *   t.synchronized[T](t1)  =>       t.infix_synchronized[T](t1)
  *   t.wait                 =>       t.infix_wait
  *   t.wait(l)              =>       t.infix_wait(l)
  *   t.wait(t1, l)          =>       t.infix_wait(t1, l)
  *   t.finalize()           =>       t.infix_finalize
  * }}}
  *
  * @todo
  * {{{
  *   try b catch c          =>       __tryCatch(b, c, f)
  *   throw e                =>       __throw(e)
  *   Nothing                =>       ???
  *   Null                   =>       ???
  *   a match {              =>       ???
  *     case t: T => rhs     =>       ???
  *     case T(u) => rhs     =>       ???
  *     case t @ T(u) => rhs =>       ???
  *     case t    => rhs     =>       ???
  *     case _    => rhs     =>       ???
  *   }                      =>       ???
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

  def runVirtualizer(t: Tree): List[Tree] = VirtualizationTransformer(t)

  private object VirtualizationTransformer {
    def apply(tree: Tree) = new VirtualizationTransformer().apply(tree)
  }

  private class VirtualizationTransformer extends Transformer {
    def apply(tree: __c.universe.Tree): List[Tree] = transformStm(tree)

    def call(receiver: Option[Tree], name: String, args: List[Tree], targs: List[Tree] = Nil): Tree = {
      val args2 = args.map(transform).map{
        case t @ Ident(TermName(_)) => methodCall(None, "__use", List(List(t)))
        case t => t
      }
      methodCall(receiver.map(transform), name, List(args2), targs)
    }

    /** Call for transforming Blocks.
      *
      * Allows expanding a single statement to multiple statements within a given scope.
      */
    private def transformStm(tree: Tree): List[Tree] = tree match {
      case ValDef(mods, term@TermName(name), tpt, rhs) if mods.hasFlag(Flag.MUTABLE) && !mods.hasFlag(Flag.PARAMACCESSOR) =>
        //info("Found var: ")
        //info(showRaw(tree))
        /**
          * Note: It's tempting to transform:
          *   var v: T = rhs
          *
          * to:
          *   val v$v: Var[T] = newVar(rhs)
          *   def v: T = readVar(v$v)
          *   def v_=(t: T): Unit = assign(v$v, t)
          *
          * Unfortunately, this doesn't work outside the primary scope of a class/object
          *  - setters are only inferred for direct class/object fields
          */
        tpt match {
          case TypeTree() =>
            __c.info(tree.pos, "Type annotation recommended for var declaration.", force = true)
            val vdef = ValDef(mods, term, TypeTree(), call(None, "__newVar", List(rhs), Nil)).asVar
            val regv = methodCall(None, "__valName", List(List(Ident(term), Literal(Constant(name)))),Nil)
            List(vdef, regv)
          case _ =>
            val vdef = ValDef(mods, term, TypeTree(), call(None, "__newVar", List(rhs), List(tpt))).asVar
            val regv = methodCall(None, "__valName", List(List(Ident(term), Literal(Constant(name)))),Nil)
            List(vdef, regv)
        }


      case v@ValDef(mods, term@TermName(name), _, _) if !mods.hasFlag(Flag.PARAMACCESSOR) =>
        val vdef = transform(v)
        val regv = methodCall(None, "__valName", List(List(Ident(term), Literal(Constant(name)))),Nil)
        List(vdef, regv)

      case _ => List(transform(tree))
    }


    override def transform(tree: Tree): Tree = atPos(tree.pos) {
      tree match {
        /* Attempt to stage vars in both class bodies and blocks */
        case Template(parents, selfType, bodyList) => Template(parents, selfType, bodyList.flatMap(transformStm))

        case Block(stms, ret) =>
          val stms2 = stms.flatMap(transformStm) ++ transformStm(ret)
          Block(stms2.dropRight(1), stms2.last)

        case Function(params,body) =>
          val named = params.collect{case ValDef(_,term@TermName(name),_,_) =>
            Apply(Ident(TermName("__valName")), List(Ident(term), Literal(Constant(name))))
          }
          Function(params, q"..$named; ${transform(body)}")

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
        case Apply(Select(qual @ Literal(Constant(s:String)), TermName("$plus")), List(arg)) =>
          call(Some(qual), "infix_$plus", List(arg))

        case Assign(lhs, rhs) => methodCall(None, "__assign", List(List(lhs, transform(rhs))))

        /* Methods defined on Any/AnyRef with arguments */

        case Apply(Select(qual, TermName("$eq$eq")), List(arg))       => call(Some(qual), "infix_$eq$eq", List(arg))
        case Apply(Select(qual, TermName("$bang$eq")), List(arg))     => call(Some(qual), "infix_$bang$eq", List(arg))
        case Apply(Select(qual, TermName("equals")), List(arg))       => call(Some(qual), "infix_equals", List(arg))
        case Apply(Select(qual, TermName("eq")), List(arg))           => call(Some(qual), "infix_eq", List(arg))
        case Apply(Select(qual, TermName("ne")), List(arg))           => call(Some(qual), "infix_ne", List(arg))
        case Apply(Select(qual, TermName("wait")), List(arg))         => call(Some(qual), "infix_wait", List(arg))
        case Apply(Select(qual, TermName("wait")), List(arg0, arg1))  => call(Some(qual), "infix_wait", List(arg0, arg1))
        case Apply(Select(qual, TermName("synchronized")), List(arg)) => call(Some(qual), "infix_synchronized", List(arg))
        case Apply(TypeApply(Select(qual, TermName("synchronized")), targs), List(arg)) => call(Some(qual), "infix_synchronized", List(arg), targs)

        case TypeApply(Select(qual, TermName("asInstanceOf")), targs) => call(Some(qual), "infix_asInstanceOf", Nil, targs)
        case TypeApply(Select(qual, TermName("isInstanceOf")), targs) => call(Some(qual), "infix_isInstanceOf", Nil, targs)

        /* Methods defined on Any/AnyRef without arguments */

        // For 0-arg methods we get a different tree depending on if the user writes empty parens 'x.clone()' or no parens 'x.clone'
        // We always match on the empty parens version first

        // q.toString(), q.toString
        case Apply(Select(qual, TermName("toString")), List())   => call(Some(qual), "infix_toString", Nil)
        case Select(qual, TermName("toString"))                  => call(Some(qual), "infix_toString", Nil)

        // q.##(), q.##
        case Apply(Select(qual, TermName("$hash$hash")), List()) => call(Some(qual), "infix_$hash$hash", Nil)
        case Select(qual, TermName("$hash$hash"))                => call(Some(qual), "infix_$hash$hash", Nil)

        // q.hashCode(), q.hashCode
        case Apply(Select(qual, TermName("hashCode")), List())   => call(Some(qual), "infix_hashCode", Nil)
        case Select(qual, TermName("hashCode"))                  => call(Some(qual), "infix_hashCode", Nil)

        // q.clone(), q.clone
        case Apply(Select(qual, TermName("clone")), List())      => call(Some(qual), "infix_clone", Nil)
        case Select(qual, TermName("clone"))                     => call(Some(qual), "infix_clone", Nil)

        // q.notify(), q.notify
        case Apply(Select(qual, TermName("notify")), List())     => call(Some(qual), "infix_notify", Nil)
        case Select(qual, TermName("notify"))                    => call(Some(qual), "infix_notify", Nil)

        // q.notifyAll(), q.notifyAll
        case Apply(Select(qual, TermName("notifyAll")), List())  => call(Some(qual), "infix_notifyAll", Nil)
        case Select(qual, TermName("notifyAll"))                 => call(Some(qual), "infix_notifyAll", Nil)

        // q.wait(), q.wait
        case Apply(Select(qual, TermName("wait")), List())       => call(Some(qual), "infix_wait", Nil)
        case Select(qual, TermName("wait"))                      => call(Some(qual), "infix_wait", Nil)

        // q.finalize(), q.finalize
        case Apply(Select(qual, TermName("finalize")), List())   => call(Some(qual), "infix_finalize", Nil)
        case Select(qual, TermName("finalize"))                  => call(Some(qual), "infix_finalize", Nil)

        // q.getClass(), q.getClass
        case Apply(Select(qual, TermName("getClass")), List())   => call(Some(qual), "infix_getClass", Nil)
        case Select(qual, TermName("getClass"))                  => call(Some(qual), "infix_getClass", Nil)

        case Apply(app,args)  =>
          val args2 = args.map(transform).map{
            case t @ Ident(TermName(_)) => methodCall(None, "__use", List(List(t)))
            case t => t
          }
          Apply(transform(app), args2)


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

        case _ =>
          super.transform(tree)
      }
    }
  }
}
