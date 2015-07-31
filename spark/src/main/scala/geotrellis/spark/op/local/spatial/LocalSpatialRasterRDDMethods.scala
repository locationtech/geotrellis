package geotrellis.spark.op.local.spatial

import geotrellis.raster.{ArrayTile, Tile}
import geotrellis.raster.op.local._
import geotrellis.spark._
import geotrellis.vector._

abstract class LocalSpatialRasterRDDMethods[K: SpatialComponent] extends RasterRDDMethods[K] with Serializable {

  /** Masks this raster by the given Polygon. */
  def mask(geom: Polygon): RasterRDD[K] =
    mask(Seq(geom))

  /** Masks this raster by the given Polygons. */
  def mask(geoms: Traversable[Polygon]): RasterRDD[K] =
    _mask { case (tileExtent, tile) =>
      val tileGeoms = geoms.flatMap { g =>
        val intersections = g.safeIntersection(tileExtent).toGeometry()
        eliminateNotQualified(intersections)
      }
      tile.mask(tileExtent, tileGeoms)
    }

  /** Masks this raster by the given MultiPolygon. */
  def mask(geom: MultiPolygon): RasterRDD[K] =
    mask(Seq(geom))

  /** Masks this raster by the given MultiPolygons. */
  def mask(geoms: Traversable[MultiPolygon])(implicit d: DummyImplicit): RasterRDD[K] =
    _mask { case (tileExtent, tile) =>
      val tileGeoms = geoms.flatMap { g =>
        val intersections = g.safeIntersection(tileExtent).toGeometry()
        eliminateNotQualified(intersections)
      }
      tile.mask(tileExtent, tileGeoms)
    }

  /** Masks this raster by the given Extent. */
  def mask(ext: Extent): RasterRDD[K] =
    _mask { case (tileExtent, tile) =>
      val tileExts = ext.intersection(tileExtent)
      tileExts match {
        case Some(intersected) if intersected.area != 0 => tile.mask(tileExtent, intersected.toPolygon())
        case _ => ArrayTile.empty(tile.cellType, tile.cols, tile.rows)
      }
    }

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

  private def _mask(masker: (Extent, Tile) => Tile): RasterRDD[K] = {
    val mapTransform = rasterRDD.metaData.mapTransform
    rasterRDD.mapPairs { case (k, tile) =>
      val key = k.spatialComponent
      val tileExtent = mapTransform(key)
      val result = masker(tileExtent, tile)
      (k, result)
    }
  }

}