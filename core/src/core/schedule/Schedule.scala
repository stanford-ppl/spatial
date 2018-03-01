package core
package schedule

/** Represents the schedule output of a scope.
  * @param block a [[Block]] representing the schedule for the given scope
  * @param motioned remaining symbols which should be motioned out of the scope
  * @param motionedImpure impure symbols which should be motioned out of the scope (this is rare)
  */
case class Schedule[R](
  block:    Block[R],
  motioned: Seq[Sym[_]],
  motionedImpure: Seq[Impure] = Nil
)

