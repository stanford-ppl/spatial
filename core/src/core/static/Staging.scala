package core
package static

import forge.tags._
import forge.recursive
import forge.implicits.collections._

import scala.annotation.unchecked.{uncheckedVariance => uV}

trait Staging { this: Printing =>
  def const[A<:Sym[A]:Type](c: A#I): A = Type[A]._new(Def.Const(c))
  def const[C,A](tp: ExpType[C,A], c: C): A = tp._new(Def.Const(c))

  @stateful def param[A<:Sym[A]:Type](c: A#I): A = Type[A]._new(Def.Param(state.nextId(),c))
  @stateful def param[C,A](tp: ExpType[C,A], c: C): A = tp._new(Def.Param(state.nextId(),c))

  @stateful def err[A:Type]: A = Type[A]._new(Def.Error[A](state.nextId()))
  @stateful def bound[A:Type]: A = Type[A]._new(Def.Bound[A](state.nextId()))

  @stateful private def symbol[A](tp: Type[A], op: Op[A]): A = tp._new(Def.Node(state.nextId(),op))

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
      (aliases + sym).foreach{alias => error(s"${alias.src}:  symbol ${stm(alias)} defined here") }
    }
    if (immutables.nonEmpty) {
      error(ctx, "Illegal mutation of immutable symbols")
      immutables.foreach{s =>
        error(s"${s.src}:  symbol ${stm(s)} defined here")
        dbgs(s"${stm(s)}")
        strMeta(s)
      }
    }
  }

  @rig def runFlows(sym: Sym[_], op: Op[_]): Unit = flows.apply(sym, op)


  @rig def register[R](op: Op[R], symbol: () => R): R = rewrites.apply(op)(op.tR,ctx,state) match {
    case Some(s) => s
    case None    =>
      if (state == null) throw new Exception("Null state during staging")
      val effects = allEffects(op)
      val mayCSE = effects.mayCSE

      def stageEffects(addToCache: Boolean): R = {
        val lhs = symbol()
        val sym = op.tR.boxed(lhs)

        checkAliases(sym,effects)
        runFlows(sym,op)

        state.scope :+= sym // Append (effective constant time for Vector)
        if (!effects.isPure) state.impure +:= Impure(sym,effects)
        if (effects != Effects.Pure) effectsOf(sym) = effects
        if (mayCSE) state.cache += op -> sym
        lhs
      }
      state.cache.get(op).filter{s => mayCSE && effectsOf(s) == effects} match {
        case Some(s) if s.tp <:< op.tR => s.asInstanceOf[R]
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
    logs(s"Staging $op with type evidence ${op.tR}")
    val t = register(op, () => symbol(op.tR,op))
    op.tR.boxed(t).src = ctx
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
