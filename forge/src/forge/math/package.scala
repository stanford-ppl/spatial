package forge

package object math {
  def isPow2(x: Int): Boolean = (x & (x-1)) == 0
}
