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

  def apply(simpleFeature: SimpleFeature): Feature[Geometry, Map[String, Object]] = {
    val properties = simpleFeature.getProperties.asScala
    val map = mutable.Map.empty[String, Object]
    var geometry: Geometry = null

    properties.foreach({ property =>
      property.getValue match {
        case lr: jts.LinearRing => geometry = Line(lr)
        case ls: jts.LineString => geometry = Line(ls)
        case pt: jts.Point => geometry = Point(pt)
        case pg: jts.Polygon => geometry = Polygon(pg)
        case mp: jts.MultiPoint => geometry = MultiPoint(mp)
        case ml: jts.MultiLineString => geometry = MultiLine(ml)
        case mp: jts.MultiPolygon => geometry = MultiPolygon(mp)
        case gc: jts.GeometryCollection => geometry = GeometryCollection(gc)
        case  g: jts.Geometry => throw new Exception(s"Unhandled JTS Geometry $g")
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
