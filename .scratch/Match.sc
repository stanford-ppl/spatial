val errs = (0::16,0::length){(i,j) =>
  if (i == 0 ) {dram1(j).to[T] + dram2(j).to[T] }
  else if (i == 1 ) { dram1(j).to[T] * dram2(j).to[T] }
  else if (i == 2 ) { dram1(j).to[T] / dram2(j).to[T] }
  else if (i == 3 ) { sqrt(dram1(j).to[T]) }
  else if (i == 4 ) { dram1(j).to[T] - dram2(j).to[T] }
  else if (i == 5 ) { dram1(j).to[T] < dram2(j).to[T] }
  else if (i == 6 ) { dram1(j).to[T] > dram2(j).to[T] }
  else if (i == 7 ) { dram1(j).to[T] == dram2(j).to[T] }
  else if (i == 8 ) { abs(dram1(j).to[T]) }
  else if (i == 9 ) { exp(dram1(j).to[T]) }
  else if (i == 10) { log(dram1(j).to[T]) }
  else if (i == 11) { 1.to[T]/(dram1(j).to[T]) }
  else if (i == 12) { 1.to[T]/sqrt(dram1(j)) }
  else if (i == 13) { sigmoid(dram1(j)) }
  else if (i == 14) { tanh(dram1(j)) }
  else if (i == 15) { dram1(j) * dram2(j) + dram3(j) }
  else 0.to[T]
}
val b = gold(i,j)
println(i + " Expected: " + a + ", Actual: " + b)
a > (b - margin) && a < (b + margin)
}
val isCorrect = errs.reduce{(a,b) => a && b}
println("PASS: " + isCorrect + " (SambaFloatBasics)")`