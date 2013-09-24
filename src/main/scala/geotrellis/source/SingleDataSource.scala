package geotrellis.source

import geotrellis._
import geotrellis.statistics._

/*
class SingleDataSource[T,P](val partitions:Op[Seq[Op[P]]]) extends DataSource[T,P] {
  def get:Op[T] = ??? //converger.converge(partitions)
}


object SingleDataSource {
  implicit def canBuild[T,U]:CanBuildSourceFrom[SingleDataSource[U,U],T,SingleDataSource[T,T]] = 
    new CanBuildSourceFrom[SingleDataSource[U,U], T, SingleDataSource[T,T] ] {
      def apply() = new SingleDataSourceBuilder[T,T] 
      def apply(from:SingleDataSource[U,U]) = new SingleDataSourceBuilder[T,T]
    }
}
 */
