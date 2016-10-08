package geotrellis.spark.streaming.crop

import geotrellis.raster._
import geotrellis.raster.crop.TileCropMethods
import geotrellis.spark._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._
import geotrellis.vector.Extent

import org.apache.spark.streaming.dstream.DStream

object Implicits extends Implicits

trait Implicits {
  implicit class withLayerRDDCropMethods[
    K: SpatialComponent,
    V <: CellGrid: (? => TileCropMethods[V]),
    M: Component[?, Bounds[K]]: GetComponent[?, Extent]: GetComponent[?, LayoutDefinition]
  ](val self: DStream[(K, V)] with Metadata[M])
    extends LayerDStreamCropMethods[K, V, M]
}
