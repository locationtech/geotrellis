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

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.vector._

import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory
import org.apache.spark.rdd._
import scala.util.Try

object ClipToGrid {

  /** Clip each geometry in the RDD to the set of SpatialKeys
    * which intersect it, where the SpatialKeys map to the
    * given [[LayoutDefinition]].
    */
  def apply[G <: Geometry](layout: LayoutDefinition, rdd: RDD[G]): RDD[(SpatialKey, Geometry)] =
    apply(layout, rdd, byExtent)

  /** Clip each geometry in the RDD to the set of SpatialKeys
    * which intersect it, where the SpatialKeys map to the
    * given [[LayoutDefinition]], using the given method
    * to clip each geometry to the extent.
    */
  def apply[G <: Geometry](
    layout: LayoutDefinition,
    rdd: RDD[G],
    clipGeom: (Extent, Feature[G, Unit]) => Option[Feature[Geometry, Unit]]
  ): RDD[(SpatialKey, Geometry)] =
    apply(layout, rdd.map(Feature(_, ())), clipGeom).mapValues(_.geom)

  /** Clip each geometry in the RDD to the set of SpatialKeys
    * which intersect it, where the SpatialKeys map to the
    * given [[LayoutDefinition]], using the given method
    * to clip each geometry to the extent.
    */
  def apply[G <: Geometry, D](
    layout: LayoutDefinition,
    rdd: RDD[Feature[G, D]]
  )(implicit d: DummyImplicit): RDD[(SpatialKey, Feature[Geometry, D])] =
    apply(layout, rdd, byExtent)

  /** Clip each geometry in the RDD to the set of SpatialKeys
    * which intersect it, where the SpatialKeys map to the
    * given [[LayoutDefinition]], using the given method
    * to clip each geometry to the extent.
    */
  def apply[G <: Geometry, D](
    layout: LayoutDefinition,
    rdd: RDD[Feature[G, D]],
    clipFeature: (Extent, Feature[G, D]) => Option[Feature[Geometry, D]]
  )(implicit d: DummyImplicit): RDD[(SpatialKey, Feature[Geometry, D])] = {
    val mapTransform: MapKeyTransform = layout.mapTransform

    /* Associate each Feature with its SpatialKeys*/
    val withKeys: RDD[(Iterator[SpatialKey], Feature[G,D])] =
      rdd.map(f => (mapTransform.keysForGeometry(f.geom), f))

    /* Clip every Feature */
    withKeys.flatMap { case (ks, f) => ks.flatMap(k => clipFeature(k.extent(layout), f).map((k, _))) }
  }

  /** Clips Features to fit the given Extent. Does its best to avoid unnecessary
    * JTS interactions.
    */
  def byExtent[G <: Geometry, D](extent: Extent, f: Feature[G, D]): Option[Feature[Geometry, D]] = f.geom match {
    case g: Point                              => Some(f)
    case g: MultiPoint if coveredBy(g, extent) => Some(f)
    case g: MultiPoint                         => clip(extent, g).map(Feature(_, f.data))
    case g: Line       if coveredBy(g, extent) => Some(f)
    case g: Line                               => clip(extent, g).map(Feature(_, f.data))
    case g: MultiLine  if coveredBy(g, extent) => Some(f)
    case g: MultiLine                          => clip(extent, g).map(Feature(_, f.data))
    case g =>  /* Polygon and MultiPolygon */
      val pg = PreparedGeometryFactory.prepare(g.jtsGeom)
      val ep = extent.toPolygon.jtsGeom

      if (pg.covers(ep)) Some(Feature(extent, f.data))
      else if (pg.coveredBy(ep)) Some(f)
      else clip(extent, g).map(Feature(_, f.data))
  }

  private def coveredBy[G <: Geometry](g: G, e: Extent): Boolean =
    g.jtsGeom.coveredBy(e.toPolygon.jtsGeom)

  private def clip[G <: Geometry](e: Extent, g: G): Option[Geometry] = {
    val exPoly: Polygon = e.toPolygon

    val clipped: Try[Geometry] = g match {
      case mp: MultiPolygon => Try(MultiPolygon(mp.polygons.flatMap(_.intersection(exPoly).as[Polygon])))
      case _ => Try(g.intersection(exPoly).toGeometry.get)
    }

    clipped.toOption
  }

  /** Clips Features to a 3x3 grid surrounding the current Tile.
    * This has been found to capture ''most'' Features which stretch
    * outside their original Tile, and helps avoid the pain of
    * restitching later.
    */
  def byBufferedExtent[G <: Geometry, D](
    extent: Extent,
    f: Feature[G, D]
  ): Option[Feature[Geometry, D]] = f.geom match {
    case g: Point => Some(f)  /* A `Point` will always fall within the Extent */
    case _ => byExtent(extent.expandBy(extent.width, extent.height), f)
  }

}
