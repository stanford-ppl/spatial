package poly

import java.nio.channels.{FileLock, OverlappingFileLockException}
import java.nio.file.StandardOpenOption.{CREATE_NEW, WRITE}

import utils.process.BackgroundProcess

import scala.language.postfixOps

/**
  * Self-initializing object that runs external polyhedral ISL processes
  */
trait ISL {
  private lazy val proc = {
    val user_bin = s"""${sys.env.getOrElse("HOME", "")}/bin"""
    val bin_path = java.nio.file.Paths.get(user_bin)
    val bin_exists = java.nio.file.Files.exists(bin_path)
    if (!bin_exists) {
      java.nio.file.Files.createDirectories(bin_path)
    }


    val emptiness_bin = s"""${user_bin}/emptiness"""

    val emptiness_lock = java.nio.file.Paths.get(emptiness_bin + ".lock")
    val emptiness_path = java.nio.file.Paths.get(emptiness_bin)

    {
      // step 1: Acquire channel to emptiness_lock
      val channel = {
        try {
          java.nio.channels.FileChannel.open(emptiness_lock, CREATE_NEW, WRITE)
        } catch {
          case _: Throwable => java.nio.channels.FileChannel.open(emptiness_lock, WRITE)
        }
      }

      // step 2: Acquire lock on emptiness_lock, spin-wait otherwise.
      var lock: FileLock = null
      while (lock == null) {
        try {
          lock = channel.lock()
        } catch {
          case ofe: OverlappingFileLockException => Unit
        }
      }

      // step 3: check if emptiness exists
      val emptiness_exists = java.nio.file.Files.exists(emptiness_path)
      println(s"Emptiness: $emptiness_exists, $emptiness_bin")

      // step 4: if emptiness does not exist, compile it.
      if (!emptiness_exists) {
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
      }

      // step 4: Now that emptiness is guaranteed to exist, release lock
      lock.release()


      // close FileChannel
      channel.close()
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

  def shutdown(wait: Long = 0): Unit = {
    proc.kill(wait)
  }

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
