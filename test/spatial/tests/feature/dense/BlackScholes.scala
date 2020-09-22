package spatial.tests.feature.dense

import spatial.dsl._

@spatial class BlackScholes extends SpatialTest {
  override def runtimeArgs: Args = "10000"

  val margin = 0.5f // Validates true if within +/- margin

  final val inv_sqrt_2xPI = 0.39894228040143270286f


  def CNDF(x: Float): Float = {
    val ax = abs(x)

    val xNPrimeofX = exp_taylor((ax ** 2) * -0.05f) * inv_sqrt_2xPI
    val xK2 = 1.to[Float] / ((ax * 0.2316419f) + 1.0f)

    val xK2_2 = xK2 ** 2
    val xK2_3 = xK2_2 * xK2
    val xK2_4 = xK2_3 * xK2
    val xK2_5 = xK2_4 * xK2

    val xLocal_10 = xK2 * 0.319381530f
    val xLocal_20 = xK2_2 * -0.356563782f
    val xLocal_30 = xK2_3 * 1.781477937f
    val xLocal_31 = xK2_4 * -1.821255978f
    val xLocal_32 = xK2_5 * 1.330274429f

    val xLocal_21 = xLocal_20 + xLocal_30
    val xLocal_22 = xLocal_21 + xLocal_31
    val xLocal_23 = xLocal_22 + xLocal_32
    val xLocal_1 = xLocal_23 + xLocal_10

    val xLocal0 = xLocal_1 * xNPrimeofX
    val xLocal  = -xLocal0 + 1.0f

    mux(x < 0.0f, xLocal0, xLocal)
  }


  def BlkSchlsEqEuroNoDiv(
    sptprice:   Float,
    strike:     Float,
    rate:       Float,
    volatility: Float,
    time:       Float,
    otype:      Int
  ): Float = {

    val xLogTerm = log_taylor( sptprice / strike )
    val xPowerTerm = (volatility ** 2) * 0.5f
    val xNum = (rate + xPowerTerm) * time + xLogTerm
    val xDen = volatility * sqrt_approx(time)

    val xDiv = xNum / (xDen ** 2)
    val nofXd1 = CNDF(xDiv)
    val nofXd2 = CNDF(xDiv - xDen)

    val futureValueX = strike * exp_taylor(-rate * time)

    val negNofXd1 = -nofXd1 + 1.0f
    val negNofXd2 = -nofXd2 + 1.0f

    val optionPrice1 = (sptprice * nofXd1) - (futureValueX * nofXd2)
    val optionPrice2 = (futureValueX * negNofXd2) - (sptprice * negNofXd1)
    mux(otype == 0, optionPrice2, optionPrice1)
  }


  def blackscholes(
    stypes:      Array[Int],
    sprices:     Array[Float],
    sstrike:     Array[Float],
    srate:       Array[Float],
    svolatility: Array[Float],
    stimes:      Array[Float]
  ): Array[Float] = {
    val B  = loadParam("ts", 4 (96 -> 96 -> 19200))
    val op = loadParam("op", 1 (1 -> 2))
    val ip = loadParam("ip", 16 (1 -> 96))
    val lp = loadParam("lp", 4 (1,2,4,8,16))

    val size = stypes.length; bound(size) = 9995328

    val N = ArgIn[Int]
    setArg(N, size)

    val types    = DRAM[Int](N)
    val prices   = DRAM[Float](N)
    val strike   = DRAM[Float](N)
    val rate     = DRAM[Float](N)
    val vol      = DRAM[Float](N)
    val times    = DRAM[Float](N)
    val optprice = DRAM[Float](N)
    setMem(types, stypes)
    setMem(prices, sprices)
    setMem(strike, sstrike)
    setMem(rate, srate)
    setMem(vol, svolatility)
    setMem(times, stimes)

    Accel {
      Foreach(N by B par op) { i =>
        val typeBlk   = SRAM[Int](B)
        val priceBlk  = SRAM[Float](B)
        val strikeBlk = SRAM[Float](B)
        val rateBlk   = SRAM[Float](B)
        val volBlk    = SRAM[Float](B)
        val timeBlk   = SRAM[Float](B)
        val optpriceBlk = SRAM[Float](B)

        // Parallel {
        typeBlk   load types(i::i+B par lp)
        priceBlk  load prices(i::i+B par lp)
        strikeBlk load strike(i::i+B par lp)
        rateBlk   load rate(i::i+B par lp)
        volBlk    load vol(i::i+B par lp)
        timeBlk   load times(i::i+B par lp)
        // }

        Foreach(B par ip){ j =>
          val price = BlkSchlsEqEuroNoDiv(priceBlk(j), strikeBlk(j), rateBlk(j), volBlk(j), timeBlk(j), typeBlk(j))
          optpriceBlk(j) = price
        }
        optprice(i::i+B par lp) store optpriceBlk
      }
    }
    getMem(optprice)
  }


  def main(args: Array[String]): Unit = {
    val N = args(0).to[Int]

    val types  = Array.fill(N)(random[Int](2))
    val prices = Array.fill(N)(random[Float])
    val strike = Array.fill(N)(random[Float])
    val rate   = Array.fill(N)(random[Float])
    val vol    = Array.fill(N)(random[Float])
    val time   = Array.fill(N)(random[Float])

    val out = blackscholes(types, prices, strike, rate, vol, time)
    val gold = Array.tabulate(N){i =>
      BlkSchlsEqEuroNoDiv(prices(i), strike(i), rate(i), vol(i), time(i), types(i))
    }

    printArray(out, "result")
    printArray(gold, "gold")

    val cksum = out.zip(gold){(o,g) => (g < (o + margin)) && g > (o - margin)}.reduce{_&&_}
    assert(cksum)
  }
}
