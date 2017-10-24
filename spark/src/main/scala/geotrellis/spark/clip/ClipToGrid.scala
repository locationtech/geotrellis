/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.clip

import geotrellis.raster.GridBounds
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._
import geotrellis.vector._

import com.vividsolutions.jts.geom.prep.{PreparedGeometry, PreparedGeometryFactory}
import org.apache.spark.rdd._
import scala.util.Try

object ClipToGrid {
  /** Trait which contains methods to be used in determining
    * the most optimal way to clip geometries and features
    * for ClipToGrid methods.
    */
  trait Predicates {
    /** True if the feature geometry covers the passed-in [[Extent]]. */
    def covers(e: Extent): Boolean
    /** True if the feature geometry is covered by the passed-in [[Extent]]. */
    def coveredBy(e: Extent): Boolean
  }

  /** Clips a feature to the given [[Extent]], using the given [[Predicates]]
    * to avoid doing intersections where unnecessary.
    */
  def clipFeatureToExtent[G <: Geometry, D](
    e: Extent,
    f: Feature[G, D],
    preds: Predicates
  ): Option[Feature[Geometry, D]] = e match {
    /* If a Feature covers the Extent, their intersection would be the Extent itself. */
    case _ if preds.covers(e) => Some(Feature(e, f.data))
    /* The Feature may be completely contained within the Extent. In that case, no clipping need occur at all. */
    case _ if preds.coveredBy(e) => Some(f)
    /* Otherwise, we need to perform a JTS intersection */
    case _ => Try(f.geom.intersection(e)).toOption.flatMap(_.toGeometry.map(g => Feature(g, f.data)))
  }

  /** Clip each geometry in the RDD to the set of SpatialKeys
    * which intersect it, where the SpatialKeys map to the
    * given [[LayoutDefinition]].
    */
  def apply[G <: Geometry](
    layout: LayoutDefinition,
    rdd: RDD[G]
  )(implicit d: DummyImplicit): RDD[(SpatialKey, Geometry)] =
    apply[G, Unit](layout, rdd.map(Feature(_, ()))).mapValues(_.geom)

  /** Clip each geometry in the RDD to the set of SpatialKeys
    * which intersect it, where the SpatialKeys map to the
    * given [[LayoutDefinition]], using the given method
    * to clip each geometry to the extent.
    */
  def apply[G <: Geometry](
    clipGeom: (Extent, G, Predicates) => Option[Geometry],
    layout: LayoutDefinition,
    rdd: RDD[G]
  )(implicit d: DummyImplicit): RDD[(SpatialKey, Geometry)] = {
    val f = { (e: Extent, f: Feature[G, Unit], p: Predicates) => clipGeom(e, f.geom, p).map(Feature(_, ())) }

    apply[G, Unit](f, layout, rdd.map(Feature(_, ()))).mapValues(_.geom)
  }

  /** Clip each geometry in the RDD to the set of SpatialKeys
    * which intersect it, where the SpatialKeys map to the
    * given [[LayoutDefinition]], using the given method
    * to clip each geometry to the extent.
    */
  def apply[G <: Geometry, D](
    layout: LayoutDefinition,
    rdd: RDD[Feature[G, D]]
  ): RDD[(SpatialKey, Feature[Geometry, D])] =
    apply[G, D](clipFeatureToExtent[G,D] _, layout, rdd)

  /** Clip each geometry in the RDD to the set of SpatialKeys
    * which intersect it, where the SpatialKeys map to the
    * given [[LayoutDefinition]], using the given method
    * to clip each geometry to the extent.
    */
  def apply[G <: Geometry, D](
    clipFeature: (Extent, Feature[G, D], Predicates) => Option[Feature[Geometry, D]],
    layout: LayoutDefinition,
    rdd: RDD[Feature[G, D]]
  ): RDD[(SpatialKey, Feature[Geometry, D])] = {
    val mapTransform: MapKeyTransform = layout.mapTransform

    rdd.flatMap { f => clipGeom(clipFeature, mapTransform, f) }
  }

