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