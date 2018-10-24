package poly

import utils.process.BackgroundProcess

import scala.language.postfixOps
/**
  * Self-initializing object that runs external polyhedral ISL processes
  */
trait ISL {
  private lazy val proc = {
    // get path to emptiness

    val emptiness_bin = s"""${sys.env.getOrElse("HOME", "")}/bin/emptiness"""

    val emptiness_lock = java.nio.file.Paths.get(emptiness_bin + ".lock")
    val emptiness_path = java.nio.file.Paths.get(emptiness_bin)
    val emptiness_exists = java.nio.file.Files.exists(emptiness_path)
    println(s"Emptiness: $emptiness_exists, $emptiness_bin")

    if (!emptiness_exists) {
      // Acquire a file lock
      val channel = {
        try {
          java.nio.channels.FileChannel.open(emptiness_lock, java.nio.file.StandardOpenOption.CREATE_NEW)
        } catch {
          case _ : Throwable => java.nio.channels.FileChannel.open(emptiness_lock, java.nio.file.StandardOpenOption.CREATE)
        }
      }
//      val channel = java.nio.channels.FileChannel.open(emptiness_lock, java.nio.file.StandardOpenOption.CREATE)
      val lock = channel.tryLock()
      try {
        if (lock == null) {
          // Someone else is handling the compile. Wait for the file to become available.
          println("Another process is making the emptiness bin.")
          val wait = channel.lock()
          wait.release()
        } else {
          // compile emptiness

          // gcc command: CC resources/emptiness.c -o emptiness  `pkg-config --cflags --libs isl`

          {
            import java.nio.file.Files
            val create_path = java.nio.file.Paths.get(emptiness_bin).getParent
            Files.createDirectories(create_path)
          }

          val pkg_config_proc = BackgroundProcess("", "pkg-config", "--cflags", "--libs", "isl")
          val pkg_config = pkg_config_proc.blockOnLine()

          println(s"Pkg Config: $pkg_config")

          val split_config = pkg_config.split("\\s") map {
            _.trim
          } filter {
            _.nonEmpty
          }

          val compile_proc = BackgroundProcess("",
            List(s"${sys.env.getOrElse("CC", "gcc")}", s"-xc", "-o", s"$emptiness_bin", "-") ++ split_config)
          val source_string = scala.io.Source.fromInputStream(
            getClass.getClassLoader.getResourceAsStream("emptiness.c")).mkString
          println(source_string)
          compile_proc send source_string
          val retcode = compile_proc.waitFor()
          println(s"Compile Retcode: $retcode")
          compile_proc.checkErrors()

          println("Finished Compiling")
          lock.release()
        }
      } finally {
        if (lock != null) {
          lock.release()
          channel.close()
        }

      }
    }



    BackgroundProcess("", emptiness_bin)
  }
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
