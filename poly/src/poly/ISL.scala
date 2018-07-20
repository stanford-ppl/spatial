package poly

import utils.process.BackgroundProcess

/**
  * Self-initializing object that runs external polyhedral ISL processes
  */
trait ISL {
  private lazy val HOME = sys.env.getOrElse("NOVA_HOME", ".")
  private lazy val proc = BackgroundProcess(s"$HOME/poly/", "./emptiness")
  private var needsInit: Boolean = true
  implicit def isl: ISL = this

  private def init(): Unit = if (needsInit) {
    proc.run()
    needsInit = false
  }
  def startup(): Unit = init()
  def shutdown(wait: Long = 0): Unit = { proc.kill(wait) }

  // This needs to be defined by the instantiator
  def domain[K](key: K): ConstraintMatrix[K]

  def nonEmpty[K](constraint: SparseConstraint[K]): Boolean = !isEmpty(constraint)
  def isEmpty[K](constraint: SparseConstraint[K]): Boolean = isEmpty(ConstraintMatrix(Set(constraint)))

  def nonEmpty[K](constraints: ConstraintMatrix[K]): Boolean = !isEmpty(constraints)
  def isEmpty[K](constraints: ConstraintMatrix[K]): Boolean = {
    val matrix = constraints.toDenseString
    //dbg(matrix)
    proc.send(matrix)
    val response = proc.blockOnChar()
    //dbg(response)

    if (response == '0') true
    else if (response == '1') false
    else throw new Exception("Failed isEmpty check")
  }

  /** True if there exists a reachable multi-dimensional index I such that addr_a(I) = addr_b(I).
    * True if all given dimensions may intersect.
    * Trivially true for random accesses with no constraints.
    */
  def overlapsAddress[K](a: SparseMatrix[K], b: SparseMatrix[K]): Boolean = {
    val equal = (a - b) === 0
    nonEmpty(equal.andDomain)
  }

  /** True if the space of addresses in a is statically known to include all of the addresses in b. */
  def isSuperset[K](a: SparseMatrix[K], b: SparseMatrix[K]): Boolean = {
    // TODO[3) Implement isSuperset in ISL. (Used for reaching write calculation)
    false
  }

  /** True if the space of addresses in a and b may have at least one element in common. */
  def intersects[K](a: SparseMatrix[K], b: SparseMatrix[K]): Boolean = {
    // TODO[3) Implement intersects in ISL. (Used for reaching write calculation)
    true
  }
}
