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

import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory
import org.apache.spark.rdd._

object ClipToGrid {
  def apply[G <: Geometry, D](
    rdd: RDD[Feature[G, D]],
    layout: LayoutDefinition
  ): RDD[(SpatialKey, Feature[Geometry, D])] =
    apply[G, D, D](rdd, layout, { (_, d) => Some(d) })

  def apply[G <: Geometry, D1, D2](
    rdd: RDD[Feature[G, D1]],
    layout: LayoutDefinition,
    clipData: (Geometry, D1) => Option[D2]
  ): RDD[(SpatialKey, Feature[Geometry, D2])] = {
    val mapTransform: MapKeyTransform = layout.mapTransform

    rdd.flatMap { f => clipGeom(mapTransform, clipData, f) }
  }

  private def clipGeom[G <: Geometry, D1, D2](
    mapTransform: MapKeyTransform,
    clipData: (Geometry, D1) => Option[D2],
    feature: Feature[G, D1]
  ): Iterator[(SpatialKey, Feature[Geometry, D2])] = {

    def clipFeature(k: SpatialKey, contains: (Extent, G) => Boolean): Option[(SpatialKey, Feature[Geometry, D2])] = {
      val extent = mapTransform(k)

      val clipped: Option[Geometry] =
        feature.geom match {
          case p: Point =>
            Some(p)
          case g =>
            g.intersection(extent).toGeometry
        }

      for(g <- clipped; d <- clipData(g, feature.data)) yield {
        (k, Feature(g, d))
      }
    }

    val iterator: Iterator[(SpatialKey, Feature[Geometry, D2])] =
      feature.geom match {
        case p: Point =>
          val k = mapTransform(p)
          Iterator(clipData(p, feature.data).map { d => (k, Feature(p, d)) }).flatten
        case mp: MultiPoint =>
          mp.points
            .map(mapTransform(_))
            .distinct
            .map(clipFeature(_, { (x, y) => true }))
            .flatten
            .iterator
        case l: Line =>
          mapTransform.multiLineToKeys(MultiLine(l)).map(clipFeature(_, { (x,y) => false })).flatten
        case ml: MultiLine =>
          mapTransform.multiLineToKeys(ml).map(clipFeature(_, { (x,y) => false })).flatten
        case p: Polygon =>
          val pg = PreparedGeometryFactory.prepare(p.jtsGeom)
          val contains = { (extent: Extent, geom: G) =>
            pg.contains(extent.toPolygon.jtsGeom)
          }

          mapTransform.multiPolygonToKeys(MultiPolygon(p)).map(clipFeature(_, contains)).flatten
        case mp: MultiPolygon =>
          val pg = PreparedGeometryFactory.prepare(mp.jtsGeom)
          val contains = { (extent: Extent, geom: G) =>
            pg.contains(extent.toPolygon.jtsGeom)
          }

          mapTransform.multiPolygonToKeys(mp).map(clipFeature(_, contains)).flatten
        case gc: GeometryCollection =>
          val b: GridBounds = mapTransform(gc.envelope)

          val keys: Iterator[SpatialKey] = for {
            x <- Iterator.range(b.colMin, b.colMax+1)
            y <- Iterator.range(b.rowMin, b.rowMax+1)
          } yield SpatialKey(x, y)

          keys.filter(k => mapTransform(k).toPolygon.intersects(gc))
              .map(k => clipFeature(k, { (x,y) => false })).flatten
      }

    iterator
  }
}
