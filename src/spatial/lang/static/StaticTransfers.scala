package spatial.lang
package static

import argon._
import forge.tags._

import spatial.node._

trait StaticTransfers {

  /**
    * Transfer a scalar value from the host to the accelerator through the ArgIn `reg`.
    */
  @api def setArg[A](reg: ArgIn[A], value: Lift[A]): Void = {
    implicit val bA: Bits[A] = reg.A
    stage(SetArgIn(reg,value.unbox))
  }
  @api def setArg[A](reg: ArgIn[A], value: Bits[A]): Void = {
    implicit val bA: Bits[A] = reg.A
    stage(SetArgIn(reg,value.unbox))
  }

  /**
    * Transfer a scalar value from the accelerator to the host through the ArgOut `reg`.
    */
  @api def getArg[A](reg: ArgOut[A]): A = {
    implicit val bA: Bits[A] = reg.A
    stage(GetArgOut(reg))
  }


  /** Transfers the given @Array of `data` from the host's memory to `dram`'s region of accelerator DRAM. **/
  @api def setMem[A:Bits,C[T]](dram: DRAM[A,C], data: Tensor1[A]): Void = stage(SetMem(dram,data))

  /** Transfers `dram`'s region of accelerator DRAM to the host's memory and returns the result as an Array. **/
  @api def getMem[A:Bits,C[T]](dram: DRAM[A,C]): Tensor1[A] = {
    val array = Tensor1.empty[A](dram.dims.prodTree)
    stage(GetMem(dram, array))
    array
  }

  /** Transfers `dram`'s region of accelerator DRAM to the host's memory and returns the result as an Array. **/
  @api def getArray[A:Bits,C[T]](dram: DRAM[A,C]): Tensor1[A] = getMem(dram)

  /** Transfers the given @Matrix of `data` from the host's memory to `dram`'s region of accelerator DRAM. **/
  @api def setMem[A:Bits,C[T]](dram: DRAM[A,C], data: Tensor2[A]): Void = setMem(dram, data.data)

  /** Transfers `dram`'s region of accelerator DRAM to the host's memory and returns the result as a Matrix. **/
  @api def getMatrix[T:Bits](dram: DRAM2[T]): Tensor2[T] = Tensor2(getMem(dram), dram.dim0, dram.dim1)

  /** Transfers the given Tensor3 of `data` from the host's memory to `dram`'s region of accelerator DRAM. **/
  @api def setMem[A:Bits,C[T]](dram: DRAM[A,C], tensor3: Tensor3[A]): Void = setMem(dram, tensor3.data)

  /** Transfers `dram`'s region of accelerator DRAM to the host's memory and returns the result as a Tensor3. **/
  @api def getTensor3[A:Bits](dram: DRAM3[A]): Tensor3[A] = Tensor3(getMem(dram), dram.dim0, dram.dim1, dram.dim2)

  /** Transfers the given Tensor4 of `data` from the host's memory to `dram`'s region of accelerator DRAM. **/
  @api def setMem[A:Bits,C[T]](dram: DRAM[A,C], tensor4: Tensor4[A]): Void = setMem(dram, tensor4.data)

  /** Transfers `dram`'s region of accelerator DRAM to the host's memory and returns the result as a Tensor4. **/
  @api def getTensor4[A:Bits](dram: DRAM4[A]): Tensor4[A] = {
    Tensor4(getMem(dram), dram.dim0, dram.dim1, dram.dim2, dram.dim3)
  }

  /** Transfers the given Tensor5 of `data` from the host's memory to `dram`'s region of accelerator DRAM. **/
  @api def setMem[A:Bits,C[T]](dram: DRAM[A,C], tensor5: Tensor5[A]): Void = setMem(dram, tensor5.data)

  /** Transfers `dram`'s region of accelerator DRAM to the host's memory and returns the result as a Tensor5. **/
  @api def getTensor5[A:Bits](dram: DRAM5[A]): Tensor5[A] = {
    Tensor5(getMem(dram), dram.dim0, dram.dim1, dram.dim2, dram.dim3, dram.dim4)
  }

}
