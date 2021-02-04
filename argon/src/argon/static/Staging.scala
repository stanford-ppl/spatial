package argon.static

import forge.tags._
import utils.implicits.collections._
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

  @api def boundVar[A:Type]: A = Type[A]._new(Def.Bound[A](state.nextId()), ctx)

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
        error(sym.ctx, s"Illegal mutation of immutable ${s.nameOr("value")}")
        error(sym.ctx)
        error(s.ctx, s"${s.nameOr("value")} originally defined here")
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

  /** Creates and registers a symbol for the given operation.
    *
    * 0. Check that we are in a valid staging scope.
    * 1. Check for any matching rewrite rules, rewrites the op to some subgraph if possible.
    * 2. Check for subexpression elimination opportunities. If any exist, uses existing symbol.
    * 3. Create a new symbol
    * 4. Add the symbol to the scope, CSE cache
    * 5. Set the symbol’s effects and aliases, register reverse aliases
    * 6. Set the consumers metadata (forward edges) for all inputs
    * 7. Set “immediate” (stageWithMetadata) metadata
    * 8. Run @flow passes registered with the compiler (more later)
    * 9. Check for mutable aliases, mutation of immutable values
    */
  @rig def register[R](op: Op[R], symbol: () => R, flowImmediate: Sym[R] => Unit): R = {
    if ((state eq null) || (state.scope eq null) || (state.impure eq null)) {
      // General operations in object/trait constructors are currently disallowed
      // because it can mess up namespaces in the output code
      // TODO[6]: Any cases where we should allow global namespace operations?
      error(ctx, "Only constant declarations are allowed in the global namespace.")
      error(ctx)
      err_[R](op.R, "Invalid declaration in global namespace")
    }
    // 1. Attempt to rewrite the operation to a subgraph
    else rewrite(op) match {
      case Some(s) => s
      case None    =>
        val sAliases = op.shallowAliases
        val dAliases = op.deepAliases

        val effects = computeEffects(op)
        val mayCSE = effects.mayCSE

        // 2. Check for CSE opportunities
        state.cache.get(op).filter{s => mayCSE && s.effects == effects} match {
          case Some(s) if s.tp <:< op.R => s.asInstanceOf[R]  // Use existing symbol
          case None =>
            // 3. Create a symbol
            val lhs = symbol()
            val sym = op.R.boxed(lhs)

            // 4. Add symbol to scope, CSE cache
            state.scope :+= sym
            if (effects.mayCSE)  state.cache += op -> sym                 // Add to CSE cache

            // 5. Set effects and aliases
            if (!effects.isPure) state.impure :+= Impure(sym,effects)     // Add to list of impure syms
            sym.effects = effects                                         // Set effects
            sym.deepAliases = dAliases                                    // Set deep aliases
            sym.shallowAliases = sAliases                                 // Set shallow aliases
            sym.allAliases.foreach{alias => alias.shallowAliases += sym } // Register reverse aliases

            // 6. Set consumer metadata
            op.inputs.foreach{in => in.consumers += sym }                 // Register consumed

            // 7. Run immediate (stageWithFlow)
            // (Includes transferring metadata during mirror for transformers)
            flowImmediate(sym)

            // 8. Run @flow passes
            // (First includes transferring metadata during transform)
            runFlows(sym,op)

            if (config.enLog) {
              logs(s"$lhs = $op")
              strMeta(sym)
            }

            // 9. Check for mutable aliases, mutation of immutable values
            checkAliases(sym, effects)

            lhs
        }
    }
  }

  /** For symbols corresponding to nodes: rewrite and flow rules on the given symbol and
    * add the symbol to the current scope.
    */
  @rig def restage[R](sym: Sym[R]): Sym[R] = restageWithFlow(sym){_ => ()}

  @rig def restageWithFlow[R](sym: Sym[R])(flow: Sym[R] => Unit): Sym[R] = sym match {
    case Op(rhs) =>
      val lhs: R = register[R](rhs, () => sym.unbox, flow)
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
  @rig def stageWithFlow[R](op: Op[R])(flow: Sym[R] => Unit): R = {
    register[R](op, () => symbol(op.R,op), {t => t.ctx = ctx; flow(t) })
  }

  /** Add the given flow rule(s) for the duration of the given scope. */
  @stateful def withFlow[A](name: String, flow: Sym[_] => Unit, prepend: Boolean = false)(scope: => A): A = {
    val rule: PartialFunction[(Sym[_],Op[_],SrcCtx,State),Unit] = {case (lhs,_,_,_) => flow(lhs) }
    val saveFlows = state.flows.save()
    if (prepend) state.flows.prepend(name, rule) else state.flows.add(name, rule)
    val result = scope
    state.flows.restore(saveFlows)
    result
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

  @rig final def computeEffects(d: Op[_]): Effects = {
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

  final def exps(a: Any*): Set[Sym[_]] = a.flatMap{
    case s: Sym[_] if !s.isType => Seq(s)
    case s: Impure              => exps(s.sym)
    case b: Block[_]            => (b.result +: b.effects.antiDeps.map(_.sym)).filterNot(_.isType)
    case d: Op[_]               => d.expInputs
    case i: Iterator[_]         => i.flatMap(e => exps(e))
    case i: Iterable[_]         => i.flatMap(e => exps(e))
    case p: Product             => p.productIterator.flatMap(e => exps(e))
    case _ => Nil
  }.toSet

  final def syms(a: Any*): Set[Sym[_]] = a.flatMap{
    case s: Sym[_] if s.isSymbol => Seq(s)
    case s: Sym[_] if s.isBound  => Seq(s)
    case s: Impure               => syms(s.sym)
    case b: Block[_]             => (b.result +: b.effects.antiDeps.map(_.sym)).filter(s => s.isBound || s.isSymbol)
    case d: Op[_]                => d.inputs
    case i: Iterator[_]          => i.flatMap(e => syms(e))
    case i: Iterable[_]          => i.flatMap(e => syms(e))
    case p: Product              => p.productIterator.flatMap(e => syms(e))
    case _ => Nil
  }.toSet

  final def collectBlocks(a: Any*): Seq[Block[_]] = a.flatMap{
    case b: Block[_]             => Seq(b)
    case d: Op[_]                => d.blocks
    case i: Iterator[_]          => i.flatMap(e => collectBlocks(e))
    case i: Iterable[_]          => i.flatMap(e => collectBlocks(e))
    case p: Product              => p.productIterator.flatMap(e => collectBlocks(e))
    case _ => Nil
  }

}
