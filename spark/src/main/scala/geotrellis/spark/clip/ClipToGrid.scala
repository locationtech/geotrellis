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

import org.locationtech.jts.geom.prep.{PreparedGeometry, PreparedGeometryFactory}
import org.apache.spark.rdd._

/**
  * These functions perform the following transformation:
  *
  * {{{
  * RDD[Geometry] => RDD[(SpatialKey, Geometry)]
  * }}}
  *
  * such that each original [[Geometry]] is clipped in some way. By default,
  * this clips them to fit inside the [[Extent]] of each [[SpatialKey]]
  * they touch.
  *
  * If you'd like more customized clipping behaviour, you can compose
  * over the [[clipFeatureToExtent]] function below, or write your own
  * entirely, while following its type signature. That can then be passed
  * into the appropriate `apply` method below.
  *
  * A variety of overloads are provided here to help you work with either
  * [[Geometry]] or [[Feature]]. The injected
  *
  * {{{
  * RDD[Geometry].clipToGrid: LayoutDefinition => RDD[(SpatialKey, Geometry)]
  * }}}
  *
  * may also be preferable to you.
  *
  * '''Note:''' If your custom clipping function relies on [[Predicates]],
  * note that its method `coveredBy` will always return `true` if your Geometry
  * fits inside the passed-in [[Extent]]. Please avoid writing clipping
  * functions that do non-sensical things.
  */
object ClipToGrid {
  /** Trait which contains methods to be used in determining
    * the most optimal way to clip geometries and features
    * for ClipToGrid methods.
    */
  trait Predicates extends Serializable {
    /** Returns true if the feature geometry covers the passed in extent */
    def covers(e: Extent): Boolean
    /** Returns true if the feature geometry is covered by the passed in extent */
    def coveredBy(e: Extent): Boolean
  }

  /** Clips a feature to the given extent, that uses the given predicates
    * to avoid doing intersections where unnecessary.
    */
  def clipFeatureToExtent[G <: Geometry, D](
    e: Extent,
    feature: Feature[G, D],
    preds: Predicates
  ): Option[Feature[Geometry, D]] =
    if(preds.covers(e)) { Some(Feature(e, feature.data)) }
    else if(preds.coveredBy(e)) { Some(feature) }
    else {
      feature.geom.intersection(e).toGeometry.map { g =>
        Feature(g, feature.data)
      }
    }

  /** Clip each geometry in the RDD to the set of SpatialKeys
    * which intersect it, where the SpatialKeys map to the
    * given [[LayoutDefinition]].
    */
  def apply[G <: Geometry](
    rdd: RDD[G],
    layout: LayoutDefinition
  )(implicit d: DummyImplicit): RDD[(SpatialKey, Geometry)] =
    apply[G, Unit](rdd.map(Feature(_, ())), layout)
      .mapValues(_.geom)

  /** Clip each geometry in the RDD to the set of SpatialKeys
    * which intersect it, where the SpatialKeys map to the
    * given [[LayoutDefinition]], using the given method
    * to clip each geometry to the extent.
    */
  def apply[G <: Geometry](
    rdd: RDD[G],
    layout: LayoutDefinition,
    clipGeom: (Extent, G, Predicates) => Option[Geometry]
  )(implicit d: DummyImplicit): RDD[(SpatialKey, Geometry)] =
    apply[G, Unit](
      rdd.map(Feature(_, ())),
      layout,
      { (e: Extent, f: Feature[G, Unit], p: Predicates) =>
        clipGeom(e, f.geom, p).map(Feature(_, ()))
      }
    ).mapValues(_.geom)

  /** Clip each geometry in the RDD to the set of SpatialKeys
    * which intersect it, where the SpatialKeys map to the
    * given [[LayoutDefinition]], using the given method
    * to clip each geometry to the extent.
    */
  def apply[G <: Geometry, D](
    rdd: RDD[Feature[G, D]],
    layout: LayoutDefinition
  ): RDD[(SpatialKey, Feature[Geometry, D])] =
    apply[G, D](rdd, layout, clipFeatureToExtent[G,D] _)

  /** Clip each geometry in the RDD to the set of SpatialKeys
    * which intersect it, where the SpatialKeys map to the
    * given [[LayoutDefinition]], using the given method
    * to clip each geometry to the extent.
    */
  def apply[G <: Geometry, D](
    rdd: RDD[Feature[G, D]],
    layout: LayoutDefinition,
    clipFeature: (Extent, Feature[G, D], Predicates) => Option[Feature[Geometry, D]]
  ): RDD[(SpatialKey, Feature[Geometry, D])] = {
    val mapTransform: MapKeyTransform = layout.mapTransform

    rdd.flatMap { f => clipGeom(mapTransform, clipFeature, f) }
  }

  private def clipGeom[G <: Geometry, D](
    mapTransform: MapKeyTransform,
    clipFeature: (Extent, Feature[G, D], Predicates) => Option[Feature[Geometry, D]],
    feature: Feature[G, D]
  ): Iterator[(SpatialKey, Feature[Geometry, D])] = {

    /* Unique keys that this Feature touches */
    val keys: Set[SpatialKey] = mapTransform.keysForGeometry(feature.geom)

    lazy val mpOrLinePredicates =
      new Predicates {
        def covers(e: Extent) = false
        def coveredBy(e: Extent) = keys.size < 2
      }

    def preparedPredicates(pg: PreparedGeometry) =
      new Predicates {
        def covers(e: Extent) = pg.covers(e.toPolygon.jtsGeom)
        def coveredBy(e: Extent) = keys.size < 2
      }

    lazy val polyPredicates =
      new Predicates {
        def covers(e: Extent) = feature.geom.jtsGeom.covers(e.toPolygon.jtsGeom)
        def coveredBy(e: Extent) = keys.size < 2
      }

    lazy val gcPredicates =
      new Predicates {
        def covers(e: Extent) = feature.geom.jtsGeom.covers(e.toPolygon.jtsGeom)
        def coveredBy(e: Extent) = keys.size < 2
      }

    def clipToKey(
      k: SpatialKey,
      preds: Predicates
    ): Option[(SpatialKey, Feature[Geometry, D])] = {
      val extent = mapTransform(k)

      clipFeature(extent, feature, preds).map(k -> _)
    }

    val iterator: Iterator[(SpatialKey, Feature[Geometry, D])] = {
      feature.geom match {
        /* Points will always exist inside their Extent */
        case p:  Point      => keys.iterator.map(k => (k, feature))
        case mp: MultiPoint => keys.iterator.flatMap(k => clipToKey(k, mpOrLinePredicates))
        case l:  Line       => keys.iterator.flatMap(k => clipToKey(k, mpOrLinePredicates))
        case ml: MultiLine  => keys.iterator.flatMap(k => clipToKey(k, mpOrLinePredicates))
        case p:  Polygon if keys.size > 10 =>
          val pg = PreparedGeometryFactory.prepare(p.jtsGeom)
          val preds = preparedPredicates(pg)

          keys.iterator.flatMap(k => clipToKey(k, preds))
        case p:  Polygon => keys.iterator.flatMap(k => clipToKey(k, polyPredicates))
        case mp: MultiPolygon if keys.size > 10 =>
          val pg = PreparedGeometryFactory.prepare(mp.jtsGeom)
          val preds = preparedPredicates(pg)

          keys.iterator.flatMap(k => clipToKey(k, preds))
        case mp: MultiPolygon       => keys.iterator.flatMap(k => clipToKey(k, polyPredicates))
        case gc: GeometryCollection => keys.iterator.flatMap(k => clipToKey(k, gcPredicates))
      }
    }

    iterator
  }
}
