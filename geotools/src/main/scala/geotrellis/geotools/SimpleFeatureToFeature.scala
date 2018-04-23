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

package geotrellis.geotools

import geotrellis.vector._

import org.locationtech.jts.{geom => jts}
import org.opengis.feature.simple.SimpleFeature

import scala.collection._
import scala.collection.JavaConverters._
import scala.reflect.ClassTag


object SimpleFeatureToFeature {

  private def jtsToGeotrellis[G <: Geometry : ClassTag](geometry: Object): Option[G] = {
    geometry match {
      case g: jts.Geometry => Some(Geometry(g).asInstanceOf[G])
      case g => throw new Exception(s"Input $g is not a jts.Geometry")
    }
  }

  def apply[G <: Geometry : ClassTag](simpleFeature: SimpleFeature): Feature[G, immutable.Map[String, AnyRef]] = {
    val properties = simpleFeature.getProperties.asScala
    val map = mutable.Map.empty[String, AnyRef]
    val defaultGeom = simpleFeature.getDefaultGeometry
    var geometry = if (defaultGeom != null) jtsToGeotrellis(defaultGeom); else None

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
    Feature(geometry.get, map.toMap)
  }
}
