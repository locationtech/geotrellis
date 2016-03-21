package geotrellis.spark.resample

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.spark._
import geotrellis.spark.tiling.ZoomedLayoutScheme
import geotrellis.util.MethodExtensions
import geotrellis.vector.Extent

abstract class ZoomResampleMethods[K: SpatialComponent](val self: TileLayerRDD[K]) extends MethodExtensions[TileLayerRDD[K]] {
  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int
  ): TileLayerRDD[K] =
    resampleToZoom(sourceZoom, targetZoom, None, NearestNeighbor)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    method: ResampleMethod
  ): TileLayerRDD[K] =
    resampleToZoom(sourceZoom, targetZoom, None, method)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    targetGridBounds: GridBounds
  ): TileLayerRDD[K] =
    resampleToZoom(sourceZoom, targetZoom, Some(targetGridBounds), NearestNeighbor)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    targetGridBounds: GridBounds,
    method: ResampleMethod
  ): TileLayerRDD[K] =
    resampleToZoom(sourceZoom, targetZoom, Some(targetGridBounds), method)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    targetExtent: Extent
  ): TileLayerRDD[K] =
    resampleToZoom(sourceZoom, targetZoom, targetExtent, NearestNeighbor)

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int,
    targetExtent: Extent,
    method: ResampleMethod
  ): TileLayerRDD[K] = {
    val layout = ZoomedLayoutScheme.layoutForZoom(targetZoom, self.metadata.layout.extent, self.metadata.layout.tileLayout.tileCols)
    val targetGridBounds = layout.mapTransform(targetExtent)
    resampleToZoom(sourceZoom, targetZoom, Some(targetGridBounds), method)
  }

  def resampleToZoom(
    sourceZoom: Int,
    targetZoom: Int ,
    targetGridBounds: Option[GridBounds] = None,
    method: ResampleMethod = NearestNeighbor
  ): TileLayerRDD[K] =
    ZoomResample(self, sourceZoom, targetZoom, targetGridBounds, method)
}
