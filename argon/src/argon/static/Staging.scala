package argon
package static

import forge.tags._
import utils.implicits.collections._
import utils.recursive
import utils.tags.instrument

trait Staging { this: Printing =>
  /** Create a checked parameter (implicit state required) */
  @stateful def parameter[A<:Sym[A]:Type](c: A#L, checked: Boolean = false): A = Type[A].from(c, checked, isParam = true)

  /** Create a checked constant (implicit state required) */
  @stateful def const[A<:Sym[A]:Type](c: A#L, checked: Boolean = false): A = Type[A].from(c, checked)

  /** Create an unchecked constant (no implicit state required) */
  def uconst[A<:Sym[A]:Type](c: A#L): A = Type[A].uconst(c)

  private[argon] def _const[A<:Sym[A]:Type](c: A#L): A = Type[A]._new(Def.Const(c), SrcCtx.empty)
  private[argon] def _const[C,A](tp: ExpType[C,A], c: C): A = tp._new(Def.Const(c), SrcCtx.empty)

  @stateful private[argon] def _param[A<:Sym[A]:Type](c: A#L): A = Type[A]._new(Def.Param(state.nextId(),c), ctx)
  @stateful private[argon] def _param[C,A](tp: ExpType[C,A], c: C): A = tp._new(Def.Param(state.nextId(),c), ctx)

  @stateful def err[A:Type](msg: String): A = Type[A]._new(Def.Error[A](state.nextId(),msg), ctx)
  @stateful def err_[A](tp: Type[A], msg: String): A = tp._new(Def.Error[A](state.nextId(),msg), ctx)

  @stateful def boundVar[A:Type]: A = Type[A]._new(Def.Bound[A](state.nextId()), ctx)

  @rig private def symbol[A](tp: Type[A], op: Op[A]): A = {
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
    val immutables = effects.writes.filterNot(_.isMutable)
    val aliases = (sym.mutableAliases diff effects.writes) diff Set(sym)

    //        logs(s"  aliases: ${aliasSyms(op)}")
    //        logs(s"  copies: ${copySyms(op)}")
    //        logs(s"  contains: ${containSyms(op)}")
    //        logs(s"  extracts: ${extractSyms(op)}")
    //        logs(s"  effects: $effects")
    //        logs(s"  deps: $deps")
    //        logs(s"  written immutables: $immutables")
    //        logs(s"  mutable aliases: $aliases")

    if (aliases.nonEmpty && !config.enableMutableAliases) {
      error(sym.ctx, s"${sym.nameOr("Value")} has multiple mutable aliases. Mutable aliasing is disallowed.")
      (aliases + sym).foreach{alias =>
        error(alias.ctx, s"${alias.nameOr("Value")} defined here.", noError = true)
        error(alias.ctx)
      }

      (aliases + sym).foreach{alias => dbgs(s"${alias.ctx}:  symbol ${stm(alias)} defined here") }
    }
    if (immutables.nonEmpty) {
      immutables.foreach{s =>
        error(s.ctx, s"Illegal mutation of immutable ${s.nameOr("value")} defined here.")
        error(s.ctx)
      }

      immutables.foreach{s =>
        dbgs(s"${s.ctx}:  Illegal mutation: ${stm(s)} defined here")
        dbgs(s"${stm(s)}")
        strMeta(s)
      }
    }
  }

  @rig def rewrite[R](op: Op[R]): Option[R] = state.rewrites.apply(op)(op.R,ctx,state)
  @rig def runFlows[A](sym: Sym[A], op: Op[A]): Unit = state.flows.apply(sym, op)

  @rig def register[R](op: Op[R], symbol: () => R, data: Sym[R] => Unit): R = rewrite(op) match {
    case Some(s) => s
    case None if state eq null =>
      // General operations in object/trait constructors are currently disallowed
      // because it can mess up namespaces in the output code
      // TODO[6]: Allow global namespace operations?
      error(ctx, "Only constant declarations are allowed in the global namespace.")
      error(ctx)
      err_[R](op.R, "Invalid declaration in global namespace")

    case None =>
      val sAliases = op.shallowAliases
      val dAliases = op.deepAliases

      val effects = allEffects(op)
      val mayCSE = effects.mayCSE

      def stageEffects(addToCache: Boolean): R = {
        val lhs = symbol()
        val sym = op.R.boxed(lhs)

        state.scope :+= sym
        if (effects.mayCSE)  state.cache += op -> sym               // Add to CSE cache
        if (!effects.isPure) state.impure :+= Impure(sym,effects)   // Add to list of impure syms
        if (!effects.isPure) sym.effects = effects                  // Register effects

        // Register aliases
        if (dAliases.nonEmpty) sym.deepAliases = dAliases           // Set deep aliases
        if (sAliases.nonEmpty) sym.shallowAliases = sAliases        // Set shallow aliases

        op.inputs.foreach{in => in.consumers += sym }                 // Register consumed
        sym.allAliases.foreach{alias => alias.shallowAliases += sym } // Register reverse aliases

        data(sym)           // Run immediate staging metadata rules
        runFlows(sym,op)    // Run flow rules

        if (config.enLog) {
          val writes = effects.writes.map{s => s"$s [${s.allAliases.mkString(",")}]" }.mkString(", ")
          logs(s"$lhs = $op")
          logs(s"  Effects:   $effects")
          logs(s"  Writes:    $writes")
          logs(s"  AliasSyms: ${op.aliasSyms}")
          logs(s"  Deep:      ${op.deepAliases}")
          logs(s"  Shallow:   ${op.shallowAliases}")
          logs(s"  Aliases:   ${sym.allAliases}")
        }

        checkAliases(sym, effects)

        lhs
      }
      state.cache.get(op).filter{s => mayCSE && s.effects == effects} match {
        case Some(s) if s.tp <:< op.R => s.asInstanceOf[R]
        case None => stageEffects(addToCache = mayCSE)
      }
  }

  /** For symbols corresponding to nodes: rewrite and flow rules on the given symbol and
    * add the symbol to the current scope.
    */
  @rig def restage[R](sym: Sym[R]): Sym[R] = sym match {
    case Op(rhs) =>
      val lhs: R = register[R](rhs, () => sym.unbox, {_ => ()})
      sym.tp.boxed(lhs)
    case _ => sym
  }


  /** Stage the given node as a symbol.
    * Also compute the effects of the node and adds them to the symbol as metadata and add
    * the symbol to the current scope.
    */
  @rig def stage[R](op: Op[R]): R = register[R](op, () => symbol(op.R,op), {t => t.ctx = ctx})


  /** Stage the given node as a symbol.
    * Use the data function to set symbol metadata _prior_ to @flow rules.
    * Also compute the effects of the node and adds them to the symbol as metadata and add
    * the symbol to the current scope.
    */
  @rig def stageWithData[R](op: Op[R])(data: Sym[R] => Unit): R = {
    register[R](op, () => symbol(op.R,op), {t => t.ctx = ctx; data(t) })
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
      val globalDep = state.impure.find{case Impure(_,e) => e.global; case _ => false } // global

      hazards ++ simpleDep ++ globalDep
    }
  }

  @rig final def allEffects(d: Op[_]): Effects = {
    val effects = propagateWrites(d.effects) andAlso Effects.Reads(d.mutableInputs)
    val deps = effectDependencies(effects)
    effects.copy(antiDeps = deps)
  }

  /** Used to allow nested ("atomic") writes, which are reflected on the top mutable object
    * rather than intermediates
    *
    * e.g.
    *   val b = Array(1, 2, 3)
    *   val a = MutableStruct(b, ...)
    *   a.b(0) = 1
    * Should become a write on (the mutable symbol) a instead of the immutable symbol resulting from a.b
    */
  @stateful final def recurseAtomicLookup(e: Sym[_]): Sym[_] = {
    e.op.flatMap{case d: AtomicRead[_] => Some(d.coll); case _ => None}.getOrElse(e)
  }
  @stateful final def extractAtomicWrite(s: Sym[_]): Sym[_] = {
    syms(recurseAtomicLookup(s)).headOption.getOrElse(s)
  }

  @stateful final def propagateWrites(effects: Effects): Effects = {
    if (!config.enableAtomicWrites) {
      val writes = effects.writes.flatMap{s => s.allAliases }
      effects.copy(writes = writes)
    }
    else {
      val writes = effects.writes.flatMap{s => extractAtomicWrite(s).allAliases }
      effects.copy(writes = writes)
    }
  }
}