  /** Given a clipping function, clip a Geometry according to some
    * sensible, pre-defined [[Predicates]].
    */
  private def clipGeom[G <: Geometry, D](
    clipFeature: (Extent, Feature[G, D], Predicates) => Option[Feature[Geometry, D]],
    mapTransform: MapKeyTransform,
    feature: Feature[G, D]
  ): Iterator[(SpatialKey, Feature[Geometry, D])] = {

    /* Perform the actual clipping */
    def clipToKey(k: SpatialKey, preds: Predicates): Option[(SpatialKey, Feature[Geometry, D])] = {
      val extent: Extent = mapTransform(k)

      clipFeature(extent, feature, preds).map(k -> _)
    }

    val pointPredicates =
      new Predicates {
        def covers(e: Extent) = false
        def coveredBy(e: Extent) = true
      }

    val mpOrLinePredicates =
      new Predicates {
        def covers(e: Extent) = false
        def coveredBy(e: Extent) =
          feature.geom.jtsGeom.coveredBy(e.toPolygon.jtsGeom)
      }

    def polyPredicates(pg: PreparedGeometry) =
      new Predicates {
        def covers(e: Extent) = pg.covers(e.toPolygon.jtsGeom)
        def coveredBy(e: Extent) = pg.coveredBy(e.toPolygon.jtsGeom)
      }

    val gcPredicates =
      new Predicates {
        def covers(e: Extent) = feature.geom.jtsGeom.covers(e.toPolygon.jtsGeom)
        def coveredBy(e: Extent) = feature.geom.jtsGeom.coveredBy(e.toPolygon.jtsGeom)
      }

    val iterator: Iterator[(SpatialKey, Feature[Geometry, D])] =
      feature.geom match {
        case p: Point =>
          val k = mapTransform(p)
          clipToKey(k, pointPredicates).toSeq.iterator
        case mp: MultiPoint =>
          mp.points
            .map(mapTransform(_))
            .distinct
            .map(clipToKey(_, mpOrLinePredicates))
            .flatten
            .iterator
        case l: Line =>
          mapTransform.multiLineToKeys(MultiLine(l))
            .map(clipToKey(_, mpOrLinePredicates))
            .flatten
        case ml: MultiLine =>
          mapTransform.multiLineToKeys(ml)
            .map(clipToKey(_, mpOrLinePredicates))
            .flatten
        case p: Polygon =>
          val pg = PreparedGeometryFactory.prepare(p.jtsGeom)
          val preds = polyPredicates(pg)

          mapTransform
            .multiPolygonToKeys(MultiPolygon(p))
            .map(clipToKey(_, preds))
            .flatten
        case mp: MultiPolygon =>
          val pg = PreparedGeometryFactory.prepare(mp.jtsGeom)
          val preds = polyPredicates(pg)

          mapTransform.multiPolygonToKeys(mp)
            .map(clipToKey(_, preds))
            .flatten
        case gc: GeometryCollection =>
          def keysFromGC(g: GeometryCollection): List[SpatialKey] = {
            var keys: List[SpatialKey] = List()
            keys = keys ++ gc.points.map(mapTransform.apply)
            keys = keys ++ gc.multiPoints.flatMap(_.points.map(mapTransform.apply))
            keys = keys ++ gc.lines.flatMap { l => mapTransform.multiLineToKeys(MultiLine(l)) }
            keys = keys ++ gc.multiLines.flatMap { ml => mapTransform.multiLineToKeys(ml) }
            keys = keys ++ gc.polygons.flatMap { p => mapTransform.multiPolygonToKeys(MultiPolygon(p)) }
            keys = keys ++ gc.multiPolygons.flatMap { mp => mapTransform.multiPolygonToKeys(mp) }
            keys = keys ++ gc.geometryCollections.flatMap(keysFromGC)
            keys
          }

          keysFromGC(gc)
            .map(clipToKey(_, gcPredicates))
            .flatten
            .iterator
      }

    iterator
  }
}
