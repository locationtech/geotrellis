package geotrellis.spark.stitch

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.spark.tiling.MapKeyTransform
import geotrellis.spark._
import org.apache.spark.rdd.RDD

object Implicits extends Implicits

trait Implicits {
  implicit class withSpatialTileLayoutRDDMethods[V <: CellGrid, M: ? => MapKeyTransform](
    val self: RDD[(GridKey, V)] with Metadata[M]
  ) extends SpatialTileLayoutRDDMethods[V, M]

  implicit class withSpatialTileRDDMethods[V <: CellGrid](
    val self: RDD[(GridKey, V)]
  ) extends SpatialTileRDDMethods[V]
}