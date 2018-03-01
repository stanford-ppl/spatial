class <<[A,B] {
  def test[A](x: A): B = x.asInstanceOf[B]
}

def check[A,B](x: A)(implicit ev: A << B): B = implicitly[A << B].test(x)
