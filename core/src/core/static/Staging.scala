package core
package static

import forge.tags._
import utils.implicits.collections._
import utils.recursive

import scala.annotation.unchecked.{uncheckedVariance => uV}

trait Staging { this: Printing =>
  /** Create a checked parameter (implicit state required) */
  @stateful def param[A<:Sym[A]:Type](c: A#L, checked: Boolean = false): A = Type[A].from(c, checked, isParam = true)

  /** Create a checked constant (implicit state required) */
  @stateful def const[A<:Sym[A]:Type](c: A#L, checked: Boolean = false): A = Type[A].from(c, checked)

  /** Create an unchecked constant (no implicit state required) */
  def uconst[A<:Sym[A]:Type](c: A#L): A = Type[A].uconst(c)

  private[core] def _const[A<:Sym[A]:Type](c: A#L): A = Type[A]._new(Def.Const(c), SrcCtx.empty)
  private[core] def _const[C,A](tp: ExpType[C,A], c: C): A = tp._new(Def.Const(c), SrcCtx.empty)

  @stateful private[core] def _param[A<:Sym[A]:Type](c: A#L): A = Type[A]._new(Def.Param(state.nextId(),c), ctx)
  @stateful private[core] def _param[C,A](tp: ExpType[C,A], c: C): A = tp._new(Def.Param(state.nextId(),c), ctx)

  @stateful def err[A:Type](msg: String): A = Type[A]._new(Def.Error[A](state.nextId(),msg), ctx)
  @stateful def err_[A](tp: Type[A], msg: String): A = tp._new(Def.Error[A](state.nextId(),msg), ctx)

  @stateful def bound[A:Type]: A = Type[A]._new(Def.Bound[A](state.nextId()), ctx)

  @stateful private def symbol[A](tp: Type[A], op: Op[A]): A = {
    if (state eq null) throw new Exception(s"Staging in null state scope")
    if (tp eq null) throw new Exception(s"Staging with null type")
    tp._new(Def.Node(state.nextId(),op), ctx)
  }

  /**
    * Correctness checks:
    *   1. No mutable aliasing
    *   2. No mutation of immutable symbols
    */
  @rig def checkAliases(sym: Sym[_], effects: Effects): Unit = {
    val immutables = effects.writes.filterNot(x => isMutable(x))
    val aliases = mutableAliases(op) diff effects.writes

    //        logs(s"  aliases: ${aliasSyms(op)}")
    //        logs(s"  copies: ${copySyms(op)}")
    //        logs(s"  contains: ${containSyms(op)}")
    //        logs(s"  extracts: ${extractSyms(op)}")
    //        logs(s"  effects: $effects")
    //        logs(s"  deps: $deps")
    //        logs(s"  written immutables: $immutables")
    //        logs(s"  mutable aliases: $aliases")

    if (aliases.nonEmpty) {
      error(ctx, "Illegal sharing of mutable objects: ")
      (aliases + sym).foreach{alias => error(s"${alias.ctx}:  symbol ${stm(alias)} defined here") }
    }
    if (immutables.nonEmpty) {
      error(ctx, "Illegal mutation of immutable symbols")
      immutables.foreach{s =>
        error(s"${s.ctx}:  symbol ${stm(s)} defined here")
        dbgs(s"${stm(s)}")
        strMeta(s)
      }
    }
  }

  @rig def runFlows[A](sym: Sym[A], op: Op[A]): Unit = flows.apply(sym, op)


  @rig def register[R](op: Op[R], symbol: () => R): R = rewrites.apply(op)(op.R,ctx,state) match {
    case Some(s) => s
    case None if state eq null =>
      // General operations in object/trait constructors are currently disallowed
      // because it can mess up namespaces in the output code
      // TODO[6]: Allow global namespace operations?
      error(ctx, "Only constant declarations are allowed in the global namespace.")
      error(ctx)
      err_[R](op.R, "Invalid declaration in global namespace")

    case None =>
      val effects = allEffects(op)
      val mayCSE = effects.mayCSE

      def stageEffects(addToCache: Boolean): R = {
        val lhs = symbol()
        val sym = op.R.boxed(lhs)

        checkAliases(sym,effects)
        runFlows(sym,op)

        state.scope :+= sym // Append (effective constant time for Vector)
        if (!effects.isPure) state.impure +:= Impure(sym,effects)
        if (effects != Effects.Pure) effectsOf(sym) = effects
        if (mayCSE) state.cache += op -> sym
        lhs
      }
      state.cache.get(op).filter{s => mayCSE && effectsOf(s) == effects} match {
        case Some(s) if s.tp <:< op.R => s.asInstanceOf[R]
        case None => stageEffects(addToCache = mayCSE)
      }
  }

  @rig def restage[R](sym: Sym[R]): Sym[R] = sym match {
    case Op(rhs) =>
      val lhs: R = register[R](rhs, () => sym.unbox)
      sym.tp.boxed(lhs)
    case _ => sym
  }
  @rig def stage[R](op: Op[R]): R = {
    val t = register(op, () => symbol(op.R,op))
    op.R.boxed(t).ctx = ctx
    t
  }

  // TODO: Performance bottleneck here
  private def aliasSyms(a: Any): Set[Sym[_]]   = recursive.collectSets{case s: Sym[_] => Set(s) case d: Op[_] => d.aliases }(a)
  private def containSyms(a: Any): Set[Sym[_]] = recursive.collectSets{case d: Op[_] => d.contains}(a)
  private def extractSyms(a: Any): Set[Sym[_]] = recursive.collectSets{case d: Op[_] => d.extracts}(a)
  private def copySyms(a: Any): Set[Sym[_]]    = recursive.collectSets{case d: Op[_] => d.copies}(a)
  private def noPrims(x: Set[Sym[_]]): Set[Sym[_]] = x.filter{s => !s.tp.isPrimitive}

  @stateful def shallowAliases(x: Any): Set[Sym[_]] = {
    noPrims(aliasSyms(x)).flatMap{case Stm(s,d) => state.shallowAliasCache.getOrElseAdd(s, () => shallowAliases(d)) + s } ++
      noPrims(extractSyms(x)).flatMap{case Stm(s,d) => state.deepAliasCache.getOrElseAdd(s, () => deepAliases(d)) }
  }
  @stateful def deepAliases(x: Any): Set[Sym[_]] = {
    noPrims(aliasSyms(x)).flatMap{case Stm(s,d) => state.deepAliasCache.getOrElseAdd(s, () => deepAliases(d)) } ++
      noPrims(copySyms(x)).flatMap{case Stm(s,d) => state.deepAliasCache.getOrElseAdd(s, () => deepAliases(d)) } ++
      noPrims(containSyms(x)).flatMap{case Stm(s,d) => state.aliasCache.getOrElseAdd(s,  () => allAliases(d)) + s } ++
      noPrims(extractSyms(x)).flatMap{case Stm(s,d) => state.deepAliasCache.getOrElseAdd(s, () => deepAliases(d)) }
  }
  @stateful final def allAliases(x: Any): Set[Sym[_]] = {
    shallowAliases(x) ++ deepAliases(x)
  }
  @stateful final def mutableAliases(x: Any): Set[Sym[_]] = allAliases(x).filter(x => isMutable(x))
  @stateful final def mutableInputs(d: Op[_]): Set[Sym[_]] = {
    val bounds = d.binds
    val actuallyReadSyms = d.reads diff bounds
    mutableAliases(actuallyReadSyms) filterNot (bounds contains _)
  }

  /**
    * Find scheduling dependencies in context
    * WAR - always include reads as scheduling dependencies of writes
    * "AAA" - always include allocation as scheduling dependencies of an access (read or write)
    * RAW/WAW - include the *most recent* write as scheduling dependency of an access ("AAW" - access after write)
    * simple - include the *most recent* previous simple effect as a scheduling dependency of a simple effect
    * global - include ALL global effects as scheduling dependencies of a global effect
    */
  @stateful final def effectDependencies(effects: Effects): Seq[Impure] = {
    if (effects.global) state.impure
    else {
      val read = effects.reads
      val write = effects.writes
      val accesses = read ++ write  // Cannot read/write prior to allocation

      def isWARHazard(u: Effects) = u.mayRead(write)

      // RAW / WAW
      var unwrittenAccesses = accesses // Reads/writes for which we have not yet found a previous writer
      def isAAWHazard(u: Effects) = {
        if (unwrittenAccesses.nonEmpty) {
          val (written, unwritten) = unwrittenAccesses.partition(u.writes.contains)
          unwrittenAccesses = unwritten
          written.nonEmpty
        }
        else false
      }

      val hazards = state.impure.filter{case Impure(s,e) => isWARHazard(e) || isAAWHazard(e) || (accesses contains s) }
      val simpleDep = if (effects.simple) state.impure.find{case Impure(s,e) => e.simple; case _ => false } else None // simple
      val globalDep = state.impure.find{case Impure(s,e) => e.global; case _ => false } // global

      hazards ++ simpleDep ++ globalDep
    }
  }

  @rig final def allEffects(d: Op[_]): Effects = {
    val mIns = mutableInputs(d)
    //val atomicEffects = propagateWrites(u)

    //logs(s"  mutable inputs = $mIns")
    //logs(s"  actual writes = ${atomicEffects.writes}")

    val effects = if (mIns.isEmpty) d.effects else d.effects andAlso Effects.Reads(mIns)
    val deps = effectDependencies(effects)
    effects.copy(antiDeps = deps)
  }
}
