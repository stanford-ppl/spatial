package argon
package passes

import utils.implicits.Readable._
import utils.tags.instrument


/** Common trait for all passes which can be run by the compiler,
  * including analysis, code generation, and transformers
  *
  * Extend this trait directly if you don't need to traverse the graph.
  * Otherwise, extend Traversal.
  */
@instrument trait Pass { self =>
  val IR: State
  final implicit def __IR: State = IR
  def name: String = r"${self.getClass}".split('.').last
  def logFile: String = state.paddedPass + "_" + name + ".log"

  var enWarn: Option[Boolean] = None
  var enError: Option[Boolean] = None
  var enInfo: Option[Boolean] = None
  var logLevel: Option[Int] = None

  var needsInit: Boolean = true
  def shouldRun: Boolean = true
  def silence(): Unit = {
    enWarn = Some(false)
    enError = Some(false)
    enInfo = Some(false)
    logLevel = Some(0)
  }
  def init(): Unit = { needsInit = false }

  /** Performance debugging */
  var lastTime  = 0.0f
  var totalTime = 0.0f

  /** Run method - called internally from compiler */
  final def run[R](block: Block[R]): Block[R] = if (shouldRun) {
    state.pass += 1
    config.withVerbosity(
      warn  = enWarn.getOrElse(config.enWarn),
      error = enError.getOrElse(config.enError),
      info  = enInfo.getOrElse(config.enInfo),
      log   = logLevel.getOrElse(config.logLevel)
    ) {
      withLog(config.logDir, logFile) {
        val start = System.currentTimeMillis()
        val result = execute(block)
        val time = (System.currentTimeMillis - start).toFloat
        lastTime = time
        totalTime += time
        result
      }
    }
  } else block

  /** Called to execute this pass. Override to implement custom IR processing */
  protected def execute[R](block: Block[R]): Block[R] = {
    val b2 = preprocess(block)
    val b3 = process(b2)
    postprocess(b3)
  }

  /** Called before the top-level block is traversed. */
  protected def preprocess[R](block: Block[R]): Block[R] = { block }

  /** Called to run the main part of this traversal. */
  protected def process[R](block: Block[R]): Block[R]

  /** Called after the top-level block is completely traversed. */
  protected def postprocess[R](block: Block[R]): Block[R] = { block }

}
