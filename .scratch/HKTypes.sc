
def test[C[_]](x: C[_]): C[_] = x
def test[C](x: C) = println(x)

test(List(1))