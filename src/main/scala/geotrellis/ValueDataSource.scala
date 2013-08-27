package geotrellis

class ValueDataSource[T:Manifest](val op:Op[T]) extends DataSource[T,T] {
  def partitions = Literal(List(op))
  def get = op
}

//object ValueDataSource {
//  implicit def canBuild[T]:CanBuildSourceFrom[DataSource[_,_],T,
//}
