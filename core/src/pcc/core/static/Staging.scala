package pcc.core.static

import forge._
import pcc.data.{Effects,isMutable,effectsOf,depsOf,Effectful}
import pcc.util.recursive

trait Staging { this: Printing =>

  def typ[T:Sym]: Sym[T] = implicitly[Sym[T]]
  def mtyp[A,B](x: Sym[A]): Sym[B] = x.asInstanceOf[Sym[B]]

  implicit def toSym[A:Sym](x: A): Sym[A] = typ[A].asSym(x)

  @stateful def bound[T:Sym]: T = fresh(typ[T])
  @stateful def const[T:Sym](c: Any): T = const(typ[T], c)
  @stateful def param[T:Sym](c: Any): T = param(typ[T], c)

  @stateful def fresh[T](tp: Sym[T]): T = tp.asSym(tp.fresh(state.nextId())).asBound()
  @stateful def const[T](tp: Sym[T], c: Any): T = tp.asSym(tp.fresh(state.nextId())).asConst(c)
  @stateful def param[T](tp: Sym[T], c: Any): T = tp.asSym(tp.fresh(state.nextId())).asParam(c)
  @stateful def symbol[T](tp: Sym[T], d: Op[T]): T = tp.asSym(tp.fresh(state.nextId())).asSymbol(d)

  @internal def restage[T](sym: Sym[T]): Sym[T] = sym match {
    case Op(rhs) =>
      val sym2 = rewrite(rhs).map{s => sym.asSym(s) }.getOrElse(sym)
      if (sym2 == sym) state.context +:= sym2
      val rhs2 = sym2.op
      rhs2.foreach{o =>
        val (effects,deps) = allEffects(o)
        if (effects != Effects.Pure) effectsOf(sym2) = effects
        if (deps.nonEmpty) depsOf(sym2) = deps
      }
      sym2

    case _ => sym
  }



  @internal def stage[T](d: Op[T]): T = rewrite(d).getOrElse{
    if (state == null) throw new Exception("Null state during staging")
    val (effects,deps) = allEffects(d)

    //logs(s"Staging $d, effects = ${d.effects}")
    //logs(s"  full effects = $effects")
    //logs(s"  isIdempotent = ${effects.isIdempotent}")

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
      val symsWithSameEffects = symsWithSameDef.filter {
        case Effectful(u2, es) => u2 == effects && es == deps
        case _ => deps.isEmpty && effects == Effects.Pure
      }

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
  }

  private def aliasSyms(a: Any): Set[Sym[_]]   = recursive.collectSets{case s: Sym[_] => Set(s) case d: Op[_] => d.aliases }(a)
  private def containSyms(a: Any): Set[Sym[_]] = recursive.collectSets{case d: Op[_] => d.contains}(a)
  private def extractSyms(a: Any): Set[Sym[_]] = recursive.collectSets{case d: Op[_] => d.extracts}(a)
  private def copySyms(a: Any): Set[Sym[_]]    = recursive.collectSets{case d: Op[_] => d.copies}(a)
  private def noPrims(x: Set[Sym[_]]): Set[Sym[_]] = x.filter{s => !s.isPrimitive}

  @stateful def shallowAliases(x: Any): Set[Sym[_]] = {
    noPrims(aliasSyms(x)).flatMap { case Stm(s,d) => state.shallowAliasCache.getOrElseUpdate(s, shallowAliases(d)) + s } ++
      noPrims(extractSyms(x)).flatMap { case Stm(s,d) => state.deepAliasCache.getOrElseUpdate(s, deepAliases(d)) }
  }
  @stateful def deepAliases(x: Any): Set[Sym[_]] = {
    noPrims(aliasSyms(x)).flatMap { case Stm(s,d) => state.deepAliasCache.getOrElseUpdate(s, deepAliases(d)) } ++
      noPrims(copySyms(x)).flatMap { case Stm(s,d) => state.deepAliasCache.getOrElseUpdate(s, deepAliases(d)) } ++
      noPrims(containSyms(x)).flatMap { case Stm(s,d) => state.aliasCache.getOrElseUpdate(s, allAliases(d)) + s } ++
      noPrims(extractSyms(x)).flatMap { case Stm(s,d) => state.deepAliasCache.getOrElseUpdate(s, deepAliases(d)) }
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

  @internal final def allEffects(d: Op[_]): (Effects, Seq[Sym[_]]) = {
    val mIns = mutableInputs(d)
    //val atomicEffects = propagateWrites(u)

    //logs(s"  mutable inputs = $mIns")
    //logs(s"  actual writes = ${atomicEffects.writes}")

    val effects = if (mIns.isEmpty) d.effects else d.effects andAlso Effects.Reads(mIns)
    val deps = effectDependencies(effects)
    (effects, deps)
  }
}
