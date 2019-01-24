package fringe

import scala.collection.mutable._
import chisel3._
import java.io.{File, PrintWriter}

/** Structure for keeping track of which ports on which interfaces are connected inside modules 
                                 
               Example:     _________
                           | KERNEL0 |                           
                           | MEM     |                      
                          / `````\```                          
                         /        \                  
                        /          \                 
           ___________ /            \ ___________                                         
          |  KERNEL1  |              |  KERNEL5  |                                    
           ``/````\````               ``/`````\``                                         
            /      \                   /       \                                  
           /        \                 /         \                                 
      ____/_____    _\________    ___/______    _\________            
     | KERNEL2  |  | KERNEL3  |  | KERNEL6  |  | KERNEL7  |    
     | xBarW 0  |  | xBarW 1  |  | xBarR 2  |  | xBarR 3  |                                          
      ``````````    ``````````    ``````````    ``````````                                             
                                                        
              Visit kernel0: Ledger.connections(mem) = HashMap()
                             controllerStack = K0
              Visit kernel1: Ledger.connections(mem) = HashMap()
                             controllerStack = K1,K0
              Visit kernel2: Ledger.connections(mem) = HashMap( K2 -> ExposedPorts(xBarW 0),
                                                                K1 -> ExposedPorts(xBarW 0),
                                                                K0 -> ExposedPorts(xBarW 0) )
                             controllerStack = K2,K1,K0
                             * Bore xBarW0 between K2 <-> K1 on exit K2
              Visit kernel3: Ledger.connections(mem) = HashMap(  K3 -> ExposedPorts(xBarW 1)
                                                                 K2 -> ExposedPorts(xBarW 0),
                                                                 K1 -> ExposedPorts(xBarW 0, xBarW 1),
                                                                 K0 -> ExposedPorts(xBarW 0, xBarW 1) )
                             controllerStack = K3,K1,K0
                             * Bore xBarW1 between K3 <-> K1 on exit K3
                             * Bore xBarW0, xBarW1 between K1 <-> K0 on exit K1
              Visit kernel5: Ledger.connections(mem) = HashMap( K3 -> ExposedPorts(xBarW 1)
                                                                K2 -> ExposedPorts(xBarW 0),
                                                                K1 -> ExposedPorts(xBarW 0, xBarW 1),
                                                                K0 -> ExposedPorts(xBarW 0, xBarW 1) )
                             controllerStack = K5,K0
              Visit kernel6: Ledger.connections(mem) = HashMap( K5 -> ExposedPorts(xBarR 0)
                                                                K6 -> ExposedPorts(xBarR 0)
                                                                K3 -> ExposedPorts(xBarW 1)
                                                                K2 -> ExposedPorts(xBarW 0),
                                                                K1 -> ExposedPorts(xBarW 0, xBarW 1),
                                                                K0 -> ExposedPorts(xBarW 0, xBarW 1, xBarR 0) )
                             controllerStack = K6,K5,K0
                             * Bore xBarR0 between K6 <-> K5 on exit K5
              Visit kernel7: Ledger.connections(mem) = HashMap( K7 -> ExposedPorts(xBarR 1)
                                                                K5 -> ExposedPorts(xBarR 0, xBarR 1)
                                                                K6 -> ExposedPorts(xBarR 0)
                                                                K3 -> ExposedPorts(xBarW 1)
                                                                K2 -> ExposedPorts(xBarW 0),
                                                                K1 -> ExposedPorts(xBarW 0, xBarW 1),
                                                                K0 -> ExposedPorts(xBarW 0, xBarW 1, xBarR 0, xBarR 1)
                             controllerStack = K7,K5,K0
                             * Bore xBarR1 between K7 <-> K5 on exit K7
                             * Bore xBarR0, xBarR1 between K5 <-> K0 on exit K5
                             * Bore xBarW0, xBarW1, xBarR0, xBarR1 between K0 <-> MEM on exit K0

*/
object ControllerStack {
  val stack = scala.collection.mutable.Stack[Ledger.KernelHash]()
}

object Ledger {
  type OpHash = Int
  type KernelHash = Int
  type BoreMap = HashMap[KernelHash, ExposedPorts] // List of bore connections to make upon leaving KernelHash
  var indent: Int = 0

  // Print all debugging signals into a header file
  val debugFileName = "chisel/debugLedger.txt"

  val debugPW = if (globals.enableVerbose) {
    val debugPW = new PrintWriter(new File(debugFileName))
    Some(debugPW)
  } else None

  def write(s: String): Unit = if (globals.enableVerbose) debugPW.get.println(s"${"  "*indent}$s")

