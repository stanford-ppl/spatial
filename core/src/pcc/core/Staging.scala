package pcc.core

import forge._
import pcc.data.{Effects,isMutable,effectsOf,depsOf,Effectful}
import pcc.util.recursive

trait Staging { this: Printing =>

  def tp[T:Sym]: Sym[T] = implicitly[Sym[T]]

  implicit class SymOps[T](x: T) {
    def asSym(implicit sym: Sym[T]): Sym[T] = sym.asSym(x)
  }

  @stateful def bound[T:Sym]: T = fresh(tp[T])
  @stateful def const[T:Sym](c: Any): T = const(tp[T], c)
  @stateful def param[T:Sym](c: Any): T = param(tp[T], c)

  @stateful def fresh[T](tp: Sym[T]): T = tp.asSym(tp.fresh(state.nextId())).asBound()
  @stateful def const[T](tp: Sym[T], c: Any): T = tp.asSym(tp.fresh(state.nextId())).asConst(c)
  @stateful def param[T](tp: Sym[T], c: Any): T = tp.asSym(tp.fresh(state.nextId())).asParam(c)
  @stateful def symbol[T](tp: Sym[T], d: Op[T]): T = tp.asSym(tp.fresh(state.nextId())).asSymbol(d)

  @internal def stage[T](d: Op[T]): T = {
    if (state == null) throw new Exception("Null state during staging")

    //val atomicEffects = propagateWrites(u)

    log(s"Staging $d, effects = ${d.effects}")
    //log(s"  actual writes = ${atomicEffects.writes}")
    val mIns = mutableInputs(d)

    val effects = if (mIns.isEmpty) d.effects else d.effects andAlso Effects.Reads(mIns)
    log(s"  mutable inputs = $mIns")
    log(s"  full effects = $effects")
    log(s"  isIdempotent = ${effects.isIdempotent}")

    //val lhs = if (effects == Effects.Pure) registerDefWithCSE(d)(ctx)
    //else {
    //  state.checkContext()
      val deps = effectDependencies(effects)

      def stageEffects(): T = {
        val lhs = symbol(d.tR, d)
        val sym = d.tR.asSym(lhs)
        if (effects != Effects.Pure) effectsOf(sym) = effects
        if (deps.nonEmpty) depsOf(sym) = deps
        state.context +:= sym // prepend

        // Correctness checks -- cannot have mutable aliases, cannot mutate immutable symbols
        val immutables = effects.writes.filterNot(x => isMutable(x))
        val aliases = mutableAliases(d) diff effects.writes

        if (aliases.nonEmpty) {
          error(ctx, "Illegal sharing of mutable objects: ")
          (aliases + sym).foreach{alias => error(s"${alias.ctx}:  symbol ${stm(alias)} defined here") }
        }
        if (immutables.nonEmpty) {
          error(ctx, "Illegal mutation of immutable symbols")
          immutables.foreach { mut => error(s"${mut.ctx}:  symbol ${stm(mut)} defined here") }
        }

        lhs
      }

      if (effects.mayCSE) {
        // CSE statements which are idempotent and have identical effect summaries (e.g. repeated reads w/o writes)
        val symsWithSameDef = state.defCache.get(d).toList intersect state.context
        val symsWithSameEffects = symsWithSameDef.filter { case Effectful(u2, es) => u2 == effects && es == deps }

        if (symsWithSameEffects.isEmpty) {
          val lhs = stageEffects()
          state.defCache += d -> d.tR.asSym(lhs)
          lhs
        }
        else {
          symsWithSameEffects.head.asInstanceOf[T]
        }
      }
      else stageEffects()
    //}

    //lhs
  }

  private def aliasSyms(a: Any): Set[Sym[_]]   = recursive.collectSets{case s: Sym[_] => Set(s) case d: Op[_] => d.aliases }(a)
  private def containSyms(a: Any): Set[Sym[_]] = recursive.collectSets{case d: Op[_] => d.contains}(a)
  private def extractSyms(a: Any): Set[Sym[_]] = recursive.collectSets{case d: Op[_] => d.extracts}(a)
  private def copySyms(a: Any): Set[Sym[_]]    = recursive.collectSets{case d: Op[_] => d.copies}(a)
  private def noPrims(x: Set[Sym[_]]): Set[Sym[_]] = x.filter{s => !s.isPrimitive}

  @stateful def shallowAliases(x: Any): Set[Sym[_]] = {
    noPrims(aliasSyms(x)).flatMap { case s@Op(d) => state.shallowAliasCache.getOrElseUpdate(s, shallowAliases(d)) + s } ++
      noPrims(extractSyms(x)).flatMap { case s@Op(d) => state.deepAliasCache.getOrElseUpdate(s, deepAliases(d)) }
  }
  @stateful def deepAliases(x: Any): Set[Sym[_]] = {
    noPrims(aliasSyms(x)).flatMap { case s@Op(d) => state.deepAliasCache.getOrElseUpdate(s, deepAliases(d)) } ++
      noPrims(copySyms(x)).flatMap { case s@Op(d) => state.deepAliasCache.getOrElseUpdate(s, deepAliases(d)) } ++
      noPrims(containSyms(x)).flatMap { case s@Op(d) => state.aliasCache.getOrElseUpdate(s, allAliases(d)) + s } ++
      noPrims(extractSyms(x)).flatMap { case s@Op(d) => state.deepAliasCache.getOrElseUpdate(s, deepAliases(d)) }
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
  @stateful final def effectDependencies(effects: Effects)(implicit state: State): Seq[Sym[_]] = {
    if (effects.global) state.context
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

      val hazards = state.context.collect{case e@Effectful(u,_) if isWARHazard(u) || isAAWHazard(u) || (accesses contains e) => e }
      val simpleDep = if (effects.simple) state.context.find{case Effectful(u,_) => u.simple; case _ => false } else None // simple
      val globalDep = state.context.find{case Effectful(u,_) => u.global; case _ => false } // global

      hazards ++ simpleDep ++ globalDep
    }
  }
}
