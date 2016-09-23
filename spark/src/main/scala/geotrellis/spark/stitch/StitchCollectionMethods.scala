package geotrellis.spark.stitch

import geotrellis.raster._
import geotrellis.raster.stitch.Stitcher
import geotrellis.vector.Extent
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._

import org.apache.spark.rdd.RDD

abstract class SpatialTileLayoutCollectionStitchMethods[V <: CellGrid: Stitcher, M: GetComponent[?, LayoutDefinition]]
  extends MethodExtensions[Seq[(SpatialKey, V)] with Metadata[M]] {

  def stitch(): Raster[V] = {
    val (tile, bounds) = TileLayoutStitcher.stitch(self)
    val mapTransform = self.metadata.getComponent[LayoutDefinition].mapTransform
    Raster(tile, mapTransform(bounds))
  }
}

abstract class SpatialTileCollectionStitchMethods[V <: CellGrid: Stitcher]
  extends MethodExtensions[Seq[(SpatialKey, V)]] {

  def stitch(): V = TileLayoutStitcher.stitch(self)._1
}
