package utils

import utils.implicits.Readable._

import java.io.PrintStream
import scala.collection.mutable.ArrayBuffer

trait Instrumented { self =>
  val fullName: String = r"${self.getClass}"
  val instrumentName: String = r"${self.getClass}".split('.').last
  lazy val instrument: Instrument = new Instrument(instrumentName)
  Instrumented.set += this

  def resetInstrument(): Unit = instrument.reset()

  def dumpInstrument(name: String = instrumentName, out: PrintStream = Console.out): Unit = {
    instrument.dump(name, out)
  }

  def dumpAllInstrument(out: PrintStream = Console.out): Unit = instrument.dumpAll(out)

}

object Instrumented {
  val set: ArrayBuffer[Instrumented] = ArrayBuffer.empty
}
