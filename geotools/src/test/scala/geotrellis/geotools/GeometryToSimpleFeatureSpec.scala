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

import geotrellis.proj4.{LatLng, WebMercator}
import geotrellis.raster._
import geotrellis.vector._

import org.geotools.data.shapefile._
import org.scalatest._

import scala.collection.JavaConverters._


class GeometryToSimpleFeatureSpec
    extends FunSpec
    with Matchers {

  describe("The Geometry to SimpleFeature Conversion") {

    it("should handle Points") {
      val geom = Point(0, 1)
      val simpleFeature = GeometryToSimpleFeature(geom, None, List(("count", 42)))
      simpleFeature.getDefaultGeometryProperty.getValue should be (geom.jtsGeom)
    }

    it("should handle Lines") {
      val geom = Line(Point(0, 0), Point(3, 3))
      val simpleFeature = GeometryToSimpleFeature(geom, None, List(("count", 42)))
      simpleFeature.getDefaultGeometryProperty.getValue should be (geom.jtsGeom)
    }

    it("should handle Polygons") {
      val geom = Polygon(Point(0, 0), Point(4, 0), Point(0, 3), Point(0, 0))
      val simpleFeature = GeometryToSimpleFeature(geom, None, List(("count", 42)))
      simpleFeature.getDefaultGeometryProperty.getValue should be (geom.jtsGeom)
    }

    it("should handle MultiPoints") {
      val geom = MultiPoint(Point(0, 0), Point(4, 0), Point(0, 3), Point(0, 0))
      val simpleFeature = GeometryToSimpleFeature(geom, None, List(("count", 42)))
      simpleFeature.getDefaultGeometryProperty.getValue should be (geom.jtsGeom)
    }

    it("should handle MultiLines") {
      val geom = MultiLine(Line(Point(0, 0), Point(4, 0)), Line(Point(0, 3), Point(0, 0)))
      val simpleFeature = GeometryToSimpleFeature(geom, None, List(("count", 42)))
      simpleFeature.getDefaultGeometryProperty.getValue should be (geom.jtsGeom)
    }

    it("should handle MultiPolygons") {
      val geom = MultiPolygon(
        Polygon(Point(0, 0), Point(4, 0), Point(0, 3), Point(0, 0)),
        Polygon(Point(0, 0), Point(5, 0), Point(0, 12), Point(0, 0))
      )
      val simpleFeature = GeometryToSimpleFeature(geom, None, List(("count", 42)))
      simpleFeature.getDefaultGeometryProperty.getValue should be (geom.jtsGeom)
    }

    it("should properly encode the CRS") {
      val simpleFeature1 = GeometryToSimpleFeature(Point(0, 0), Some(LatLng), List(("count", 42)))
      simpleFeature1.getType.getCoordinateReferenceSystem.toString.contains("4326") should be (true)

      val simpleFeature2 = GeometryToSimpleFeature(Point(0, 0), Some(WebMercator), List(("count", 42)))
      simpleFeature2.getType.getCoordinateReferenceSystem.toString.contains("3857") should be (true)

      val simpleFeature3 = GeometryToSimpleFeature(Point(0, 0), None, List(("count", 42)))
      simpleFeature3.getType.getCoordinateReferenceSystem should be (null)
    }

    it("should properly store additional properties") {
      val o = new Object
      val simpleFeature = GeometryToSimpleFeature(
        Point(0, 0),
        None,
        List(("integer", 42), ("double", 85.0), ("object", o), ("class", classOf[Object]))
      )
      val properties = simpleFeature.getProperties

      properties.size should be (5)
      simpleFeature.getProperty("integer").getValue should be (42)
      simpleFeature.getProperty("double").getValue should be (85.0)
      simpleFeature.getProperty("object").getValue should be (o)
      simpleFeature.getProperty("class").getValue should be (classOf[Object])
    }
  }
}
