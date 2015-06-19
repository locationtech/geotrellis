package geotrellis.spark.io

import geotrellis.spark.{SpaceTimeKey, SpatialKey}
import scala.reflect.ClassTag

package object s3 {
  implicit def s3SpatialRasterRDDReader[T: ClassTag] = new spatial.SpatialRasterRDDReader[T]
  implicit def s3SpatialRasterRDDWriter[T: ClassTag] = new spatial.SpatialRasterRDDWriter[T]
  implicit def s3SpatialRasterTileReader[T: ClassTag] = new spatial.SpatialTileReader[T]

  implicit def s3SpaceTimeRasterRDDReader[T: ClassTag] = new spacetime.SpaceTimeRasterRDDReader[T]
  implicit def s3SpaceTimeRasterRDDWriter[T: ClassTag] = new spacetime.SpaceTimeRasterRDDWriter[T]
  implicit def s3SpaceTimeRasterTileReader[T: ClassTag] = new spacetime.SpaceTimeTileReader[T]

  private[s3]
  def maxIndexWidth(maxIndex: Long): Int = {
    def digits(x: Long): Int = if (x < 10) 1 else 1 + digits(x/10)
    digits(maxIndex)
  }

}
