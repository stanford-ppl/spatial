package nova.poly

import forge.tags._
import forge.process.BackgroundProc
import nova.core._

/**
  * Self initializing object that runs
  */
case class ISL()(implicit state: State) {
  private lazy val HOME = sys.env.getOrElse("PCC_HOME", ".")
  private lazy val proc = BackgroundProc(s"$HOME/isl/", "./emptiness")
  private var needsInit: Boolean = true

  private def init(): Unit = if (needsInit) {
    try { proc.run() }
    catch {case t:Throwable =>
      bug(s"Could not open the ISL subprocess.")
      throw t
    }
    needsInit = false
  }
  def startup(): Unit = init()
  def shutdown(wait: Long = 0): Unit = { proc.kill(wait) }

  def nonEmpty(constraint: SparseConstraint): Boolean = !isEmpty(constraint)
  def isEmpty(constraint: SparseConstraint): Boolean = isEmpty(ConstraintMatrix(Set(constraint)))

  def nonEmpty(constraints: ConstraintMatrix): Boolean = !isEmpty(constraints)
  def isEmpty(constraints: ConstraintMatrix): Boolean = {
    val matrix = constraints.toDenseString
    //dbg(matrix)
    proc.send(matrix)
    val response = proc.blockOnChar()
    //dbg(response)

    if (response == '0') true
    else if (response == '1') false
    else throw new Exception("Failed isEmpty check")
  }

  /**
    * Returns true if there exists a reachable multi-dimensional index I such that addr_a(I) = addr_b(I).
    * True if all given dimensions may intersect. Trivially true for random accesses with no constraints.
    */
  def overlaps(a: SparseMatrix, b: SparseMatrix): Boolean = {
    val equal = (a - b).asConstraintEqlZero
    nonEmpty(equal.andDomain)
  }

  /**
    * Returns true if the space of addresses in a is statically known to include all of the addresses in b.
    * TODO: Used for reaching write calculation
    */
  def isSuperset(a: SparseMatrix, b: SparseMatrix): Boolean = {
    false
  }

  /**
    * Returns true if the space of addresses in a and b may have at least one element in common.
    * TODO: Used for reaching write calculation
    */
  def intersects(a: SparseMatrix, b: SparseMatrix): Boolean = {
    true
  }
}
