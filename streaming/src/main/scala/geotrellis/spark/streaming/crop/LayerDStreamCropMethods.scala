package geotrellis.spark.streaming.crop

import geotrellis.raster._
import geotrellis.raster.crop.TileCropMethods
import geotrellis.raster.crop.Crop.Options
import geotrellis.spark._
import geotrellis.spark.crop.Crop
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._
import geotrellis.vector.Extent
import geotrellis.spark.streaming._

import org.apache.spark.streaming.dstream.DStream

abstract class LayerDStreamCropMethods[
  K: SpatialComponent,
  V <: CellGrid: (? => TileCropMethods[V]),
  M: Component[?, Bounds[K]]: GetComponent[?, Extent]: GetComponent[?, LayoutDefinition]
] extends MethodExtensions[DStream[(K, V)] with Metadata[M]] {
  def crop(extent: Extent, options: Options): DStream[(K, V)] with Metadata[M] =
    self.transformWithContext(Crop(_, extent, options))

  def crop(extent:Extent): DStream[(K, V)] with Metadata[M] =
    crop(extent, Options.DEFAULT)
}

