/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.layers.mask

import geotrellis.vector._
import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.raster.mask._
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.layers.{Metadata, ContextCollection}
import geotrellis.util._

import scala.reflect.ClassTag


object Mask extends Mask {
  /** Options for masking tile RDDs.
    *
    * @param     rasterizerOptions        Options that dictate how to rasterize the masking geometries.
    * @param     filterEmptyTiles         If true, tiles that are completely masked will be filtered out
    *                                     of the RDD. If not, the empty tiles will still be elements of
    *                                     the resulting RDD.
    */
  case class Options(
    rasterizerOptions: Rasterizer.Options = Rasterizer.Options.DEFAULT,
    filterEmptyTiles: Boolean = true
  )

  object Options {
    def DEFAULT = Options()
    implicit def rasterizerOptionsToOptions(opt: Rasterizer.Options): Options = Options(opt)
  }
}

trait Mask {
  import Mask._
  // As done by [[geotrellis.raster.rasterize.polygon.TestLineSet]] in [[geotrellis.raster.rasterize.polygon.PolygonRasterizer]].
  private[geotrellis] def eliminateNotQualified(geom: Option[Geometry]): Option[Geometry] = {

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
    K: SpatialComponent,
    V,
    M: GetComponent[?, LayoutDefinition]
  ](seq: Seq[(K, V)] with Metadata[M], masker: (Extent, V) => Option[V]): Seq[(K, V)] with Metadata[M] = {
    val mapTransform = seq.metadata.getComponent[LayoutDefinition].mapTransform
    val masked =
      seq.flatMap { case (k, tile) =>
        val key = k.getComponent[SpatialKey]
        val tileExtent = mapTransform(key)
        masker(tileExtent, tile).map { result =>
          (k, result)
        }
      }

    ContextCollection(masked, seq.metadata)
  }

  def apply[
    K: SpatialComponent,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
  ](seq: Seq[(K, V)] with Metadata[M], geoms: Traversable[Polygon], options: Options): Seq[(K, V)] with Metadata[M] =
    _mask(seq, { case (tileExtent, tile) =>
      val tileGeoms = geoms.flatMap { g =>
        val intersections = g.intersectionSafe(tileExtent).toGeometry()
        eliminateNotQualified(intersections)
      }
      if(tileGeoms.isEmpty && options.filterEmptyTiles) { None }
      else {
        Some(tile.mask(tileExtent, tileGeoms, options.rasterizerOptions))
      }
    }: (Extent, V) => Option[V])

  /** Masks this raster by the given MultiPolygons. */
  def apply[
    K: SpatialComponent,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
  ](seq: Seq[(K, V)] with Metadata[M], geoms: Traversable[MultiPolygon], options: Options)(implicit d: DummyImplicit): Seq[(K, V)] with Metadata[M] =
    _mask(seq, { case (tileExtent, tile) =>
      val tileGeoms = geoms.flatMap { g =>
        val intersections = g.intersectionSafe(tileExtent).toGeometry()
        eliminateNotQualified(intersections)
      }
      if(tileGeoms.isEmpty && options.filterEmptyTiles) { None }
      else {
        Some(tile.mask(tileExtent, tileGeoms, options.rasterizerOptions))
      }
    }: (Extent, V) => Option[V])

  /** Masks this raster by the given Extent. */
  def apply[
    K: SpatialComponent,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
  ](seq: Seq[(K, V)] with Metadata[M], ext: Extent, options: Options): Seq[(K, V)] with Metadata[M] =
    _mask(seq, { case (tileExtent, tile) =>
      val tileExts = ext.intersection(tileExtent)
      tileExts match {
        case Some(intersected) if intersected.area != 0 => Some(tile.mask(tileExtent, intersected.toPolygon(), options.rasterizerOptions))
        case _ if options.filterEmptyTiles => None
        case _ => Some(tile.mask(tileExtent, Extent(0.0, 0.0, 0.0, 0.0), options.rasterizerOptions))
      }
    }: (Extent, V) => Option[V])

  def apply[
    K: SpatialComponent,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
  ](seq: Seq[(K, V)] with Metadata[M], ext: Extent): Seq[(K, V)] with Metadata[M] = {
    val options = Options.DEFAULT
    _mask(seq, {
      case (tileExtent, tile) =>
        val tileExts = ext.intersection(tileExtent)
        tileExts match {
          case Some(intersected) if intersected.area != 0 => Some(tile.mask(tileExtent, intersected.toPolygon(), options.rasterizerOptions))
          case _ if options.filterEmptyTiles => None
          case _ => Some(tile.mask(tileExtent, Extent(0.0, 0.0, 0.0, 0.0), options.rasterizerOptions))
        }
    }: (Extent, V) => Option[V])
  }
}
