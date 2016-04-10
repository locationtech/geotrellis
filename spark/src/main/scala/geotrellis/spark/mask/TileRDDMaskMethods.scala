package geotrellis.spark.mask

import geotrellis.raster._
import geotrellis.raster.mask._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.util._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

import Mask.Options

abstract class TileRDDMaskMethods[
    K: SpatialComponent: ClassTag,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
] extends MethodExtensions[RDD[(K, V)] with Metadata[M]] {
  /** Masks this raster by the given Polygon. */
  def mask(geom: Polygon): RDD[(K, V)] with Metadata[M] = mask(Seq(geom), Options.DEFAULT)

  def mask(geom: Polygon, options: Options): RDD[(K, V)] with Metadata[M] = mask(Seq(geom), options)

  /** Masks this raster by the given Polygons. */
  def mask(geoms: Traversable[Polygon]): RDD[(K, V)] with Metadata[M] = mask(geoms, Options.DEFAULT)

  def mask(geoms: Traversable[Polygon], options: Options): RDD[(K, V)] with Metadata[M] =
    Mask(self, geoms, options)

  /** Masks this raster by the given MultiPolygon. */
  def mask(geom: MultiPolygon): RDD[(K, V)] with Metadata[M] = mask(geom, Options.DEFAULT)

  def mask(geom: MultiPolygon, options: Options): RDD[(K, V)] with Metadata[M] = mask(Seq(geom), options)

  /** Masks this raster by the given MultiPolygons. */
  def mask(geoms: Traversable[MultiPolygon], options: Options)(implicit d: DummyImplicit): RDD[(K, V)] with Metadata[M] =
    Mask(self, geoms, options)

  /** Masks this raster by the given Extent. */
  def mask(ext: Extent): RDD[(K, V)] with Metadata[M] =
    mask(ext, Options.DEFAULT)

  /** Masks this raster by the given Extent. */
  def mask(ext: Extent, options: Options): RDD[(K, V)] with Metadata[M] =
    Mask(self, ext, options)
}
