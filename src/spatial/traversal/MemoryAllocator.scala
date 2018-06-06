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

    case class Instance(mem: Sym[_], memory: Memory, idx: Int)
    case class ProfiledInstance(inst: Instance, rawCount: Double, area: Area)


    def areaMetric(mem: Sym[_], inst: Memory, resource: MemoryResource): Double = {
      resource.summary(areaModel.rawMemoryArea(mem, inst, resource)) // sortBy: smallest to largest
    }

    // Memories which can use more specialized memory resources
    val (sramAble,nonSRAM) = localMems.all.partition(canSRAM)

    var unassigned: Set[Instance] = sramAble.flatMap{mem =>
      mem.duplicates.zipWithIndex.map{case (d,i) => Instance(mem,d,i) }
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
      val costs = unassigned.toSeq.map{case inst @ Instance(mem,dup,idx) =>
        val count = areaMetric(mem,dup,resource)      // Raw count of this memory type
        val area  = Area(resource.name -> count)      // Count as an area model map
        ProfiledInstance(inst, count, area)
      }

      var assigned: Set[Instance] = Set.empty

      // Terminate early if no memory can fit
      if (costs.isEmpty) {
        dbg(s"  Skipping ${resource.name} - all memories have been assigned!")
      }
      else if (costs.forall{t => t.area > capacity }) {
        dbg(s"  Skipping ${resource.name} - not enough resources remaining")
      }
      else {
        val sortedInsts = costs.sortBy{p => -p.rawCount}.iterator

        while (capacity(resource.name) > 0 && sortedInsts.hasNext) {
          val ProfiledInstance(inst, _, area) = sortedInsts.next()
          val Instance(mem, dup, idx) = inst
          val depth = areaModel.memoryBankDepth(mem, dup)

          if (area <= capacity && depth >= resource.minDepth) {
            dbg(s"  Assigned $mem#$idx to ${resource.name} (area: $area (<= $capacity), depth: $depth (>= ${resource.minDepth}))")
            dup.resourceType = Some(resource)
            capacity -= area
            assigned += inst
          }
          else if (area > capacity) {
            dbg(s"  Skipped $mem#$idx - too few resources (Needed: $area, Remaining: $capacity)")
          }
          else {
            dbg(s"  Skipped $mem#$idx - did not meet depth threshold (Depth: $depth, Threshold: ${resource.minDepth})")
          }
        }
      }
      unassigned = unassigned diff assigned
    }

    unassigned.foreach{inst => inst.memory.resourceType = Some(resources.last) }
    nonSRAM.foreach{mem => mem.duplicates.foreach{dup => dup.resourceType = Some(resources.last) }}
  }

}

