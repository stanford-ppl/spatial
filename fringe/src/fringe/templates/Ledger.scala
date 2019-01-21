package fringe

import scala.collection.mutable._

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

object Ledger {
  type OpHash = Int
  type KernelHash = Int
  type BoreMap = HashMap[KernelHash, ExposedPorts] // List of bore connections to make upon leaving KernelHash

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

    def addXBarR(p: RAddr): ExposedPorts = {xBarR = xBarR :+ p; this}
    def addDirectR(p: RAddr): ExposedPorts = {directR = directR :+ p; this}
    def addXBarW(p: Int): ExposedPorts = {xBarW = xBarW :+ p; this}
    def addDirectW(p: Int): ExposedPorts = {directW = directW :+ p; this}
    def addBroadcastW(p: Int): ExposedPorts = {broadcastW = broadcastW :+ p; this}
    def addBroadcastR(p: RAddr): ExposedPorts = {broadcastR = broadcastR :+ p; this}
    def addReset(p: Int): ExposedPorts = {reset = reset :+ p; this}
    def addOutput(p: Int): ExposedPorts = {output = output :+ p; this}
    def addAccessActivesIn(p: Int): ExposedPorts = {accessActivesIn = accessActivesIn :+ p; this}
  }

  val connections = HashMap[OpHash, BoreMap]()

  // TODO: Should there be a cleanup method upon exiting KernelHash?
  def connectXBarR(hash: OpHash, p: Int, lane: Int)(implicit stack: List[KernelHash]): Unit = {
    val bmap = connections.getOrElseUpdate(hash, new BoreMap())
    stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addXBarR(RAddr(p,lane)))}
  }
  def connectDirectR(hash: OpHash, p: Int, lane: Int)(implicit stack: List[KernelHash]): Unit = {
    val bmap = connections.getOrElseUpdate(hash, new BoreMap())
    stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addDirectR(RAddr(p,lane)))}
  }
  def connectXBarW(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    val bmap = connections.getOrElseUpdate(hash, new BoreMap())
    stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addXBarW(p))}
  }
  def connectDirectW(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    val bmap = connections.getOrElseUpdate(hash, new BoreMap())
    stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addDirectW(p))}
  }
  def connectBroadcastW(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    val bmap = connections.getOrElseUpdate(hash, new BoreMap())
    stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addBroadcastW(p))}
  }
  def connectBroadcastR(hash: OpHash, p: Int, lane: Int)(implicit stack: List[KernelHash]): Unit = {
    val bmap = connections.getOrElseUpdate(hash, new BoreMap())
    stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addBroadcastR(RAddr(p,lane)))}
  }
  def connectReset(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    val bmap = connections.getOrElseUpdate(hash, new BoreMap())
    stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addReset(p))}
  }
  def connectOutput(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    val bmap = connections.getOrElseUpdate(hash, new BoreMap())
    stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addOutput(p))}
  }
  def connectAccessActivesIn(hash: OpHash, p: Int)(implicit stack: List[KernelHash]): Unit = {
    val bmap = connections.getOrElseUpdate(hash, new BoreMap())
    stack.foreach{case k => connections(hash) += (k -> bmap.getOrElse(k, new ExposedPorts).addAccessActivesIn(p))}
  }

}
