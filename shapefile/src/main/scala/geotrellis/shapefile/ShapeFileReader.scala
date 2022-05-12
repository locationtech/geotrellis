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

package geotrellis.shapefile

import geotrellis.vector._

import org.geotools.data.simple._
import org.opengis.feature.simple._
import org.geotools.data.shapefile._

import java.net.URL
import java.io.File
import java.nio.charset.Charset

import scala.collection.mutable
import scala.collection.JavaConverters._

object ShapeFileReader {
  val DEFAULT_CHARSET = Charset.forName("ISO-8859-1")
  implicit class SimpleFeatureWrapper(ft: SimpleFeature) {
    def geom[G <: Geometry: Manifest]: Option[G] =
      ft.getAttribute(0) match {
        case g: G => Some(g)
        case _ => None
      }

    def attributeMap: Map[String, Object] =
      ft.getProperties.asScala.drop(1).map { p =>
        (p.getName.toString, ft.getAttribute(p.getName))
      }.toMap

    def attribute[D](name: String): D =
      ft.getAttribute(name).asInstanceOf[D]
  }

  // TODO: use default argument instead of overloads in the next major release
  def readSimpleFeatures(path: String): Seq[SimpleFeature] = readSimpleFeatures(new URL(s"file://${new File(path).getAbsolutePath}"), DEFAULT_CHARSET)
  def readSimpleFeatures(path: String, charSet: Charset): Seq[SimpleFeature] = readSimpleFeatures(new URL(s"file://${new File(path).getAbsolutePath}"), charSet)

  def readSimpleFeatures(url: URL): Seq[SimpleFeature] = readSimpleFeatures(url, DEFAULT_CHARSET)
  def readSimpleFeatures(url: URL, charSet: Charset): Seq[SimpleFeature] = {
    // Extract the features as GeoTools 'SimpleFeatures'
    val ds = new ShapefileDataStore(url)
    ds.setCharset(charSet)
    val ftItr: SimpleFeatureIterator = ds.getFeatureSource.getFeatures.features

    try {
      val simpleFeatures = mutable.ListBuffer[SimpleFeature]()
      while(ftItr.hasNext) simpleFeatures += ftItr.next()
      simpleFeatures.toList
    } finally {
      ftItr.close
      ds.dispose
    }
  }

  def readPointFeatures(path: String): Seq[PointFeature[Map[String,Object]]] = readPointFeatures(path, DEFAULT_CHARSET)
  def readPointFeatures(path: String, charSet: Charset): Seq[PointFeature[Map[String,Object]]] =
    readSimpleFeatures(path, charSet)
      .flatMap { ft => ft.geom[Point].map(PointFeature(_, ft.attributeMap)) }

  def readPointFeatures[D](path: String, dataField: String): Seq[PointFeature[D]] = readPointFeatures(path, dataField, DEFAULT_CHARSET)
  def readPointFeatures[D](path: String, dataField: String, charSet: Charset): Seq[PointFeature[D]] =
    readSimpleFeatures(path, charSet)
      .flatMap { ft => ft.geom[Point].map(PointFeature(_, ft.attribute[D](dataField))) }

  def readPointFeatures(url: URL): Seq[PointFeature[Map[String,Object]]] = readPointFeatures(url, DEFAULT_CHARSET)
  def readPointFeatures(url: URL, charSet: Charset): Seq[PointFeature[Map[String,Object]]] =
    readSimpleFeatures(url, charSet)
      .flatMap { ft => ft.geom[Point].map(PointFeature(_, ft.attributeMap)) }

  def readPointFeatures[D](url: URL, dataField: String): Seq[PointFeature[D]] = readPointFeatures(url, dataField, DEFAULT_CHARSET)
  def readPointFeatures[D](url: URL, dataField: String, charSet: Charset): Seq[PointFeature[D]] =
    readSimpleFeatures(url, charSet)
      .flatMap { ft => ft.geom[Point].map(PointFeature(_, ft.attribute[D](dataField))) }

  def readLineFeatures(path: String): Seq[Feature[LineString, Map[String,Object]]] = readLineFeatures(path, DEFAULT_CHARSET)
  def readLineFeatures(path: String, charSet: Charset): Seq[Feature[LineString, Map[String,Object]]] =
    readSimpleFeatures(path, charSet)
      .flatMap { ft => ft.geom[LineString].map(Feature(_, ft.attributeMap)) }

