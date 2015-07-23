package geotrellis.spark.io.s3.spatial

import geotrellis.spark._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._
import scala.reflect.ClassTag
import scala.collection.JavaConversions._

class SpatialTileReader[T: ClassTag] extends TileReader[SpatialKey, T] {
  val encodeKey = spatial.encodeKey
}
