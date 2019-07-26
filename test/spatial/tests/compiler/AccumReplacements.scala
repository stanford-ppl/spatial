package spatial.tests.compiler

object ARHelper{
  def contains(a: Option[String], b: String): Boolean = {a.getOrElse("").indexOf(b) != -1}
}

import spatial.dsl._
import spatial.node._
import argon.Block
import argon.Op

@spatial class AccumReplacements extends SpatialTest {
  override def runtimeArgs: Args = "2"

  def main(args: Array[String]): Unit = {
    val X = ArgIn[Int]
    setArg(X, args(0).to[Int])

    val Y1 = ArgOut[Int]
    val Y2 = ArgOut[Int]
    val Y3 = ArgOut[Int]
    val Y4 = ArgOut[Int]
    val Y5 = ArgOut[Int]
    val Y6 = ArgOut[Int]
    val Y7 = ArgOut[Int]
    val Y8 = ArgOut[Int]
    val Y9 = ArgOut[Int]
    val Y10 = ArgOut[Int]
    val Y11 = ArgOut[Int]

    Accel {
      /**
           0   t   __         
            \   \ /  |      
             \   +   |      
              |  |   | 
    i==0 --> \````/  |
              ``|`   |
               _|    |       
              |__|---'
                          
      */
      val acc1 = Reg[Int](0)  // Not a valid accumulation because r = 0 for i == 0, then r = r + t else
      'ACC1.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc1 := mux(i == 0, 0, acc1.value + t)
      }
      Y1 := acc1

      /**
               t   __         
               /\ /  |      
              |  +   |      
              |  |   | 
    i==0 --> \````/  |
              ``|`   |
               _|    |       
              |__|---'
                          
      */
      val acc2 = Reg[Int](0) // Valid accumulation
      'ACC2.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc2 := mux(i == 0, t, t + acc2.value)
      }
      Y2 := acc2

