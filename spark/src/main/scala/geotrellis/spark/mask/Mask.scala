package geotrellis.spark.mask

import geotrellis.raster._
import geotrellis.raster.mask._
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.util._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object Mask {
  // As done by [[geotrellis.raster.rasterize.polygon.TestLineSet]] in [[geotrellis.raster.rasterize.polygon.PolygonRasterizer]].
  private def eliminateNotQualified(geom: Option[Geometry]): Option[Geometry] = {

    def rec(geom: GeometryCollection): GeometryCollection = geom match {
      case GeometryCollection(_, lines, polygons, multiPoints, multiLines, multiPolygons, geometryCollections) =>
        GeometryCollection(
          Seq(),
          lines.filter(_.envelope.area != 0),
          polygons,
          multiPoints,
          multiLines,
          multiPolygons,
          geometryCollections.map(rec))
    }

    geom match {
      case Some(g: Line) if g.envelope.area == 0 => None
      case Some(_: Point) => None
      case Some(g: GeometryCollection) => Some(rec(g))
      case _ => geom
    }
  }

  private def _mask[
    K: SpatialComponent: ClassTag,
    V,
    M: GetComponent[?, LayoutDefinition]
  ](rdd: RDD[(K, V)] with Metadata[M], masker: (Extent, V) => Option[V]): RDD[(K, V)] with Metadata[M] = {
    val mapTransform = rdd.metadata.getComponent[LayoutDefinition].mapTransform
    val masked =
      rdd.mapPartitions({ partition =>
        partition.flatMap { case (k, tile) =>
          val key = k.getComponent[SpatialKey]
          val tileExtent = mapTransform(key)
          masker(tileExtent, tile).map { result =>
            (k, result)
          }
        }
      }, preservesPartitioning = true)
    ContextRDD(masked, rdd.metadata)
  }

  def apply[
    K: SpatialComponent: ClassTag,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
  ](rdd: RDD[(K, V)] with Metadata[M], geoms: Traversable[Polygon], options: Options): RDD[(K, V)] with Metadata[M] =
    _mask(rdd, { case (tileExtent, tile) =>
      val tileGeoms = geoms.flatMap { g =>
        val intersections = g.safeIntersection(tileExtent).toGeometry()
        eliminateNotQualified(intersections)
      }
      if(tileGeoms.isEmpty) { None }
      else {
        Some(tile.mask(tileExtent, tileGeoms, options))
      }
    })

  /** Masks this raster by the given MultiPolygons. */
  def apply[
    K: SpatialComponent: ClassTag,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
  ](rdd: RDD[(K, V)] with Metadata[M], geoms: Traversable[MultiPolygon], options: Options)(implicit d: DummyImplicit): RDD[(K, V)] with Metadata[M] =
    _mask(rdd, { case (tileExtent, tile) =>
      val tileGeoms = geoms.flatMap { g =>
        val intersections = g.safeIntersection(tileExtent).toGeometry()
        eliminateNotQualified(intersections)
      }
      if(tileGeoms.isEmpty) { None }
      else {
        Some(tile.mask(tileExtent, tileGeoms, options))
      }
    })

  /** Masks this raster by the given Extent. */
  def apply[
    K: SpatialComponent: ClassTag,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
  ](rdd: RDD[(K, V)] with Metadata[M], ext: Extent, options: Options = Options.DEFAULT): RDD[(K, V)] with Metadata[M] =
    _mask(rdd, { case (tileExtent, tile) =>
      val tileExts = ext.intersection(tileExtent)
      tileExts match {
        case Some(intersected) if intersected.area != 0 => Some(tile.mask(tileExtent, intersected.toPolygon(), options))
        case _ => None
      }
    })

}
