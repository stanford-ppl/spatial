package spatial.tests.ee109

import scala.collection.mutable.ArrayBuffer

class Solver(capacity: Int, sizes: Array[Int], values: Array[Int]) {

  private val unknown: Int = -1
  private val nItems = sizes.length
  private val DPArray = ArrayBuffer.tabulate(nItems)(_ =>
    ArrayBuffer.tabulate[Int](capacity + 1)(_ => unknown))

  def solveKnapsackRecursive(): Int =
    getRecursiveKnapsackSolution(nItems, capacity)

  def solveKnapsackDP(): Int = getDPKnapsackSolution(nItems, capacity)

  def solveKnapsackIterative(): Unit = createKnapsackIterativeSolution()

  private def createKnapsackIterativeSolution(): Unit = {
    for (nIdx <- 0 until nItems) {
      val vn = values(nIdx)
      val sn = sizes(nIdx)
      for (sIdx <- 0 to capacity) {
        val scanRowIdx = nIdx - 1
        if (sn > sIdx) {
          val notPickedFromSizeLimitVal =
            if (scanRowIdx < 0) 0 else DPArray(scanRowIdx)(sIdx)
          DPArray(nIdx)(sIdx) = notPickedFromSizeLimitVal
        } else {
          val pickedCandidate = sIdx - sn
          val notPickedCandidate = sIdx
          val pickedVal = vn + (if (scanRowIdx < 0) 0
          else DPArray(scanRowIdx)(pickedCandidate))
          val notPickedVal =
            if (scanRowIdx < 0) 0 else DPArray(scanRowIdx)(notPickedCandidate)
          DPArray(nIdx)(sIdx) = math.max(pickedVal, notPickedVal)
        }
      }
    }
  }

  def printDPArray(): Unit =
    DPArray.foreach(m => scala.Console.println(m.mkString(", ")))

  def reportPath(): (ArrayBuffer[Int], ArrayBuffer[Int], ArrayBuffer[Int]) = {
    val pickedBuckets :: pickedValues :: pickedSizes :: Nil =
      List.tabulate(3)(_ => new ArrayBuffer[Int]())
    var sIdx = capacity
    var nIdx = nItems - 1
    var nextIdx = nIdx - 1

    while (nIdx >= 0) {
      val currentScore = DPArray(nIdx)(sIdx)
      val peekScore = if (nIdx == 0) unknown else DPArray(nextIdx)(sIdx)
      if (currentScore != peekScore) {
        val sn = sizes(nIdx)
        pickedBuckets.append(nIdx)
        pickedValues.append(values(nIdx))
        pickedSizes.append(sizes(nIdx))
        sIdx = sIdx - sn
      }

      nIdx = nextIdx
      nextIdx = nIdx - 1
    }

    (pickedBuckets, pickedValues, pickedSizes)
  }

  private def getRecursiveKnapsackSolution(n: Int, S: Int): Int = {
    val nIdx = n - 1
    n match {
      case 0 => 0
      case _ =>
        val sn = sizes(nIdx)
        sn match {
          case x if x > S =>
            getRecursiveKnapsackSolution(n - 1, S)
          case _ =>
            val vn = values(nIdx)
            val nextVn = n - 1
            val sPicked = S - sn
            val sNotPicked = S
            math.max(vn + getRecursiveKnapsackSolution(nextVn, sPicked),
              getRecursiveKnapsackSolution(nextVn, sNotPicked))
        }
    }
  }

  private def getDPKnapsackSolution(n: Int, S: Int): Int = {
    val nIdx = n - 1
    val SIdx = S
    n match {
      case 0 => 0
      case _ =>
        val m = DPArray(nIdx)(SIdx)
        m match {
          case x if x != unknown => m
          case _ =>
            val sn = sizes(nIdx)
            sn match {
              case x if x > S => getDPKnapsackSolution(n - 1, S)
              case _ =>
                val vn = values(nIdx)
                val nextVn = n - 1
                val sPicked = S - sn
                val sNotPicked = S
                val result = math.max(
                  vn + getDPKnapsackSolution(nextVn, sPicked),
                  getDPKnapsackSolution(nextVn, sNotPicked))
                DPArray(nIdx)(SIdx) = result
                result
            }
        }
    }
  }
}