      /**
                .----.
             t  | t  |        
              | \ /  |      
              |  +   |      
              |  |   | 
    i==0 --> \````/  |
              ``|`   |
               _|    |       
              |__|---'
                          
      */
      val acc3 = Reg[Int](0) // Valid accumulation
      'ACC3.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc3 := mux(i == 0, t, acc3.value + t)
      }
      Y3 := acc3

      /**
             .-------.
             | t     |       
             \ /\    |      
              +  |   |      
              |  |   | 
    i!=0 --> \````/  |
              ``|`   |
               _|    |       
              |__|---'
                          
      */
      val acc4 = Reg[Int](0) // Valid accumulation
      'ACC4.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc4 := mux(i != 0, acc4.value + t, t)
      }
      Y4 := acc4

      /**
               .-----.
            t  |  t  |       
             \ /  /  |      
              +  |   |      
              |  |   | 
    i!=0 --> \````/  |
              ``|`   |
               _|    |       
              |__|---'
                          
      */
      val acc5 = Reg[Int](0) // Valid accumulation
      'ACC5.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc5 := mux(i != 0, t + acc5.value, t)
      }
      Y5 := acc5

      /**
               .-----.
            t  |  t  |       
             \ /  /  |      
              +  |   |      
              |  |   | 
    i==0 --> \````/  |
              ``|`   |
               _|    |       
              |__|---'
                          
      */
      val acc6 = Reg[Int](0) // Not a valid accumulation because r = r + t for i == 0, then r = t else
      'ACC6.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc6 := mux(i == 0, t + acc6.value, t)
      }
      Y6 := acc6

      /**
                0  .--.
                |  |  |       
      i==0 --> \````/ |      
                ``|`  |      
              t   |   | 
               \ /    | 
                +     |
               _|     |       
              |__|----'
                          
      */
      val acc7 = Reg[Int](0) // Valid accumulation
      'ACC7.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc7 := t + mux(i == 0, 0, acc7.value)
      }
      Y7 := acc7

      /**
                .-----.
                |  0  |
                |  |  |       
      i!=0 --> \````/ |      
                ``|`  |      
              t   |   | 
               \ /    | 
                +     |
               _|     |       
              |__|----'
                          
      */
      val acc8 = Reg[Int](0) // Valid accumulation
      'ACC8.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc8 := t + mux(i != 0, acc8.value, 0)
      }
      Y8 := acc8

      /**
            .---------.
            |  0      |
            |  |      |       
  i!=0 --> \````/     |      
            ``|`      |      
              |   t   | 
               \ /    | 
                +     |
               _|     |       
              |__|----'
                          
      */
      val acc9 = Reg[Int](0) // Valid accumulation
      'ACC9.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc9 := mux(i != 0, acc9.value, 0) + t
      }
      Y9 := acc9

      /**
               .------.
            0  |      |
            |  |      |       
  i==0 --> \````/     |      
            ``|`      |      
              |   t   | 
               \ /    | 
                +     |
               _|     |       
              |__|----'
                          
      */
      val acc10 = Reg[Int](0) // Valid accumulation
      'ACC10.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc10 := mux(i != 0, acc10.value, 0) + t
      }
      Y10 := acc10

      /**
               .------.
            0  |      |
            |  |      |       
  ???? --> \````/     |      
            ``|`      |      
              |   t   | 
               \ /    | 
                +     |
               _|     |       
              |__|----'
                          
      */
      val acc11 = Reg[Int](0) // Not a valid accumulation because mux sel is based on something weird
      'ACC11.Foreach(4 by 1) { i =>
        val t = List.tabulate(8) { j => X.value * X.value }.sumTree
        acc11 := mux(X.value == 0, acc11.value, 0) + t
      }
      Y11 := acc11
    }

    val gold1 =  (args(0).to[Int] * args(0).to[Int]) * (4-1) * 8
    val gold2 =  (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    val gold3 =  (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    val gold4 =  (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    val gold5 =  (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    val gold6 =  (args(0).to[Int] * args(0).to[Int]) * 8
    val gold7 =  (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    val gold8 =  (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    val gold9 =  (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    val gold10 = (args(0).to[Int] * args(0).to[Int]) * 4 * 8
    val gold11 = (args(0).to[Int] * args(0).to[Int]) * 8

    println(r"acc1: $Y1 =?= $gold1")
    println(r"acc2: $Y2 =?= $gold2")
    println(r"acc3: $Y3 =?= $gold3")
    println(r"acc4: $Y4 =?= $gold4")
    println(r"acc5: $Y5 =?= $gold5")
    println(r"acc6: $Y6 =?= $gold6")
    println(r"acc7: $Y7 =?= $gold7")
    println(r"acc8: $Y8 =?= $gold8")
    println(r"acc9: $Y9 =?= $gold9")
    println(r"acc10: $Y10 =?= $gold10")
    println(r"acc11: $Y11 =?= $gold11")

    assert(Y1 == gold1)
    assert(Y2 == gold2)
    assert(Y3 == gold3)
    assert(Y4 == gold4)
    assert(Y5 == gold5)
    assert(Y6 == gold6)
    assert(Y7 == gold7)
    assert(Y8 == gold8)
    assert(Y9 == gold9)
    assert(Y10 == gold10)
    assert(Y11 == gold11)
  }

  override def checkIR(block: Block[_]): Result = {
    val regular_writes = block.nestedStms.collect{case x@Op(_:RegWrite[_]) => x }.size 
    val special_writes = block.nestedStms.collect{case x@Op(_:RegAccumOp[_]) => x }.size 

    require(regular_writes == 3, "Should have 3 regular RegWrites")
    require(special_writes == 8, "Should have 8 specialized RegAccumOp")

    // val acc1 = block.nestedStms.collect{case x@Op(RegWrite(reg,_,_)) if ARHelper.contains(reg.name, "acc1") => reg }.size
    // val acc2 = block.nestedStms.collect{case x@Op(RegAccumOp(reg,_,_,_,_)) if ARHelper.contains(reg.name, "acc2") => reg }.size
    // val acc3 = block.nestedStms.collect{case x@Op(RegAccumOp(reg,_,_,_,_)) if ARHelper.contains(reg.name, "acc3") => reg }.size
    // val acc4 = block.nestedStms.collect{case x@Op(RegAccumOp(reg,_,_,_,_)) if ARHelper.contains(reg.name, "acc4") => reg }.size
    // val acc5 = block.nestedStms.collect{case x@Op(RegAccumOp(reg,_,_,_,_)) if ARHelper.contains(reg.name, "acc5") => reg }.size
    // val acc6 = block.nestedStms.collect{case x@Op(RegWrite(reg,_,_)) if ARHelper.contains(reg.name, "acc6") => reg }.size
    // val acc7 = block.nestedStms.collect{case x@Op(RegAccumOp(reg,_,_,_,_)) if ARHelper.contains(reg.name, "acc7") => reg }.size
    // val acc8 = block.nestedStms.collect{case x@Op(RegAccumOp(reg,_,_,_,_)) if ARHelper.contains(reg.name, "acc8") => reg }.size
    // val acc9 = block.nestedStms.collect{case x@Op(RegAccumOp(reg,_,_,_,_)) if ARHelper.contains(reg.name, "acc9") => reg }.size
    // val acc10 = block.nestedStms.collect{case x@Op(RegAccumOp(reg,_,_,_,_)) if ARHelper.contains(reg.name, "acc10") => reg }.size
    // val acc11 = block.nestedStms.collect{case x@Op(RegWrite(reg,_,_)) if ARHelper.contains(reg.name, "acc11") => reg }.size

    // require(acc1 == 1, "Should have 1 RegWrite for acc1")
    // require(acc2 == 1, "Should have 1 RegAccumOp for acc2")
    // require(acc3 == 1, "Should have 1 RegAccumOp for acc3")
    // require(acc4 == 1, "Should have 1 RegAccumOp for acc4")
    // require(acc5 == 1, "Should have 1 RegAccumOp for acc5")
    // require(acc6 == 1, "Should have 1 RegWrite for acc6")
    // require(acc7 == 1, "Should have 1 RegAccumOp for acc7")
    // require(acc8 == 1, "Should have 1 RegAccumOp for acc8")
    // require(acc9 == 1, "Should have 1 RegAccumOp for acc9")
    // require(acc10 == 1, "Should have 1 RegAccumOp for acc10")
    // require(acc11 == 1, "Should have 1 RegWrite for acc11")

    super.checkIR(block)
  }
}
