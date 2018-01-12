package pcc.traversal

import pcc.core._
import pcc.util.{Instrument,NoInstrument}

/**
  * Common trait for all passes which can be run by the compiler,
  * including analysis, code generation, and transformers
  *
  * Extend this trait directly if you don't need to traverse the graph.
  * Otherwise, extend Traversal.
  */
trait Pass { self =>
  val IR: State
  final implicit def __IR: State = IR
  def name: String = self.getClass.toString
  def logFile: String = state.paddedPass + " " + name + ".log"

  var verbosity: Option[Int] = None
  var needsInit: Boolean = true
  def shouldRun: Boolean = true
  def silence(): Unit = { verbosity = Some(-2) }
  def init(): Unit = { needsInit = false }

  /** Performance debugging **/
  var lastTime  = 0.0f
  var totalTime = 0.0f
  protected lazy val instrument: Instrument = new NoInstrument("top")

  /** Run method - called internally from compiler **/
  final def run[R](block: Block[R]): Block[R] = if (shouldRun) {
    state.pass += 1
    withLog(config.logDir, logFile){
      val start = System.currentTimeMillis()
      val result = process(block)
      val time = (System.currentTimeMillis - start).toFloat
      lastTime = time
      totalTime += time
      result
    }
  } else block

  /** Called to execute this pass. Override to implement custom IR processing **/
  protected def process[R](block: Block[R]): Block[R]
}
