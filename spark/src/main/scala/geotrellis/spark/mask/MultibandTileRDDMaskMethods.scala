package geotrellis.spark.mask

import geotrellis.raster._
import geotrellis.raster.mask._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.util._

import Mask.Options

abstract class MultibandTileCollectionMaskMethods[
    K: SpatialComponent,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
] extends MethodExtensions[Seq[(K, V)] with Metadata[M]] {
  /** Masks this MultibandTile by the given Polygon. */
  def mask(geom: Polygon): Seq[(K, V)] with Metadata[M] = mask(Seq(geom), Options.DEFAULT)

  def mask(geom: Polygon, options: Options): Seq[(K, V)] with Metadata[M] = mask(Seq(geom), options)

  /** Masks this MultibandTile by the given Polygons. */
  def mask(geoms: Traversable[Polygon]): Seq[(K, V)] with Metadata[M] = mask(geoms, Options.DEFAULT)

  def mask(geoms: Traversable[Polygon], options: Options): Seq[(K, V)] with Metadata[M] =
    Mask(self, geoms, options)

  /** Masks this MultibandTile by the given MultiPolygon. */
  def mask(geom: MultiPolygon): Seq[(K, V)] with Metadata[M] = mask(geom, Options.DEFAULT)

  def mask(geom: MultiPolygon, options: Options): Seq[(K, V)] with Metadata[M] = mask(Seq(geom), options)

  /** Masks this MultibandTile by the given MultiPolygons. */
  def mask(geoms: Traversable[MultiPolygon], options: Options)(implicit d: DummyImplicit): Seq[(K, V)] with Metadata[M] =
    Mask(self, geoms, options)

  /** Masks this MultibandTile by the given Extent. */
  def mask(ext: Extent): Seq[(K, V)] with Metadata[M] =
    mask(ext, Options.DEFAULT)

  /** Masks this MultibandTile by the given Extent. */
  def mask(ext: Extent, options: Options): Seq[(K, V)] with Metadata[M] =
    Mask(self, ext, options)
}
