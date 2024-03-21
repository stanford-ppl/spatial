package spatial.tests.ee109

import spatial.dsl._

@spatial class Lab1Part1RegExample extends SpatialTest {
    // In this app, the type of numbers is Int.
    type T = Int

    // Sets the runtime arguments for args(0) and args(1). These can be overridden later via command line, but are used for simulation.
    override def runtimeArgs = "3 5"

    def main(args: Array[String]): Unit = {
        // Part 1: Read in arguments
        // In Spatial, you can get the Nth argument from the command line by using args(N). 
        // We need to cast it as type T because we use T as the type of the values throughout the whole app. 
        val N = args(0).to[T]
        val M = args(1).to[T]

        // Part 2: Create & Set registers on the CPU side
        // Input Registers
        // Create two ArgIn registers
        val argRegIn0 = ArgIn[T]
        val argRegIn1 = ArgIn[T]

        // Set two ArgIn registers with N and M
        setArg(argRegIn0, N)
        setArg(argRegIn1, M)
        
        // Output Register
        // Create one ArgOut register
        val argRegOut = ArgOut[T]
        

        // Part 3: Accelerator design
        // fetch two values from the CPU side and passes their sum back to the CPU
        Accel {
            // Get values of the two argIn registers. We get the value of a register by using .value. 
            val argRegIn0Value = argRegIn0.value
            val argRegIn1Value = argRegIn1.value

            // Perform the addition, then set the output register with the result. The := sign is used to assign a value to a register.
            argRegOut := argRegIn0Value + argRegIn1Value
        }

        // Get the result from the accelerator.
        val argRegOutResult = getArg(argRegOut)

        // Print the result.
        println("Result = " + argRegOutResult)

        // Calculate the reference result. Make sure that it matches the accelerator output.
        val gold = M + N
        println("Gold = " + gold)
        val cksum = gold == argRegOutResult

        // Print PASS if the reference result matches the accelerator result.
        println("PASS = " + cksum)

        // To make the compiler happy
        assert(cksum == 1)
    }
}

@spatial class Lab1Part1RegExampleSubmit extends SpatialTest {
    // In this app, the type of numbers is Int.
    type T = Int

    // Sets the runtime arguments for args(0) and args(1). These can be overridden later via command line, but are used for simulation.
    override def runtimeArgs = "3 5 7"

    def main(args: Array[String]): Unit = {
        // Part 1: Read in arguments
        // In Spatial, you can get the Nth argument from the command line by using args(N). 
        // We need to cast it as type T because we use T as the type of the values throughout the whole app. 
        val N = args(0).to[T]
        val M = args(1).to[T]
        val O = args(2).to[T]

        // Part 2: Create & Set registers on the CPU side
        // Input Registers
        // Create two ArgIn registers
        val argRegIn0 = ArgIn[T]
        val argRegIn1 = ArgIn[T]
        val argRegIn2 = ArgIn[T]

        // Set two ArgIn registers with N and M
        setArg(argRegIn0, N)
        setArg(argRegIn1, M)
        setArg(argRegIn2, O)
        
        // Output Register
        // Create one ArgOut register
        val argRegOut = ArgOut[T]
        

        // Part 3: Accelerator design
        // fetch two values from the CPU side and passes their sum back to the CPU
        Accel {
            // Get values of the two argIn registers. We get the value of a register by using .value. 
            val argRegIn0Value = argRegIn0.value
            val argRegIn1Value = argRegIn1.value
            val argRegIn2Value = argRegIn2.value

            // Perform the addition, then set the output register with the result. The := sign is used to assign a value to a register.
            argRegOut := argRegIn0Value + argRegIn1Value + argRegIn2Value
        }

        // Get the result from the accelerator.
        val argRegOutResult = getArg(argRegOut)

        // Print the result.
        println("Result = " + argRegOutResult)

        // Calculate the reference result. Make sure that it matches the accelerator output.
        val gold = M + N + O
        println("Gold = " + gold)
        val cksum = gold == argRegOutResult

        // Print PASS if the reference result matches the accelerator result.
        println("PASS = " + cksum)

        // To make the compiler happy
        assert(cksum == 1)
    }
}

