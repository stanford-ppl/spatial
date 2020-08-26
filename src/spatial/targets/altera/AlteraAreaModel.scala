package spatial.targets
package altera

import argon._
import forge.tags._
import models._

class AlteraAreaModel(target: AlteraDevice, mlModel: AreaEstimator) extends AreaModel(target, mlModel) {
  import AlteraDevice._

  @stateful override def summarize(area: Area): (Area, String) = {
    val design = area + model("Fringe")()

    val routingLUTs = 0 //calculateRoutingLUTs(design)
    val routingRegs = 0 //calculateRoutingRegs(design)
    //val unavailable = 0 //calculateUnavailALMs(design)

    val logicLUTs = design(LUT1)/2 + design(LUT2)/2 + design(LUT3)/2 + design(LUT4) + design(LUT5) + design(LUT6) + routingLUTs
    val memoryLUTs = RAMs.map{a => design(a)* RAM_LUT_USAGE(a) }.sum

    val logicSlices = logicLUTs / 4
    val memorySlices = memoryLUTs / 4

    val totalRegs = design(Regs) + routingRegs + design(Mregs)

    val regSlices = Math.max( ((totalRegs - ((logicSlices+design(SLICEL))*1.9) )/8).toInt, 0)

    val routingBRAM = 0 //Math.max(0.02*routingLUTs - 500, 0.0).toInt

    val totalDSPs = design(DSPs)
    val totalBRAM = design(RAM18)/2 + design(RAM36) + routingBRAM
    val totalURAM = design(URAM)

    val capacity = target.capacity

    val totalSLICEM = memorySlices + design(SLICEM)
    val totalSLICEL = logicSlices + design(SLICEL) + regSlices
    val totalSlices = totalSLICEM + totalSLICEL

    val areaReport = if (config.enInfo) {
      s"""
        |Resource Estimate Breakdown:
        |----------------------------
        |LUTs
        |  LUT1: ${design(LUT1)}
        |  LUT2: ${design(LUT2)}
        |  LUT3: ${design(LUT3)}
        |  LUT4: ${design(LUT4)}
        |  LUT5: ${design(LUT5)}
        |  LUT6: ${design(LUT6)}
        |  Routing: $routingLUTs
        |
        |Distributed Memory:
        |  RAM32X1S:  ${design(RAM32X1S)}
        |  RAM32X1D:  ${design(RAM32X1D)}
        |  RAM32M:    ${design(RAM32M)}
        |  RAM64X1S:  ${design(RAM64X1S)}
        |  RAM64X1D:  ${design(RAM64X1D)}
        |  RAM64M:    ${design(RAM64M)}
        |  RAM128X1S: ${design(RAM128X1S)}
        |  RAM128X1D: ${design(RAM128X1D)}
        |  RAM256X1S: ${design(RAM256X1S)}
        |  SRL16E:    ${design(SRL16E)}
        |  SRLC32E:   ${design(SRLC32E)}
        |
        |Registers
        |  Design:  ${design(Regs)}
        |  Memory:  ${design(Mregs)}
        |  Routing: $routingRegs
        |
        |Register-only Slices: $regSlices
        |Design Logic Slices:  ${design(SLICEL)}
        |Design Memory Slices: ${design(SLICEM)}
        |
        |URAM: $totalURAM / ${capacity(URAM)} (${"%.2f".format(100*totalURAM.toDouble/capacity(URAM))}%)
        |
        |BRAMs: $totalBRAM / ${capacity(BRAM)} (${"%.2f".format(100*totalBRAM.toDouble/capacity(BRAM))}%)
        |  RAM18:   ${design(RAM18)}
        |  RAM36:   ${design(RAM36)}
        |  Routing: $routingBRAM
        |
        |Slices: $totalSlices / ${capacity(Slices)} (${"%.2f".format(100*totalSlices.toDouble/capacity(Slices))}%)
        |  SLICEM: $totalSLICEM / ${capacity(SLICEM)} (${"%.2f".format(100*totalSLICEM.toDouble/capacity(SLICEM))}%)
        |  SLICEL: $totalSLICEL / ${capacity(SLICEL)} (${"%.2f".format(100*totalSLICEL.toDouble/capacity(SLICEL))}%)
        |
        |Resource Estimate Summary
        |-------------------------
        |Slices: $totalSlices / ${capacity(Slices)} (${"%.2f".format(100*totalSlices.toDouble/capacity(Slices))}%)
        |Regs:   $totalRegs
        |DSPs:   $totalDSPs / ${capacity(DSPs)} (${"%.2f".format(100*totalDSPs.toDouble/capacity(DSPs))}%)
        |BRAM:   $totalBRAM / ${capacity(BRAM)} (${"%.2f".format(100*totalBRAM.toDouble/capacity(BRAM))}%)
        |
         """.stripMargin
    }
    else ""

    val total = Area(
      SLICEM -> totalSLICEM,
      SLICEL -> totalSLICEL,
      Slices -> totalSlices,
      Regs   -> totalRegs,
      DSPs   -> totalDSPs,
      BRAM   -> totalBRAM,
      URAM   -> totalURAM
    )
    (total,areaReport)
  }
}

