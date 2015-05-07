package geotrellis.spark.io.s3.spatial

import geotrellis.spark._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import scala.collection.JavaConversions._

object SpatialTileReader extends TileReader[SpatialKey] {
  val encodeKey = spatial.encodeKey
}