@spatial class Lab1Part2DramSramExample extends SpatialTest {

    val N = 32
    type T = Int

    // Sets the runtime arguments for args(0) and args(1). These can be overridden later via command line, but are used for simulation.
    override def runtimeArgs = "3"

    // In this example, we write the accelerator code in a function.
    // [T:Type:Num] means that this function takes in a type T.
    // The operator "=" means that this function is returning a value.
    def simpleLoadStore(srcHost: Array[T], value: T) = {
        val tileSize = 16

        val srcFPGA = DRAM[T](N)
        val dstFPGA = DRAM[T](N)

        // 1. Bring the N elements from the host side into DRAM
        setMem(srcFPGA, srcHost)

        val x = ArgIn[T]
        setArg(x, value)
        Accel {

            Sequential.Foreach(N by tileSize) { i =>
            val b1 = SRAM[T](tileSize)

            b1 load srcFPGA(i::i+tileSize)

            // 2. Bring the elements into the accelerator
            val b2 = SRAM[T](tileSize)
            Foreach(tileSize by 1) { ii =>
                // 3. Multiply each element by a factor of x
                b2(ii) = b1(ii) * x
            }

            // 4. Store the result back to DRAM
            dstFPGA(i::i+tileSize) store b2
            }
        }

        // 5. Intruct the host to fetch data from DRAM
        getMem(dstFPGA)
    }

  def main(args: Array[String]): Unit = {
    val arraySize = N
    val value = args(0).to[Int]

    // This line means that we are creating an array of size "arraySize", where each 
    // element is an integer. "i => i % 256" means that for each index i, populate an 
    // element with value i % 256. 
    // tabulate is a function that returns an immutable Array with the given size and elements defined by func.
    // more array methods can be found in https://spatial-lang.readthedocs.io/en/legacy/api/sw/array.html
    val src = Array.tabulate[Int](arraySize) { i => i % 256 }
    val dst = simpleLoadStore(src, value)

    // This line means that for each element in src, generate an element using 
    // the function "_ * value". Map is an operator that maps a function to 
    // every single element of an array.
    val gold = src.map { _ * value }

    println("Sent in: ")
    (0 until arraySize) foreach { i => print(gold(i) + " ") }
    println("Got out: ")
    (0 until arraySize) foreach { i => print(dst(i) + " ") }
    println("")

    // This line means that for every pair of elements in dst, gold, check if each 
    // pair contains equal elements. Reduce coalesces all the pairs by using the 
    // function "_&&_".
    val cksum = dst.zip(gold){_ == _}.reduce{_&&_}
    println("PASS: " + cksum)

    assert(cksum == 1)
  }
}

@spatial class Lab1Part2DramSramExampleSubmit extends SpatialTest {

    val N = 32
    type T = Int

    // Sets the runtime arguments for args(0) and args(1). These can be overridden later via command line, but are used for simulation.
    override def runtimeArgs = "3"

    // In this example, we write the accelerator code in a function.
    // [T:Type:Num] means that this function takes in a type T.
    // The operator "=" means that this function is returning a value.
    def simpleLoadStore(srcHost: Array[T], value: T) = {
        val tileSize = 16

        val srcFPGA = DRAM[T](N)
        val dstFPGA = DRAM[T](N)

        // 1. Bring the N elements from the host side into DRAM
        setMem(srcFPGA, srcHost)

        val x = ArgIn[T]
        setArg(x, value)
        Accel {

            Sequential.Foreach(N by tileSize) { i =>
            val b1 = FIFO[T](tileSize)

            b1 load srcFPGA(i::i+tileSize)

            // 2. Bring the elements into the accelerator
            val b2 = FIFO[T](tileSize)
            Foreach(tileSize by 1) { ii =>
                // 3. Multiply each element by a factor of x
                b2.enq(b1.deq() * x)
            }

            // 4. Store the result back to DRAM
            dstFPGA(i::i+tileSize) store b2
            }
        }

        // 5. Intruct the host to fetch data from DRAM
        getMem(dstFPGA)
    }

