def reductionTreeDelays(nLeaves: Int): List[Long] = {
  val binary = Array.tabulate(16) { i => (nLeaves & (1 << i)) > 0 }
  val partialTrees = binary.zipWithIndex.filter(_._1).map(_._2)
  var leftHeight = partialTrees.head
  var i = 1
  var dlys: List[Long] = Nil
  while (i < partialTrees.length) {
    val rightHeight = partialTrees(i)
    if (rightHeight > leftHeight) dlys ::= (rightHeight - leftHeight).toLong
    leftHeight = rightHeight + 1
    i += 1
  }
  dlys
}


reductionTreeDelays(32)

reductionTreeDelays(85)

reductionTreeDelays(100)

reductionTreeDelays(75)

reductionTreeDelays(16)
reductionTreeDelays(4)