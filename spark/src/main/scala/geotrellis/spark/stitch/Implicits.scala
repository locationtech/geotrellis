package geotrellis.spark.stitch

import geotrellis.raster._
import geotrellis.raster.stitch._
import geotrellis.vector.Extent
import geotrellis.spark.tiling._
import geotrellis.spark._
import geotrellis.util._

import org.apache.spark.rdd.RDD

object Implicits extends Implicits

trait Implicits {
  implicit class withSpatialTileLayoutRDDMethods[V <: CellGrid: Stitcher, M: GetComponent[?, LayoutDefinition]](
    val self: RDD[(SpatialKey, V)] with Metadata[M]
  ) extends SpatialTileLayoutRDDStitchMethods[V, M]

  implicit class withSpatialTileRDDMethods[V <: CellGrid: Stitcher](
    val self: RDD[(SpatialKey, V)]
  ) extends SpatialTileRDDStitchMethods[V]
}
