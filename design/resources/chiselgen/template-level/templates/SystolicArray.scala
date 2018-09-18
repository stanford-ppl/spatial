package templates

import ops._
import chisel3._
import chisel3.util
import chisel3.util.Mux1H
import scala.math._

sealed trait OperationMode
object Sum extends OperationMode
object Product extends OperationMode
object MAC extends OperationMode // multiply-accumulate
object Max extends OperationMode
object Min extends OperationMode

/*

           SystolicArray(List(7,6),      List(2,2),       List(0,1,1,0),   List(0,0,0,1),   Sum             ) 
                         6x6 array   2x2 neighborhood,       0 1 movement      0 0          addition lambda
                                                             1 0               0 * <- dst
                            neg
                   <-       edge 0      ->
           vert0   _↓_______________↓_____  vert1                                            
                  → + |   |   |   → + |   |                                                  
                  |___|___|___|___|___|___|                                                  
               e  |   |   |   |   |   |   |                                                  
               d  |___|___|_↓_|___|___|___|                                                  
               g  |   |   → + |   |   |   |                                                  
         n     e  |___|___|___|___|___|___| p                                                
         e        |   |   |   |   |   |   | o
         g     1  |___|___|_↓_|___|___|_↓_| s
                  |   |   → + |   |   → + |
                  |___|___|___|___|___|___|
                  |   |   |   |   |   |   |
                  |___|___|___|___|___|___|
                  |   |   |   |   |   |   |                                                                
                  |___|___|___|___|___|___|                                                                
               vert2        pos             vert3                       
*/
class SystolicArray2D(val dims: List[Int], val neighborhood_size: List[Int], val movement_scalars: List[Double], val self_position: List[Int],
                      val inits: Option[List[Double]] ,
                      val operation: OperationMode, val bitWidth: Int = 32, val fracBits: Int = 0) extends Module {
  def this(tuple: (List[Int], List[Int], List[Double], List[Int], Option[List[Double]], OperationMode, Int, Int)) = this(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5, tuple._6, tuple._7, tuple._8)

  /* Interface Analysis */

  // Identify address of self
  assert(self_position.filter(_!=0).length == 1)
  val self_coords = (0 until neighborhood_size.length).map{dim => 
    val flat_id = self_position.indexWhere(_ != 0) 
    val divide = neighborhood_size.drop(dim + 1).product
    val mod = neighborhood_size(dim)
    (flat_id/divide) % mod
  }

  /*
      Assume perimeter of movement_scalars has at least one non-zero on each plane, and determine thickness of each entry plane, edge, vertex, etc...

      A 2D array has 4 1-D planes and 4 2-D vertices => List( List( (neg_dim0, pos_dim0), (neg_dim1, pos_dim1) ),  //edge thicknesses
                                                      List( (quad0_dim0, quad0_dim1), (quad1_dim0, quad1_dim1), (quad2_dim0, quad2_dim1), (quad3_dim0, quad3_dim1) ) //vertex thicknesses
                                                    )
  */
  val edge_thickness = (0 until 2).map{p =>
    val neg_thickness = self_coords(p)
    val pos_thickness = neighborhood_size(p) - self_coords(p) - 1
    (neg_thickness, pos_thickness)
  }
  val vertex_thickness = (0 until 4).map{p => 
    val dim0_start = if (p / 2 == 0) 0 else self_coords(0) + 1
    val dim0_end = if (p / 2 == 0) self_coords(0) else neighborhood_size(0)
    val dim1_start = if (p % 2 == 0) 0 else self_coords(1) + 1
    val dim1_end = if (p % 2 == 0) self_coords(1) else neighborhood_size(1)
    val has_nonzero = (dim0_start until dim0_end).map{i => (dim1_start until dim1_end).map{j => 
      movement_scalars(i*neighborhood_size(1) + j)
    }}.flatten.filter(_ != 0).length > 0
    if (has_nonzero) ((dim0_end - dim0_start, dim1_end - dim1_start)) else (0,0)
  }

  /*
       Ordering of input vector example for edge_thickness (2,1), (1,1) and vertex thickness (1,2), (1,1), (1,2), (1,1)

      quad0       neg0    quad1
            0 1 | 2 3 4 | 5
            ----|-------|---
    neg1    6 7 | SYS   | 8  pos1
            9 A |  ARR  | B  
            C D |       | E
            ----|-------|---
            F G | H I J | K
     quad2        pos0     quad3

  */
  val num_input_ports = edge_thickness.zip(dims).map{case (th, dim) => dim*(th._1 + th._2)}.sum + vertex_thickness.map{ th => th._1 * th._2 }.sum
  val num_output_ports = dims.product
  val super_square_dims = List(edge_thickness(0)._1 + dims(0) + edge_thickness(0)._2, edge_thickness(1)._1 + dims(1) + edge_thickness(1)._2)

  // Create IO
  val io = IO( new Bundle {
    val in = Vec(num_input_ports, Input(UInt(bitWidth.W)))
    val out = Vec(num_output_ports, Output(UInt(bitWidth.W)))
    val shiftEn = Input(Bool()) 
    val reset = Input(Bool())
  })

  // Create registers
  val registers = if (inits.isDefined){
    List.tabulate(dims(0)){i => List.tabulate(dims(1)){j =>  
      val initval = (inits.get.apply(i*dims(1)+j)*-*scala.math.pow(2,fracBits)).toLong
      RegInit(initval.U(bitWidth.W))
    }}.flatten
  } else {
    List.tabulate(dims(0)){i => List.tabulate(dims(1)){j => 
      operation match {
        case _@(Sum | Min | Max) => RegInit(0.U(bitWidth.W))
        case _@(MAC | Product) => RegInit(1.U(bitWidth.W))
      }
    }}.flatten
  }

  // Wire up registers
  // (edge_thickness(0)._1 until (dims(0) - edge_thickness(0)._2)).foreach{ i =>
  //   (edge_thickness(1)._1 until (dims(1) - edge_thickness(1)._2)).foreach{ j => 
  (0 until dims(0)).foreach{ i =>
    (0 until dims(1)).foreach{ j => 
      val mac_base = if (operation match {case MAC => true; case _ => false}) {
        // val src_row = i - self_coords(0)
        // val src_col = j - self_coords(1)
        val weight = movement_scalars(self_coords(0)*neighborhood_size(1) + self_coords(1))
        val src_addr = i*dims(1) + j
        weight.FP(true, bitWidth-fracBits, fracBits) *-* registers(src_addr).FP(true, bitWidth-fracBits, fracBits)
      } else { 0.FP(true, bitWidth-fracBits, fracBits) }
      val new_val = (0 until neighborhood_size(0)).map { ii => (0 until neighborhood_size(1)).map{ jj => 
        val is_self = self_position(ii*neighborhood_size(1) + jj) == 1
        val weight = movement_scalars(ii*neighborhood_size(1) + jj)
        val src_row = i - self_coords(0) + ii
        val src_col = j - self_coords(1) + jj
        val super_square_row = edge_thickness(0)._1 + src_row
        val super_square_col = edge_thickness(1)._1 + src_col  
        val mac_ignore_self = is_self & {operation match {case MAC => true; case _ => false}}
        if (weight != 0 & !mac_ignore_self) {
            if (src_row < 0 && src_col < 0) { // Quadrant 0
                val quad1_correction = if (vertex_thickness(1)._1 * vertex_thickness(1)._2 == 0) {super_square_row * edge_thickness(1)._2} else 0
                val src_addr = super_square_row * super_square_dims(1) + super_square_col - quad1_correction
                io.in(src_addr).FP(true, bitWidth-fracBits, fracBits)
            } else if (src_row < 0 && src_col >= dims(1)) { // Quadrant 1
                val quad0_correction = if (vertex_thickness(0)._1 * vertex_thickness(0)._2 == 0) {(super_square_row+1) * edge_thickness(1)._1} else 0
                val src_addr = super_square_row * super_square_dims(1) + super_square_col - quad0_correction
                io.in(src_addr).FP(true, bitWidth-fracBits, fracBits)
            } else if (src_col < 0 && src_row >= dims(0)) { // Quadrant 2
                val quad0_correction = if (vertex_thickness(0)._1 * vertex_thickness(0)._2 == 0) {edge_thickness(0)._1 * edge_thickness(1)._1} else 0
                val quad1_correction = if (vertex_thickness(1)._1 * vertex_thickness(1)._2 == 0) {edge_thickness(0)._1 * edge_thickness(1)._2} else 0
                val quad3_correction = if (vertex_thickness(3)._1 * vertex_thickness(3)._2 == 0) {(super_square_row - dims(0) - edge_thickness(0)._1) * edge_thickness(1)._2} else 0
                val src_addr = src_row * super_square_dims(1) + src_col - dims.product - quad0_correction - quad1_correction - quad3_correction
                io.in(src_addr).FP(true, bitWidth-fracBits, fracBits)
            } else if (src_row >= dims(0) && src_col >= dims(1)) { // Quadrant 3
                val quad0_correction = if (vertex_thickness(0)._1 * vertex_thickness(0)._2 == 0) {edge_thickness(0)._1 * edge_thickness(1)._1} else 0
                val quad1_correction = if (vertex_thickness(1)._1 * vertex_thickness(1)._2 == 0) {edge_thickness(0)._1 * edge_thickness(1)._2} else 0
                val quad2_correction = if (vertex_thickness(2)._1 * vertex_thickness(2)._2 == 0) {(super_square_row - dims(0) - edge_thickness(0)._1 + 1) * edge_thickness(1)._1} else 0
                val src_addr = src_row * super_square_dims(1) + src_col - dims.product - quad0_correction - quad1_correction - quad2_correction
                io.in(src_addr).FP(true, bitWidth-fracBits, fracBits)
            } else if (src_row < 0) { // Neg Edge 0
                val quad0_correction = if (vertex_thickness(0)._1 * vertex_thickness(0)._2 == 0) {(super_square_row+1) * edge_thickness(1)._1} else 0
                val quad1_correction = if (vertex_thickness(1)._1 * vertex_thickness(1)._2 == 0) {super_square_row * edge_thickness(1)._2} else 0
                val src_addr = super_square_row * super_square_dims(1) + super_square_col - quad0_correction - quad1_correction
                io.in(src_addr).FP(true, bitWidth-fracBits, fracBits)
            } else if (src_col < 0) { // Neg Edge 1
                val quad0_correction = if (vertex_thickness(0)._1 * vertex_thickness(0)._2 == 0) {edge_thickness(0)._1 * edge_thickness(1)._1} else 0
                val quad1_correction = if (vertex_thickness(1)._1 * vertex_thickness(1)._2 == 0) {edge_thickness(0)._1 * edge_thickness(1)._2} else 0
                val super_addr = super_square_row * super_square_dims(1) + super_square_col - quad0_correction - quad1_correction
                val num_holes = dims(1)*src_row
                val src_addr = super_addr - num_holes
                io.in(src_addr).FP(true, bitWidth-fracBits, fracBits)
            } else if (src_row >= dims(0)) { // Pos Edge 0
                val quad0_correction = if (vertex_thickness(0)._1 * vertex_thickness(0)._2 == 0) {edge_thickness(0)._1 * edge_thickness(1)._1} else 0
                val quad1_correction = if (vertex_thickness(1)._1 * vertex_thickness(1)._2 == 0) {edge_thickness(0)._1 * edge_thickness(1)._2} else 0
                val quad2_correction = if (vertex_thickness(2)._1 * vertex_thickness(2)._2 == 0) {(super_square_row - dims(0) - edge_thickness(0)._1 + 1) * edge_thickness(1)._1} else 0
                val quad3_correction = if (vertex_thickness(3)._1 * vertex_thickness(3)._2 == 0) {(super_square_row - dims(0) - edge_thickness(0)._1) * edge_thickness(1)._2} else 0
                val src_addr = super_square_row * super_square_dims(1) + super_square_col - dims.product - quad0_correction - quad1_correction - quad2_correction - quad3_correction
                io.in(src_addr).FP(true, bitWidth-fracBits, fracBits)
            } else if (src_col >= dims(1)) { // Pos Edge 1
                val quad0_correction = if (vertex_thickness(0)._1 * vertex_thickness(0)._2 == 0) {edge_thickness(0)._1 * edge_thickness(1)._1} else 0
                val quad1_correction = if (vertex_thickness(1)._1 * vertex_thickness(1)._2 == 0) {edge_thickness(0)._1 * edge_thickness(1)._2} else 0
                val super_addr = super_square_row * super_square_dims(1) + super_square_col - quad0_correction - quad1_correction
                val num_holes = dims(1)*(src_row+1)
                val src_addr = super_addr - num_holes
                io.in(src_addr).FP(true, bitWidth-fracBits, fracBits)
            } else { // Internal source
                val src_addr = src_row*dims(1) + src_col
                weight.FP(true, bitWidth-fracBits, fracBits) *-* registers(src_addr).FP(true, bitWidth-fracBits, fracBits)
            }            
        } else {
          operation match {
            case _@(Sum | Min | Max) => 0.FP(true, bitWidth-fracBits, fracBits)
            case _@(MAC | Product) => 1.FP(true, bitWidth-fracBits, fracBits)
          }
        }

      }}.flatten.reduce{ (a,b) => 
        operation match {
          case Sum      => a + b  
          case MAC      => a *-* b  
          case Product  => a *-* b 
          case Min      => Mux(a < b, a, b) 
          case Max      => Mux(a < b, b, a) 
        }
      } + mac_base
      when(io.shiftEn){
        registers(i*dims(1) + j) := new_val.r  
      }.elsewhen(io.reset){
        if (inits.isDefined){
          registers(i*dims(1) + j) := (inits.get.apply(i*dims(1)+j)*-*scala.math.pow(2,fracBits)).toLong.U(bitWidth.W)
        } else {
          operation match {
            case _@(Sum | Min | Max) => registers(i*dims(1)+j) := 0.U(bitWidth.W)
            case _@(MAC | Product) => registers(i*dims(1)+j) := 1.U(bitWidth.W)
          }
        }
      }.otherwise{
        registers(i*dims(1) + j) := registers(i*dims(1) + j)
      }
    }
  }

  (0 until dims(0)).foreach{i =>
    (0 until dims(1)).foreach{j => 
      io.out(i*dims(1) + j) := registers(i*dims(1) + j)
    }
  }


}
