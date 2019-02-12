import scala.language.higherKinds

package object models {
  type NodeModel = Either[LinearModel,Double]

  type Area    = Model[Double,AreaFields]
  type Latency = Model[Double,LatencyFields]
  type NodeArea  = Model[NodeModel,AreaFields]
  type NodeLatency = Model[NodeModel,LatencyFields]

  object Area {
    def empty[T](implicit c: AreaFields[T]) = new Model[T,AreaFields]("",Nil,Map.empty)
    def apply[T](entries: (String,T)*)(implicit c: AreaFields[T]): Model[T,AreaFields] = {
      new Model[T,AreaFields]("",Nil,entries.toMap)
    }
    def fromArray[T](name: String, params: Seq[String], entries: Array[T])(implicit config: AreaFields[T]): Model[T,AreaFields] = {
      new Model[T,AreaFields](name, params, config.fields.zip(entries).toMap)
    }
  }
  object Latency {
    def empty[T](implicit c: LatencyFields[T]) = new Model[T,LatencyFields]("",Nil,Map.empty)
    def apply[T](entries: (String,T)*)(implicit c: LatencyFields[T]): Model[T,LatencyFields] = {
      new Model[T,LatencyFields]("",Nil,entries.toMap)
    }
    def fromArray[T](name: String, params: Seq[String], entries: Array[T])(implicit config: LatencyFields[T]): Model[T,LatencyFields] = {
      new Model[T,LatencyFields](name, params, config.fields.zip(entries).toMap)
    }
  }

  implicit class NodeModelOps(x: NodeModel) {
    def eval(args: (String,Double)*): Double = x match {
      case Left(model) => Math.max(0.0, model.eval(args:_*))
      case Right(num) => Math.max(0.0, num)
    }
  }

  implicit class SeqAreaOps[T,C[A]<:Fields[A,C]](areas: Seq[Model[T,C]]) {
    def getFirst(name: String, params: (Int,String)*): Model[T,C] = {
      areas.find{a => a.name == name && params.forall{case (i,p) => a.params(i) == p} }.get
    }
    def getAll(name: String, params: (Int,String)*): Seq[Model[T,C]] = {
      areas.filter{a: Model[T,C] => a.name == name && params.forall{case (i,p) => a.params(i) == p} }
    }
  }

  implicit class DoubleModelOps[C[A]<:Fields[A,C]](a: Model[Double,C]) {
    implicit val lin: C[LinearModel] = a.config.convert(d = LinearModel(Nil,Set.empty))
    implicit val dbl: C[Double] = a.config
    def cleanup: Model[Double,C] = a.map{x => Math.max(0.0, Math.round(x)) }
    def fractional: Model[Double,C] = a.map{x => Math.max(0.0, x) }

    /*def +(b: Model[LinearModel,C]): Model[LinearModel,C] = {
      a.zip(b){(y,x) => x + LinearModel(Seq(Prod(y,Nil)),Set.empty) }
    }*/
  }

  implicit class LinearModelAreaOps[C[A]<:Fields[A,C]](a: Model[LinearModel,C]) {
    implicit val lin: C[LinearModel] = a.config
    implicit val dbl: C[Double] = a.config.convert(d = 0.0)

    def *(b: String): Model[LinearModel,C] = a.map(_*b)
    def *(b: Double): Model[LinearModel,C] = a.map(_*b)
    def /(b: Double): Model[LinearModel,C] = a.map(_/b)
    def +(b: Double): Model[LinearModel,C] = a.map(_+b)
    def -(b: Double): Model[LinearModel,C] = a.map(_-b)

    def ++(b: Model[LinearModel,C]): Model[LinearModel,C] = a.zip(b){(x,y) => x + y }
    def --(b: Model[LinearModel,C]): Model[LinearModel,C] = a.zip(b){(x,y) => x - y }
    def <->(b: Model[LinearModel,C]): Model[LinearModel,C] = a.zip(b){(x,y) => x <-> y }


    def apply(xs: (String,Double)*): Model[Double,C] = a.map(_.eval(xs:_*))
    def eval(xs: (String,Double)*): Model[Double,C] = a.map(_.eval(xs:_*))
    def partial(xs: (String,Double)*): Model[LinearModel,C] = a.map(_.partial(xs:_*))

    def cleanup: Model[LinearModel,C] = a.map(_.cleanup)
    def fractional: Model[LinearModel,C] = a.map(_.fractional)
  }

  implicit class ModelNodeModelOps[C[A]<:Fields[A,C]](a: Model[NodeModel,C]) {
    implicit val dbl: C[Double] = a.config.convert(d = 0.0)
    def apply(xs: (String,Double)*): Model[Double,C] = a.map{nm => nm.eval(xs:_*) }
    def eval(xs: (String,Double)*): Model[Double,C] = a.map(_.eval(xs:_*))
  }
}
