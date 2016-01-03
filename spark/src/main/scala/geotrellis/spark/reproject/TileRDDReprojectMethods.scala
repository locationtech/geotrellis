package geotrellis.spark.reproject

import geotrellis.raster._
import geotrellis.raster.stitch._
import geotrellis.raster.crop._
import geotrellis.raster.reproject._
import geotrellis.raster.mosaic._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.ingest._
import geotrellis.proj4._

import org.apache.spark.rdd._

import scala.reflect.ClassTag

class TileRDDReprojectMethods[
  K: SpatialComponent: ClassTag,
  V <: CellGrid: ClassTag: Stitcher: (? => TileReprojectMethods[V]): (? => CropMethods[V]): (? => MergeMethods[V]): (? => CellGridPrototype[V])
](val self: RDD[(K, V)] with Metadata[RasterMetaData]) extends MethodExtensions[RDD[(K, V)] with Metadata[RasterMetaData]] {
  import geotrellis.raster.reproject.Reproject.Options

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme, options: Options): (Int, RDD[(K, V)] with Metadata[RasterMetaData]) =
    TileRDDReproject(self, destCrs, layoutScheme, options)

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme): (Int, RDD[(K, V)] with Metadata[RasterMetaData]) =
    reproject(destCrs, layoutScheme, Options.DEFAULT)

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme, bufferSize: Int, options: Options): (Int, RDD[(K, V)] with Metadata[RasterMetaData]) =
    TileRDDReproject(self, destCrs, layoutScheme, bufferSize, options)

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme, bufferSize: Int): (Int, RDD[(K, V)] with Metadata[RasterMetaData]) =
    reproject(destCrs, layoutScheme, bufferSize, Options.DEFAULT)
}
