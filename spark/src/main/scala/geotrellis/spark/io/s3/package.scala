package geotrellis.spark.io

import geotrellis.spark._
import org.apache.spark.rdd.RDD

package object s3 {
  implicit def s3SpatialRasterRDDReader = new DirectRasterRDDReader[SpatialKey]
  implicit def s3SpatialRasterRDDWriter = new RasterRDDWriter[SpatialKey]
  implicit def s3SpatialRasterTileReader = new TileReader[SpatialKey]

  implicit def s3SpaceTimeRasterRDDReader = new DirectRasterRDDReader[SpaceTimeKey]
  implicit def s3SpaceTimeRasterRDDWriter = new RasterRDDWriter[SpaceTimeKey]
  implicit def s3SpaceTimeRasterTileReader = new TileReader[SpaceTimeKey]

  private[s3]
  def maxIndexWidth(maxIndex: Long): Int = {
    def digits(x: Long): Int = if (x < 10) 1 else 1 + digits(x/10)
    digits(maxIndex)
  }

  private[s3]
  val encodeIndex = (index: Long, max: Int) => {
    index.toString.reverse.padTo(max, '0').reverse
  }

  implicit class S3RDDExtensions[K,V](rdd: RDD[(K,V)]) extends SaveToS3Methods[K, V](rdd)
}
