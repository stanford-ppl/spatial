package forge

import java.io.PrintStream

import forge.implicits.readable._

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
}

object Instrumented {
  val set: ArrayBuffer[Instrumented] = ArrayBuffer.empty
}