  def readLineFeatures[D](path: String, dataField: String): Seq[Feature[LineString, D]] = readLineFeatures(path, dataField, DEFAULT_CHARSET)
  def readLineFeatures[D](path: String, dataField: String, charSet: Charset): Seq[Feature[LineString, D]] =
    readSimpleFeatures(path, charSet)
      .flatMap { ft => ft.geom[LineString].map(Feature(_, ft.attribute[D](dataField))) }

  def readLineFeatures(url: URL): Seq[Feature[LineString, Map[String,Object]]] = readLineFeatures(url, DEFAULT_CHARSET)
  def readLineFeatures(url: URL, charSet: Charset): Seq[Feature[LineString, Map[String,Object]]] =
    readSimpleFeatures(url, charSet)
      .flatMap { ft => ft.geom[LineString].map(Feature(_, ft.attributeMap)) }

  def readLineFeatures[D](url: URL, dataField: String): Seq[Feature[LineString, D]] = readLineFeatures(url, dataField, DEFAULT_CHARSET)
  def readLineFeatures[D](url: URL, dataField: String, charSet: Charset): Seq[Feature[LineString, D]] =
    readSimpleFeatures(url, charSet)
      .flatMap { ft => ft.geom[LineString].map(Feature(_, ft.attribute[D](dataField))) }

  def readPolygonFeatures(path: String): Seq[PolygonFeature[Map[String,Object]]] = readPolygonFeatures(path, DEFAULT_CHARSET)
  def readPolygonFeatures(path: String, charSet: Charset): Seq[PolygonFeature[Map[String,Object]]] =
    readSimpleFeatures(path, charSet)
      .flatMap { ft => ft.geom[Polygon].map(PolygonFeature(_, ft.attributeMap)) }

  def readPolygonFeatures[D](path: String, dataField: String): Seq[PolygonFeature[D]] = readPolygonFeatures(path, dataField, DEFAULT_CHARSET)
  def readPolygonFeatures[D](path: String, dataField: String, charSet: Charset): Seq[PolygonFeature[D]] =
    readSimpleFeatures(path, charSet)
      .flatMap { ft => ft.geom[Polygon].map(PolygonFeature(_, ft.attribute[D](dataField))) }

  def readPolygonFeatures(url: URL): Seq[PolygonFeature[Map[String,Object]]] = readPolygonFeatures(url, DEFAULT_CHARSET)
  def readPolygonFeatures(url: URL, charSet: Charset): Seq[PolygonFeature[Map[String,Object]]] =
    readSimpleFeatures(url, charSet)
      .flatMap { ft => ft.geom[Polygon].map(PolygonFeature(_, ft.attributeMap)) }

  def readPolygonFeatures[D](url: URL, dataField: String): Seq[PolygonFeature[D]] = readPolygonFeatures(url, dataField, DEFAULT_CHARSET)
  def readPolygonFeatures[D](url: URL, dataField: String, charSet: Charset): Seq[PolygonFeature[D]] =
    readSimpleFeatures(url, charSet)
      .flatMap { ft => ft.geom[Polygon].map(PolygonFeature(_, ft.attribute[D](dataField))) }

  def readMultiPointFeatures(path: String): Seq[MultiPointFeature[Map[String,Object]]] = readMultiPointFeatures(path, DEFAULT_CHARSET)
  def readMultiPointFeatures(path: String, charSet: Charset): Seq[MultiPointFeature[Map[String,Object]]] =
    readSimpleFeatures(path, charSet)
      .flatMap { ft => ft.geom[MultiPoint].map(MultiPointFeature(_, ft.attributeMap)) }

  def readMultiPointFeatures[D](path: String, dataField: String): Seq[MultiPointFeature[D]] = readMultiPointFeatures(path, dataField, DEFAULT_CHARSET)
  def readMultiPointFeatures[D](path: String, dataField: String, charSet: Charset): Seq[MultiPointFeature[D]] =
    readSimpleFeatures(path, charSet)
      .flatMap { ft => ft.geom[MultiPoint].map(MultiPointFeature(_, ft.attribute[D](dataField))) }

  def readMultiPointFeatures(url: URL): Seq[MultiPointFeature[Map[String,Object]]] = readMultiPointFeatures(url, DEFAULT_CHARSET)
  def readMultiPointFeatures(url: URL, charSet: Charset): Seq[MultiPointFeature[Map[String,Object]]] =
    readSimpleFeatures(url, charSet)
      .flatMap { ft => ft.geom[MultiPoint].map(MultiPointFeature(_, ft.attributeMap)) }

