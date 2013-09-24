package geotrellis.source


import geotrellis._

object SeqDataSource {

  implicit def canBuildSourceFromGen[T]:CanBuildSourceFrom[DataSource[_,_],T,SeqDataSource[T]] = 
    new CanBuildSourceFrom[DataSource[_,_],
                           T,
                           SeqDataSource[T]] {
    def apply() = new SeqDataSourceBuilder[T]
    def apply(src:DataSource[_,_]) = 
      new SeqDataSourceBuilder[T]
    }
}

class SeqDataSource[A](val seqOp:Op[Seq[Op[A]]]) extends SeqDataSourceLike[A,SeqDataSource[A]] {
  def elements = seqOp
}