  def main(args: Array[String]): Unit = {
    val arraySize = N
    val value = args(0).to[Int]

    // This line means that we are creating an array of size "arraySize", where each 
    // element is an integer. "i => i % 256" means that for each index i, populate an 
    // element with value i % 256. 
    // tabulate is a function that returns an immutable Array with the given size and elements defined by func.
    // more array methods can be found in https://spatial-lang.readthedocs.io/en/legacy/api/sw/array.html
    val src = Array.tabulate[Int](arraySize) { i => i % 256 }
    val dst = simpleLoadStore(src, value)

    // This line means that for each element in src, generate an element using 
    // the function "_ * value". Map is an operator that maps a function to 
    // every single element of an array.
    val gold = src.map { _ * value }

    println("Sent in: ")
    (0 until arraySize) foreach { i => print(gold(i) + " ") }
    println("Got out: ")
    (0 until arraySize) foreach { i => print(dst(i) + " ") }
    println("")

    // This line means that for every pair of elements in dst, gold, check if each 
    // pair contains equal elements. Reduce coalesces all the pairs by using the 
    // function "_&&_".
    val cksum = dst.zip(gold){_ == _}.reduce{_&&_}
    println("PASS: " + cksum)

    assert(cksum == 1)
  }
}


@spatial class Lab1Part6ReduceExample extends SpatialTest {
    val N = 32
    val tileSize = 16
    type T = Int

    def main(args: Array[String]): Unit = {
        val arraySize = N
        val srcFPGA = DRAM[T](N)
        val src = Array.tabulate[Int](arraySize) { i => i % 256 }
        setMem(srcFPGA, src)
        val destArg = ArgOut[T]

        Accel {
            // First Reduce Controller
            val accum = Reg[T](0)
            Sequential.Reduce(accum)(N by tileSize) { i =>
                val b1 = SRAM[T](tileSize)
                b1 load srcFPGA(i::i+tileSize)
                // Second Reduce Controller. In Scala / Spatial, the last element
                // of a function will be automatically returned (if your function
                // should return anything). Therefore you don't need to write a
                // return at this line explicitly.
                Reduce(0)(tileSize by 1) { ii => b1(ii) }{_+_}
            }{_+_}


            destArg := accum.value
        }

        val result = getArg(destArg)
        val gold = src.reduce{_+_}
        println("Gold: " + gold)
        println("Result: : " + result)
        println("")

        val cksum = gold == result
        println("PASS: " + cksum)

        assert(cksum == 1)
    }
}


@spatial class Lab1Part6FoldExample extends SpatialTest {
    val N = 32
    val tileSize = 16
    type T = Int

    def main(args: Array[String]): Unit = {
        val arraySize = N
        val srcFPGA = DRAM[T](N)
        val src = Array.tabulate[Int](arraySize) { i => i % 256 }
        setMem(srcFPGA, src)
        val destArg = ArgOut[T]

        Accel {
            // First Reduce Controller
            val accum = Reg[T](0)
            Sequential.Fold(accum)(N by tileSize) { i =>
                val b1 = SRAM[T](tileSize)
                b1 load srcFPGA(i::i+tileSize)
                // Second Reduce Controller. In Scala / Spatial, the last element
                // of a function will be automatically returned (if your function
                // should return anything). Therefore you don't need to write a
                // return at this line explicitly.
                Fold(0)(tileSize by 1) { ii => b1(ii) }{_+_}
            }{_+_}


            destArg := accum.value
        }

        val result = getArg(destArg)
        val gold = src.reduce{_+_}
        println("Gold: " + gold)
        println("Result: : " + result)
        println("")

        val cksum = gold == result
        println("PASS: " + cksum)

        assert(cksum == 1)
    }
}