  case class RAddr(val port: Int, val lane: Int)
  class ExposedPorts {
    var xBarR = ListBuffer[RAddr]()
    var directR = ListBuffer[RAddr]()
    var xBarW = ListBuffer[Int]()
    var directW = ListBuffer[Int]()
    var broadcastW = ListBuffer[Int]()
    var broadcastR = ListBuffer[RAddr]()
    var reset = ListBuffer[Int]()
    var output = ListBuffer[Int]()
    var accessActivesIn = ListBuffer[Int]()
    var stageCtrl = ListBuffer[Int]()
    var mergeEnq = ListBuffer[Int]()
    var mergeDeq = ListBuffer[Int]()
    var mergeBound = ListBuffer[Int]()
    var mergeInit = ListBuffer[Int]()
    var allocDealloc = ListBuffer[Int]()

    def addXBarR(p: RAddr): ExposedPorts = {xBarR = xBarR :+ p; this}
    def addDirectR(p: RAddr): ExposedPorts = {directR = directR :+ p; this}
    def addXBarW(p: Int): ExposedPorts = {xBarW = xBarW :+ p; this}
    def addDirectW(p: Int): ExposedPorts = {directW = directW :+ p; this}
    def addBroadcastW(p: Int): ExposedPorts = {broadcastW = broadcastW :+ p; this}
    def addBroadcastR(p: RAddr): ExposedPorts = {broadcastR = broadcastR :+ p; this}
    def addReset(p: Int): ExposedPorts = {reset = reset :+ p; this}
    def addOutput(p: Int): ExposedPorts = {output = output :+ p; this}
    def addAccessActivesIn(p: Int): ExposedPorts = {accessActivesIn = accessActivesIn :+ p; this}
    def addStageCtrl(p: Int): ExposedPorts = {stageCtrl = stageCtrl :+ p; this}
    def addMergeEnq(p: Int): ExposedPorts = {mergeEnq = mergeEnq :+ p; this}
    def addMergeDeq(p: Int): ExposedPorts = {mergeDeq = mergeDeq :+ p; this}
    def addMergeBound(p: Int): ExposedPorts = {mergeBound = mergeBound :+ p; this}
    def addMergeInit(p: Int): ExposedPorts = {mergeInit = mergeInit :+ p; this}
    def addAllocDealloc(p: Int): ExposedPorts = {allocDealloc = allocDealloc :+ p; this}
    def log: Unit = {
      if (xBarR.nonEmpty)           write(s"|-- xBarR: $xBarR")
      if (directR.nonEmpty)         write(s"|-- directR: $directR")
      if (xBarW.nonEmpty)           write(s"|-- xBarW: $xBarW")
      if (directW.nonEmpty)         write(s"|-- directW: $directW")
      if (broadcastW.nonEmpty)      write(s"|-- broadcastW: $broadcastW")
      if (broadcastR.nonEmpty)      write(s"|-- broadcastR: $broadcastR")
      if (reset.nonEmpty)           write(s"|-- reset: $reset")
      if (output.nonEmpty)          write(s"|-- output: $output")
      if (accessActivesIn.nonEmpty) write(s"|-- accessActivesIn: $accessActivesIn")
      if (stageCtrl.nonEmpty)       write(s"|-- stageCtrl: $stageCtrl")
    }
    def merge(port: ExposedPorts): ExposedPorts = {
      xBarR = xBarR ++ port.xBarR
      directR = directR ++ port.directR
      xBarW = xBarW ++ port.xBarW
      directW = directW ++ port.directW
      broadcastW = broadcastW ++ port.broadcastW
      broadcastR = broadcastR ++ port.broadcastR
      reset = reset ++ port.reset
      output = output ++ port.output
      accessActivesIn = accessActivesIn ++ port.accessActivesIn
      stageCtrl = stageCtrl ++ port.stageCtrl
      this
    }
  }

  val connections = HashMap[OpHash, BoreMap]()
  val instrIdsBelow = HashMap[KernelHash, List[Int]]()
  val breakpointsBelow = HashMap[KernelHash, List[Int]]()

  def combine(oldMap: BoreMap, newMap: BoreMap): BoreMap = {
    write(s"newHash:")
    newMap.foreach{case (k, ports) => 
      write(s"+ K.$k:")
      ports.log
    }
    write(s"oldHash:")
    oldMap.foreach{case (k, ports) => 
      write(s"+ K.$k:")
      ports.log
    }
    oldMap.foreach{case (k, ports) => 
      newMap += k -> newMap.getOrElse(k, new ExposedPorts).merge(ports)
    }
    newMap
  }

  def lookup(op: OpHash)(implicit stack: List[KernelHash]): ExposedPorts = {
    write(s"lookup($op)(K.${stack.head})")
    if (connections.contains(op) && connections(op).contains(stack.head)) connections(op)(stack.head)
    else new ExposedPorts
  }

