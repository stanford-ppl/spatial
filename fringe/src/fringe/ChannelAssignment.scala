package fringe

sealed trait ChannelAssignment {
  protected def numStreams: Int = globals.loadStreamInfo.size + globals.storeStreamInfo.size
  protected def numChannels: Int = globals.target.num_channels

  /** Divide up channels within memStreams into 'numChannels'.
    * Returns the IDs of load and store streams associated for each channel.
    **/
  def assignments: List[List[Int]]
}
/** Assigns all DRAMs to channel 0. */
object AllToOne extends ChannelAssignment {
  def assignments: List[List[Int]] = List.tabulate(numChannels){i => if (i == 0) (0 until numStreams).toList else Nil }
}

/** Assign DRAM i to channel i % numChannels. */
object BasicRoundRobin extends ChannelAssignment {
  def assignments: List[List[Int]] = List.tabulate(numChannels){i => (i until numStreams by numChannels).toList }
}

/** Assign DRAM i to color % numChannels, where color is computed in Spatial. */
object ColoredRoundRobin extends ChannelAssignment {
  def assignments: List[List[Int]] = List.tabulate(numChannels) { i =>
    globals.loadStreamInfo.zipWithIndex.collect{case (s, ii) if s.memChannel % numChannels == i => ii } ++
      globals.storeStreamInfo.zipWithIndex.collect{case (s, ii) if s.memChannel % numChannels == i => ii + globals.loadStreamInfo.size }
  }
}

/** Assign DRAM to color, based on Spatial's advanced allocation rules (accounting for transfer intensity). */
object AdvancedColored extends ChannelAssignment {
  // TODO: Complete the assignments definition for advanced coloring
  def assignments: List[List[Int]] = List.tabulate(numChannels){i => (i until numStreams by numChannels).toList }
}