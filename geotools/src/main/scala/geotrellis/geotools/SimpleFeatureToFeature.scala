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
import org.opengis.feature.simple.SimpleFeature

import scala.collection._
import scala.collection.JavaConverters._
import scala.reflect.ClassTag

object SimpleFeatureToFeature {

  def apply[G <: Geometry : ClassTag](simpleFeature: SimpleFeature): Feature[G, immutable.Map[String, AnyRef]] = {
    val properties = simpleFeature.getProperties.asScala
    val map = mutable.Map.empty[String, AnyRef]
    val defaultGeom = simpleFeature.getDefaultGeometry
    var geometry: Option[G] = if (defaultGeom != null) Some(defaultGeom.asInstanceOf[G]) else None

    properties.foreach({ property =>
      (defaultGeom, property.getValue) match {
        case (null, g: Geometry) => geometry = Some(g.asInstanceOf[G])
        case (g1: Geometry, g2: Geometry) =>
          val key = property.getName.toString
          val value = g2.asInstanceOf[G]
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
