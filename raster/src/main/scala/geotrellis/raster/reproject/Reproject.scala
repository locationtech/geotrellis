package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.raster.resample._

object Reproject {
  trait Apply[T] extends Serializable {
    def apply(method: ResampleMethod = NearestNeighbor, errorThreshold: Double = 0.125): T
  }
  implicit def applyToCall[T](a: Apply[T]): T = a()

  class NoOpApply[T](v: T) extends Apply[T] { 
    def apply(method: ResampleMethod = NearestNeighbor, errorThreshold: Double = 0.125): T = v
  }
}
