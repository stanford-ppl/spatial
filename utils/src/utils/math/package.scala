package utils

package object math {
  def log2(x: Double): Double = Math.log10(x)/Math.log10(2)
  def isPow2(x: Int): Boolean = (x & (x-1)) == 0
}
