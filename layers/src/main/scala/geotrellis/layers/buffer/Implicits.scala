package geotrellis.layers.buffer

import geotrellis.tiling.SpatialComponent
import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.stitch._

import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class withCollectionsBufferTilesMethodsWrapper[
    K: SpatialComponent,
    V <: CellGrid[Int]: Stitcher: (? => CropMethods[V])
  ](self: Seq[(K, V)]) extends CollectionBufferTilesMethods[K, V](self)
}
