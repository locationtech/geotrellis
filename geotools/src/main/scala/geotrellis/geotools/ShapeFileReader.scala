/*
 * Copyright (c) 2014 Azavea.
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

import org.geotools.data.simple._
import org.opengis.feature.simple._
import org.geotools.data.shapefile._
import com.vividsolutions.jts.{geom => jts}

import java.net.URL
import java.io.File

import scala.collection.mutable
import scala.collection.JavaConversions._

object ShapeFileReader {
  implicit class SimpleFeatureWrapper(ft: SimpleFeature) {
    def geom[G <: jts.Geometry: Manifest]: Option[G] =
      ft.getAttribute(0) match {
        case g: G => Some(g)
        case _ => None
      }

    def attributeMap: Map[String, Object] =
      ft.getProperties.drop(1).map { p =>
        (p.getName.toString, ft.getAttribute(p.getName))
      }.toMap

    def attribute[D](name: String): D =
      ft.getAttribute(name).asInstanceOf[D]
  }

  def readSimpleFeatures(path: String) = {
    // Extract the features as GeoTools 'SimpleFeatures'
    val url = s"file://${new File(path).getAbsolutePath}"
    val ftItr: SimpleFeatureIterator =
      new ShapefileDataStore(new URL(url))
        .getFeatureSource
        .getFeatures
        .features

    val simpleFeatures = mutable.ListBuffer[SimpleFeature]()
    while(ftItr.hasNext()) simpleFeatures += ftItr.next()
    simpleFeatures.toList
  }

  def readPointFeatures(path: String): Seq[PointFeature[Map[String,Object]]] =
    readSimpleFeatures(path)
      .map { ft => ft.geom[jts.Point].map(PointFeature(_, ft.attributeMap)) }
      .flatten

  def readPointFeatures[D](path: String, dataField: String): Seq[PointFeature[D]] =
    readSimpleFeatures(path)
      .map { ft => ft.geom[jts.Point].map(PointFeature(_, ft.attribute[D](dataField))) }
      .flatten

  def readLineFeatures(path: String): Seq[LineFeature[Map[String,Object]]] =
    readSimpleFeatures(path)
      .map { ft => ft.geom[jts.LineString].map(LineFeature(_, ft.attributeMap)) }
      .flatten

  def readLineFeatures[D](path: String, dataField: String): Seq[LineFeature[D]] =
    readSimpleFeatures(path)
      .map { ft => ft.geom[jts.LineString].map(LineFeature(_, ft.attribute[D](dataField))) }
      .flatten

  def readPolygonFeatures(path: String): Seq[PolygonFeature[Map[String,Object]]] =
    readSimpleFeatures(path)
      .map { ft => ft.geom[jts.Polygon].map(PolygonFeature(_, ft.attributeMap)) }
      .flatten

  def readPolygonFeatures[D](path: String, dataField: String): Seq[PolygonFeature[D]] =
    readSimpleFeatures(path)
      .map { ft => ft.geom[jts.Polygon].map(PolygonFeature(_, ft.attribute[D](dataField))) }
      .flatten

  def readMultiPointFeatures(path: String): Seq[MultiPointFeature[Map[String,Object]]] =
    readSimpleFeatures(path)
      .map { ft => ft.geom[jts.MultiPoint].map(MultiPointFeature(_, ft.attributeMap)) }
      .flatten

  def readMultiPointFeatures[D](path: String, dataField: String): Seq[MultiPointFeature[D]] =
    readSimpleFeatures(path)
      .map { ft => ft.geom[jts.MultiPoint].map(MultiPointFeature(_, ft.attribute[D](dataField))) }
      .flatten

  def readMultiLineFeatures(path: String): Seq[MultiLineFeature[Map[String,Object]]] =
    readSimpleFeatures(path)
      .map { ft => ft.geom[jts.MultiLineString].map(MultiLineFeature(_, ft.attributeMap)) }
      .flatten

  def readMultiLineFeatures[D](path: String, dataField: String): Seq[MultiLineFeature[D]] =
    readSimpleFeatures(path)
      .map { ft => ft.geom[jts.MultiLineString].map(MultiLineFeature(_, ft.attribute[D](dataField))) }
      .flatten

  def readMultiPolygonFeatures(path: String): Seq[MultiPolygonFeature[Map[String,Object]]] =
    readSimpleFeatures(path)
      .map { ft => ft.geom[jts.MultiPolygon].map(MultiPolygonFeature(_, ft.attributeMap)) }
      .flatten

  def readMultiPolygonFeatures[D](path: String, dataField: String): Seq[MultiPolygonFeature[D]] =
    readSimpleFeatures(path)
      .map { ft => ft.geom[jts.MultiPolygon].map(MultiPolygonFeature(_, ft.attribute[D](dataField))) }
      .flatten
}
