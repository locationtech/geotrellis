package geotrellis

import geotrellis._

class SingleDataSource[T,P](val partitions:Op[Seq[Op[P]]])
                           (implicit converger:CanConvergeTo[T,P]) 
    extends DataSource[T,P] {
  def get:Op[T] =
    converger.converge(partitions)
}

trait CanConvergeTo[T,P] {
  def converge(ops:Op[Seq[Op[P]]]):Op[T]
}

import geotrellis.statistics._

object SingleDataSource {
  implicit def canConvergeTo:CanConvergeTo[Histogram,Histogram] =
    new CanConvergeTo[Histogram,Histogram] {
      def converge(ops:Op[Seq[Op[Histogram]]]):Op[Histogram] =
        logic.Collect(ops)
             .map(FastMapHistogram.fromHistograms(_))
    }

  implicit def canConvergeToSelf[T]:CanConvergeTo[T,T] = 
    new CanConvergeTo[T,T] {
      def converge(ops:Op[Seq[Op[T]]]):Op[T] = 
        logic.Collect(ops).map(_.head)
    }

  implicit def canBuild[T,U]:CanBuildSourceFrom[SingleDataSource[U,U],T,SingleDataSource[T,T]] = 
    new CanBuildSourceFrom[SingleDataSource[U,U], T, SingleDataSource[T,T] ] {
      def apply() = new SingleDataSourceBuilder[T,T] 
      def apply(from:SingleDataSource[U,U]) = new SingleDataSourceBuilder[T,T]
    }
}
