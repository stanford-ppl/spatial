package spatial.traversal

import argon._
import argon.passes.Pass
import models._

import spatial.data._
import spatial.util._
import spatial.internal.spatialConfig
import spatial.targets.MemoryResource

case class MemoryAllocator(IR: State) extends Pass {
  private def target = spatialConfig.target
  private def areaModel = target.areaModel
  implicit def AREA_FIELDS: AreaFields[Double] = areaModel.RESOURCE_FIELDS

  override protected def process[R](block: Block[R]): Block[R] = {
    allocate()
    block
  }

  private def canSRAM(mem: Sym[_]): Boolean = {
    mem.isSRAM || mem.isFIFO || mem.isLIFO
  }

  def allocate(): Unit = {
    val memories = localMems.all

    def areaMetric(mem: Sym[_], inst: Memory, resource: MemoryResource): Double = {
      // Negative because sortBy gives smallest to largest
      -resource.summary(areaModel.rawMemoryArea(mem, inst, resource))
    }

    // Memories which can use more specialized memory resources
    val (sramAble,nonSRAM) = memories.partition(canSRAM)

    var remaining: Set[(Sym[_],Memory,Int)] = sramAble.flatMap{mem =>
      duplicatesOf(mem).zipWithIndex.map{case (d,i) => (mem,d,i) }
    }

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
      val costs = remaining.map{case (mem,dup,i) =>
        val area = areaMetric(mem,dup,resource)
        val raw: Area  = Area(resource.name -> -area)
        //dbg(s"  ${str(mem)} [#$i]: ${-area}")
        (mem, dup, i, area, raw)
      }

      var idx: Int = 0
      var assigned: Set[Int] = Set.empty

      // Terminate early if no memory can fit
      if (costs.forall{t => t._5 > capacity }) {
        dbg(s"  Skipping ${resource.name} - not enough resources remaining")
      }
      else {
        val sortedInsts = costs.toSeq.sortBy(_._4)

        while (capacity(resource.name) > 0 && idx < sortedInsts.length) {
          val (mem, dup, d, _, area) = sortedInsts(idx)
          val size = constDimsOf(mem).product

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
      remaining = remaining.zipWithIndex.filterNot{case (_,i) => assigned.contains(i) }.map(_._1)
    }

    remaining.foreach{case (_,dup,_) => dup.resourceType = Some(resources.last) }
    nonSRAM.foreach{mem => duplicatesOf(mem).foreach{dup => dup.resourceType = Some(resources.last) }}
  }

}

