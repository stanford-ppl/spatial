package spatial.traversal

import argon._
import argon.passes.Pass
import models._

import spatial.data._
import spatial.util._
import spatial.targets.MemoryResource

case class MemoryAllocator(IR: State) extends Pass {
  implicit def AREA_FIELDS: AreaFields[Double] = areaModel.RESOURCE_FIELDS

  override protected def process[R](block: Block[R]): Block[R] = {
    allocate()
    block
  }

  private def canSRAM(mem: Sym[_]): Boolean = {
    mem.isSRAM || mem.isFIFO || mem.isLIFO
  }

  def allocate(): Unit = {

    def areaMetric(mem: Sym[_], inst: Memory, resource: MemoryResource): Double = {
      -resource.summary(areaModel.rawMemoryArea(mem, inst, resource)) // sortBy: smallest to largest
    }

    // Memories which can use more specialized memory resources
    val (sramAble,nonSRAM) = localMems.all.partition(canSRAM)

    var unassigned: Set[(Sym[_],Memory,Int)] = sramAble.flatMap{mem => duplicatesOf(mem).zipWithIndex.map{case (d,i) => (mem,d,i) }}

    dbg(s"\n\n")
    dbg("Allocating Memory Resources")
    dbg("---------------------------")
    dbg("Capacity: ")
    var capacity: Area = target.capacity
    capacity.foreach{case (k,v) => dbg(s"  $k: $v") }

    // Desired order:
    //   Large (within cutoffs, e.g. >= 512 for URAM)
    //   Medium (within cutoffs, e.g. < 512, >= 32 for BRAM)
    //   Large  (without cutoffs)
    //   Medium (without cutoffs)
    //   Small

    val resources = target.memoryResources

    // All but the last
    resources.dropRight(1).foreach{resource =>
      dbg(s"Allocating ${resource.name}: ")
      // Sort by the expected resource utilization of this memory
      val costs = unassigned.map{case (mem,dup,i) =>
        val area = areaMetric(mem,dup,resource)
        val raw: Area  = Area(resource.name -> -area)
        //dbg(s"  ${str(mem)} [#$i]: ${-area}")
        (mem, dup, i, area, raw)
      }

      var idx: Int = 0
      var assigned: Set[Int] = Set.empty

      // Terminate early if no memory can fit
      if (costs.isEmpty) {
        dbg(s"  Skipping ${resource.name} - all memories have been assigned!")
      }
      else if (costs.forall{t => t._5 > capacity }) {
        dbg(s"  Skipping ${resource.name} - not enough resources remaining")
      }
      else {
        val sortedInsts = costs.toSeq.sortBy(_._4)

        while (capacity(resource.name) > 0 && idx < sortedInsts.length) {
          val (mem, dup, d, _, area) = sortedInsts(idx)
          val size = areaModel.memoryBankDepth(mem,dup)

          if (area <= capacity && size >= resource.minDepth) {
            dbg(s"  Assigned $mem#$d to ${resource.name} (uses $area)")
            dup.resourceType = Some(resource)
            capacity -= area
            assigned += idx
          }
          else if (area > capacity) {
            dbg(s"  Skipped $mem#$d - too few resources (Needed: $area, Remaining: $capacity)")
          }
          else {
            dbg(s"  Skipped $mem#$d - did not meet depth threshold (Depth: $size, Threshold: ${resource.minDepth})")
          }

          idx += 1
        }
      }
      unassigned = unassigned.filterNot{case (mem,d,i) => assigned.contains(i) }
    }

    unassigned.foreach{case (mem,dup,_) => dup.resourceType = Some(resources.last) }
    nonSRAM.foreach{mem => duplicatesOf(mem).foreach{dup => dup.resourceType = Some(resources.last) }}
  }

}

