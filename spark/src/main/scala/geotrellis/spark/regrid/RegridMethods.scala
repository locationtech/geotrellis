package geotrellis.spark.regrid

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.prototype._
import geotrellis.raster.stitch._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

class RegridMethods[
  K: SpatialComponent: ClassTag,
  V <: CellGrid: ClassTag: Stitcher: (? => CropMethods[V]),
  M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
](val self: RDD[(K, V)] with Metadata[M]) extends MethodExtensions[RDD[(K, V)] with Metadata[M]] {

  def regrid(tileCols: Int, tileRows: Int): RDD[(K, V)] with Metadata[M] = Regrid(self, tileCols, tileRows)

  def regrid(tileSize: Int): RDD[(K, V)] with Metadata[M] = Regrid(self, tileSize)

}  
