/*
 * Copyright (c) 2016 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.geotools

import geotrellis.vector._

import com.vividsolutions.jts.{geom => jts}
import org.opengis.feature.simple.SimpleFeature
import scala.collection._

import scala.collection.JavaConverters._


object SimpleFeatureToFeature {

  private def jtsToGeotrellis(geometry: Object): Geometry = {
    geometry match {
      case lr: jts.LinearRing => Line(lr)
      case ls: jts.LineString => Line(ls)
      case pt: jts.Point => Point(pt)
      case pg: jts.Polygon => Polygon(pg)
      case mp: jts.MultiPoint => MultiPoint(mp)
      case ml: jts.MultiLineString => MultiLine(ml)
      case mp: jts.MultiPolygon => MultiPolygon(mp)
      case gc: jts.GeometryCollection => GeometryCollection(gc)
      case  g: jts.Geometry => throw new Exception(s"Unhandled JTS Geometry $g")
      case _ => throw new Exception("Non-Geometry")
    }
  }

  def apply(simpleFeature: SimpleFeature): Feature[Geometry, immutable.Map[String, Object]] = {
    val properties = simpleFeature.getProperties.asScala
    val map = mutable.Map.empty[String, Object]
    val defaultGeom = simpleFeature.getDefaultGeometry
    var geometry: Geometry = if (defaultGeom != null) jtsToGeotrellis(defaultGeom); else null

    properties.foreach({ property =>
      (defaultGeom, property.getValue) match {
        case (null, g: jts.Geometry) => geometry = jtsToGeotrellis(g)
        case (g1: jts.Geometry, g2: jts.Geometry) =>
          val key = property.getName.toString
          val value = jtsToGeotrellis(g2)
          if (g1.toString != g2.toString) {
            map += (key -> value)
          }
        case _ =>
          val key = property.getName.toString
          val value = property.getValue
          map += (key -> value)
      }
    })

    assert(geometry != null, s"$simpleFeature")
    Feature(geometry, map.toMap)
  }
}