  def readMultiPointFeatures[D](url: URL, dataField: String): Seq[MultiPointFeature[D]] = readMultiPointFeatures(url, dataField, DEFAULT_CHARSET)
  def readMultiPointFeatures[D](url: URL, dataField: String, charSet: Charset): Seq[MultiPointFeature[D]] =
    readSimpleFeatures(url, charSet)
      .flatMap { ft => ft.geom[MultiPoint].map(MultiPointFeature(_, ft.attribute[D](dataField))) }

  def readMultiLineFeatures(path: String): Seq[Feature[MultiLineString, Map[String,Object]]] = readMultiLineFeatures(path, DEFAULT_CHARSET)
  def readMultiLineFeatures(path: String, charSet: Charset): Seq[Feature[MultiLineString, Map[String,Object]]] =
    readSimpleFeatures(path, charSet)
      .flatMap { ft => ft.geom[MultiLineString].map(Feature(_, ft.attributeMap)) }

  def readMultiLineFeatures[D](path: String, dataField: String): Seq[Feature[MultiLineString, D]] = readMultiLineFeatures(path, dataField, DEFAULT_CHARSET)
  def readMultiLineFeatures[D](path: String, dataField: String, charSet: Charset): Seq[Feature[MultiLineString, D]] =
    readSimpleFeatures(path, charSet)
      .flatMap { ft => ft.geom[MultiLineString].map(Feature(_, ft.attribute[D](dataField))) }

  def readMultiLineFeatures(url: URL): Seq[Feature[MultiLineString, Map[String,Object]]] = readMultiLineFeatures(url, DEFAULT_CHARSET)
  def readMultiLineFeatures(url: URL, charSet: Charset): Seq[Feature[MultiLineString, Map[String,Object]]] =
    readSimpleFeatures(url, charSet)
      .flatMap { ft => ft.geom[MultiLineString].map(Feature(_, ft.attributeMap)) }

  def readMultiLineFeatures[D](url: URL, dataField: String): Seq[Feature[MultiLineString, D]] = readMultiLineFeatures(url, dataField, DEFAULT_CHARSET)
  def readMultiLineFeatures[D](url: URL, dataField: String, charSet: Charset): Seq[Feature[MultiLineString, D]] =
    readSimpleFeatures(url, charSet)
      .flatMap { ft => ft.geom[MultiLineString].map(Feature(_, ft.attribute[D](dataField))) }

  def readMultiPolygonFeatures(path: String): Seq[MultiPolygonFeature[Map[String,Object]]] = readMultiPolygonFeatures(path, DEFAULT_CHARSET)
  def readMultiPolygonFeatures(path: String, charSet: Charset): Seq[MultiPolygonFeature[Map[String,Object]]] =
    readSimpleFeatures(path, charSet)
      .flatMap { ft => ft.geom[MultiPolygon].map(MultiPolygonFeature(_, ft.attributeMap)) }

  def readMultiPolygonFeatures[D](path: String, dataField: String): Seq[MultiPolygonFeature[D]] = readMultiPolygonFeatures(path, dataField, DEFAULT_CHARSET)
  def readMultiPolygonFeatures[D](path: String, dataField: String, charSet: Charset): Seq[MultiPolygonFeature[D]] =
    readSimpleFeatures(path, charSet)
      .flatMap { ft => ft.geom[MultiPolygon].map(MultiPolygonFeature(_, ft.attribute[D](dataField))) }

  def readMultiPolygonFeatures(url: URL): Seq[MultiPolygonFeature[Map[String,Object]]] = readMultiPolygonFeatures(url, DEFAULT_CHARSET)
  def readMultiPolygonFeatures(url: URL, charSet: Charset): Seq[MultiPolygonFeature[Map[String,Object]]] =
    readSimpleFeatures(url, charSet)
      .flatMap { ft => ft.geom[MultiPolygon].map(MultiPolygonFeature(_, ft.attributeMap)) }

  def readMultiPolygonFeatures[D](url: URL, dataField: String): Seq[MultiPolygonFeature[D]] = readMultiPolygonFeatures(url, dataField, DEFAULT_CHARSET)
  def readMultiPolygonFeatures[D](url: URL, dataField: String, charSet: Charset): Seq[MultiPolygonFeature[D]] =
    readSimpleFeatures(url, charSet)
      .flatMap { ft => ft.geom[MultiPolygon].map(MultiPolygonFeature(_, ft.attribute[D](dataField))) }
}