  def substitute(oldHash: OpHash, newHash: OpHash): Unit = {
    if (connections.contains(oldHash)) {
      write(s"substitute($oldHash, $newHash)")
      val tmp = connections(oldHash)
      val current = connections.getOrElse(newHash, new BoreMap)
      connections -= oldHash
      val combined = combine(tmp,current)
      connections += (newHash -> combined)
    }
  }

  // TODO: Should there be a cleanup method upon exiting KernelHash?
  def connectXBarR(hash: OpHash, p: Int, lane: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addXBarR(RAddr(p,lane)))}
      write(s"connectXBarR(${hash}, $p)")
    }
  }
  def connectDirectR(hash: OpHash, p: Int, lane: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addDirectR(RAddr(p,lane)))}
      write(s"connectDirectR(${hash}, $p, $lane)")
    }
  }
  def connectXBarW(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addXBarW(p))}
      write(s"connectXBarW(${hash}, $p)")
    }
  }
  def connectDirectW(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addDirectW(p))}
      write(s"connectDirectW(${hash}, $p)")
    }
  }
  def connectBroadcastW(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addBroadcastW(p))}
      write(s"connectBroadcastW(${hash}, $p)")
    }
  }
  def connectBroadcastR(hash: OpHash, p: Int, lane: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addBroadcastR(RAddr(p,lane)))}
      write(s"connectBroadcastR(${hash}, $p, $lane)")
    }
  }
  def connectReset(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addReset(p))}
      write(s"connectReset(${hash}, $p)")
    }
  }
  def connectOutput(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addOutput(p))}
      write(s"connectOutput(${hash}, $p)")
    }
  }
  def connectAccessActivesIn(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addAccessActivesIn(p))}
      write(s"connectAccessActivesIn(${hash}, $p)")
    }
  }
  def connectStageCtrl(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addStageCtrl(p))}
      write(s"connectStageCtrl(${hash}, $p)")
    }
  }
  def connectMergeEnq(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addMergeEnq(p))}
      write(s"connectMergeEnq(${hash}, $p)")
    }    
  }
  def connectMergeDeq(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addMergeDeq(p))}
      write(s"connectMergeDeq(${hash}, $p)")
    }    
  }
  def connectMergeBound(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addMergeBound(p))}
      write(s"connectMergeBound(${hash}, $p)")
    }    
  }
  def connectMergeInit(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addMergeInit(p))}
      write(s"connectMergeInit(${hash}, $p)")
    }    
  }
  def connectAllocDealloc(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    if (globals.enableModular) {
      val bmap = connections.getOrElseUpdate(hash, new BoreMap())
      stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addAllocDealloc(p))}
      write(s"connectAllocDealloc(${hash}, $p)")
    }    
  }

  def tieInstrCtr(values: List[InstrCtr], id: Int, cycs: UInt, iters: UInt, stalls: UInt, idles: UInt)(implicit stack: List[KernelHash]): Unit = {
    values(id).cycs := cycs
    values(id).iters := iters
    values(id).stalls := stalls 
    values(id).idles := idles
    if (globals.enableModular) stack.foreach{k => instrIdsBelow += (k -> (instrIdsBelow.getOrElse(k, List()) :+ id))}
  }

  def connectInstrCtrs(upstream: List[InstrCtr], downstream: Vec[InstrCtr])(implicit stack: List[KernelHash]): Unit = {
    if (stack.isEmpty) upstream.zip(downstream).foreach{case (a,b) => a := b}
    else instrIdsBelow.getOrElse(stack.head,List()).foreach{i => upstream(i) := downstream(i)}
  }

  def tieBreakpoint(values: Vec[Bool], id: Int, b: Bool)(implicit stack: List[KernelHash]): Unit = {
    values(id) := b
    if (globals.enableModular) stack.foreach{k => breakpointsBelow += (k -> (breakpointsBelow.getOrElse(k, List()) :+ id))}
  }

  def connectBreakpoints(upstream: Vec[Bool], downstream: Vec[Bool])(implicit stack: List[KernelHash]): Unit = {
    if (stack.isEmpty) upstream.zip(downstream).foreach{case (a,b) => a := b}
    else breakpointsBelow.getOrElse(stack.head,List()).foreach{i => upstream(i) := downstream(i)}
  }

  def enter(ctrl: KernelHash, name: String): Unit = {
    write(s"Enter K.$ctrl ($name)")
    indent = indent + 1
    ControllerStack.stack.push(ctrl)
  }
  def exit(): Unit = {
    if (globals.enableVerbose) indent = indent-1
    ControllerStack.stack.pop()
  }
  def finish(): Unit = if (globals.enableVerbose) debugPW.get.close()

}
