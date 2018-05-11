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
import geotrellis.proj4.CRS

import com.vividsolutions.jts.{geom => jts}
import org.geotools.feature.simple.{SimpleFeatureTypeBuilder, SimpleFeatureBuilder}
import org.opengis.feature.simple.{SimpleFeatureType, SimpleFeature}

import scala.collection.JavaConverters._


object GeometryToSimpleFeature {
  val geometryField = "the_geom"

  /**
    * Given a Geotrellis geometry, a CRS, and a sequence of ancillary
    * data, produce a GeoTools SimpleFeature.
    *
    * @param  geom       The Geotrellis geometry
    * @param  crs        The CRS of the geometry
    * @param  data       A sequence of (String, Any) pairs
    * @param  featureId  A identifier for the output simple feature (null for a randomly generated id)
    */
  def apply(geom: Geometry, crs: Option[CRS], data: Seq[(String, Any)], featureId: String = null): SimpleFeature = {
    val sftb = (new SimpleFeatureTypeBuilder).minOccurs(1).maxOccurs(1).nillable(false)

    sftb.setName("Bespoke Type")
    crs match {
      case Some(crs) => sftb.setSRS(s"EPSG:${crs.epsgCode.get}")
      case None =>
    }
    geom match {
      case pt: Point => sftb.add(geometryField, classOf[jts.Point])
      case ln: Line => sftb.add(geometryField, classOf[jts.LineString])
      case pg: Polygon => sftb.add(geometryField, classOf[jts.Polygon])
      case mp: MultiPoint => sftb.add(geometryField, classOf[jts.MultiPoint])
      case ml: MultiLine => sftb.add(geometryField, classOf[jts.MultiLineString])
      case mp: MultiPolygon => sftb.add(geometryField, classOf[jts.MultiPolygon])
      case  g: Geometry => throw new Exception(s"Unhandled Geotrellis Geometry $g")
    }
    sftb.setDefaultGeometry(geometryField)
    data.foreach({ case (key, value) => sftb
      .minOccurs(1).maxOccurs(1).nillable(false)
      .add(key, value.getClass)
    })

    val sft = sftb.buildFeatureType
    val sfb = new SimpleFeatureBuilder(sft)

    geom match {
      case Point(pt) => sfb.add(pt)
      case Line(ln) => sfb.add(ln)
      case Polygon(pg) => sfb.add(pg)
      case MultiPoint(mp) => sfb.add(mp)
      case MultiLine(ml) => sfb.add(ml)
      case MultiPolygon(mp) => sfb.add(mp)
      case g: Geometry => throw new Exception(s"Unhandled Geotrellis Geometry $g")
    }
    data.foreach({ case (key, value) => sfb.add(value) })

    sfb.buildFeature(featureId)
  }
}
