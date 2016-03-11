package geotrellis.spark.reproject

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.raster.reproject._
import geotrellis.raster.stitch._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.ingest._
import geotrellis.proj4._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd._

import scala.reflect.ClassTag


class TileRDDReprojectMethods[
  K: SpatialComponent: Boundable: ClassTag,
  V <: CellGrid: ClassTag: Stitcher: (? => TileReprojectMethods[V]): (? => CropMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
](val self: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) extends MethodExtensions[RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] {
  import Reproject.Options

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme, options: Options): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    TileRDDReproject(self, destCrs, Left(layoutScheme), options)

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(destCrs, layoutScheme, Options.DEFAULT)

  def reproject(zoomedLayoutScheme: ZoomedLayoutScheme, options: Options): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(zoomedLayoutScheme.crs, zoomedLayoutScheme, options)

  def reproject(zoomedLayoutScheme: ZoomedLayoutScheme): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(zoomedLayoutScheme, Options.DEFAULT)

  def reproject(zoomedLayoutScheme: ZoomedLayoutScheme, bufferSize: Int, options: Options): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(zoomedLayoutScheme.crs, zoomedLayoutScheme, bufferSize, options)

  def reproject(zoomedLayoutScheme: ZoomedLayoutScheme, bufferSize: Int): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(zoomedLayoutScheme, bufferSize, Options.DEFAULT)

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme, bufferSize: Int, options: Options): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    TileRDDReproject(self, destCrs, Left(layoutScheme), bufferSize, options)

  def reproject(destCrs: CRS, layoutScheme: LayoutScheme, bufferSize: Int): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    reproject(destCrs, layoutScheme, bufferSize, Options.DEFAULT)

  def reproject(destCrs: CRS, layoutDefinition: LayoutDefinition, options: Options): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    TileRDDReproject(self, destCrs, Right(layoutDefinition), options)

  def reproject(destCrs: CRS, layoutDefinition: LayoutDefinition): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) =
    TileRDDReproject(self, destCrs, Right(layoutDefinition), Options.DEFAULT)
}
