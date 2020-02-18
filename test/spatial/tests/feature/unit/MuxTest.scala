package spatial.tests.feature.unit

import spatial.dsl._

@spatial class MuxTest extends SpatialTest {
  override def runtimeArgs: Args = "0 0 1 0 1 0 0 0 0 0 0 0 0 0 0 0 0"

  def main(args: Array[String]): Unit = {
    val numel = 17
    val sels = List.tabulate(numel){i => ArgIn[Int]}
    val ins = List.tabulate(numel){i => ArgIn[Int]}

    val mux1h = ArgOut[Int]
    val muxp = ArgOut[Int]

    sels.zipWithIndex.foreach{case (s, i) => setArg(s, args(i).to[Int])}
    ins.zipWithIndex.foreach{case (s, i) => setArg(s, (i+1).to[Int])}

    Accel {
      mux1h := oneHotMux(sels.map(_.value == 1.to[Int]), ins.map(_.value)) // Take all enabled entries and 'or' them together
      muxp := priorityMux(sels.map(_.value == 1.to[Int]), ins.map(_.value)) // Take first enabled entry
    }

    val hot = getArg(mux1h)
    val pri = getArg(muxp)

    val goldhot = List.tabulate(numel){i => if (args(i).to[Int] == 0) 0 else (i+1)}.reduce{_|_}
    val goldpri = List.tabulate(numel){i =>
      if (i == 0) if (args(i).to[Int] == 1) (i+1) else 0
      else {
        if (List.tabulate(i){j => args(j).to[Int] == 0}.reduce{_&&_} && args(i).to[Int] == 1) (i+1) else 0
      }
    }.reduce{_|_}

    println(r"Mux1H got ${hot} =?= $goldhot hot")
    println(r"Priority got ${pri} =?= $goldpri hot")

    val cksum = goldhot == hot && goldpri == pri
    println("PASS: " + cksum + " (InOutArg)")
    assert(cksum)
  }
}

