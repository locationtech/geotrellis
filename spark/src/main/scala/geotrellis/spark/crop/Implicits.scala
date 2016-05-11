package geotrellis.spark.crop

import geotrellis.raster._
import geotrellis.raster.crop.TileCropMethods
import geotrellis.spark._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._
import geotrellis.vector.Extent

import org.apache.spark.rdd.RDD

object Implicits extends Implicits

trait Implicits {
  implicit class withLayerRDDCropMethods[
    K: SpatialComponent,
    V <: CellGrid: (? => TileCropMethods[V]),
    M: Component[?, Bounds[K]]: GetComponent[?, Extent]: GetComponent[?, LayoutDefinition]
  ](val self: RDD[(K, V)] with Metadata[M])
      extends LayerRDDCropMethods[K, V, M]
}
