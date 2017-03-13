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

import geotrellis.raster._
import geotrellis.vector._

import org.geotools.data.shapefile._
import org.scalatest._

import scala.collection.JavaConverters._


class SimpleFeatureToFeatureSpec
    extends FunSpec
    with Matchers {

  val shapefiles : List[String] = List(
    "geotools/src/test/resources/timepyramid/timepyramid/2/2.shp",
    "geotools/src/test/resources/timepyramid/timepyramid/0/0.shp",
    "geotools/src/test/resources/timepyramid/timepyramid/1/1.shp",
    "geotools/src/test/resources/timepyramid/goodpyramid/2/pyramid.shp",
    "geotools/src/test/resources/timepyramid/goodpyramid/4/pyramid.shp",
    "geotools/src/test/resources/timepyramid/goodpyramid/8/pyramid.shp",
    "geotools/src/test/resources/timepyramid/goodpyramid/0/pyramid.shp",
    "geotools/src/test/resources/shapes/holeTouchEdge.shp",
    "geotools/src/test/resources/shapes/MultiPointTest.shp",
    "geotools/src/test/resources/shapes/wgs1snt.shp",
    "geotools/src/test/resources/shapes/chinese_poly.shp",
    "geotools/src/test/resources/shapes/danish_point.shp",
    "geotools/src/test/resources/shapes/roads.shp",
    "geotools/src/test/resources/shapes/rstrct.shp",
    "geotools/src/test/resources/shapes/rus-windows-1251.shp",
    "geotools/src/test/resources/shapes/archsites.shp",
    "geotools/src/test/resources/shapes/bugsites.shp",
    "geotools/src/test/resources/shapes/streams.shp",
    "geotools/src/test/resources/shapes/statepop.shp",
    "geotools/src/test/resources/shapes/polygontest.shp",
    "geotools/src/test/resources/shapes/pointtest.shp",
    "geotools/src/test/resources/shapefiles/rivers.shp",
    "geotools/src/test/resources/shapefiles/streams.shp",
    "geotools/src/test/resources/shapefiles/lakes.shp",
    "geotools/src/test/resources/shapefiles/swamps.shp",
    "geotools/src/test/resources/imagemosaic/test-data/empty_mosaic/empty_mosaic_with_caching.shp",
    "geotools/src/test/resources/imagemosaic/test-data/empty_mosaic/empty_mosaic.shp",
    "geotools/src/test/resources/imagemosaic/test-data/rgb-footprints/footprints.shp",
    "geotools/src/test/resources/imagemosaic/test-data/multiresolution/sf.shp",
    "geotools/src/test/resources/imagemosaic/test-data/crs_nztm/mosaic.shp",
    "geotools/src/test/resources/shapefile/dup-column/dup_column.shp",
    "geotools/src/test/resources/shapefile/filter-before-screenmap/filter-before-screenmap.shp",
    "geotools/src/test/resources/shapefile/sparse/sparse.shp",
    "geotools/src/test/resources/shapefile/bad/state.shp",
    "geotools/src/test/resources/shapefile/lsOnePoint/lsOnePoint.shp",
    "geotools/src/test/resources/shapefile/screenmap-deleted/screenmap-deleted.shp",
    "geotools/src/test/resources/shapefile/deleted/archsites.shp",
    "geotools/src/test/resources/shapefile/folder with spaces/pointtest.shp",
    "geotools/src/test/resources/shapefile/empty-shapefile/empty-shapefile.shp",
    "geotools/src/test/resources/goodpyramid/2/pyramid.shp",
    "geotools/src/test/resources/goodpyramid/4/pyramid.shp",
    "geotools/src/test/resources/goodpyramid/8/pyramid.shp",
    "geotools/src/test/resources/goodpyramid/0/pyramid.shp"
  )

  describe("The SimpleFeature to Geotrellis Feature Conversion") {
    it("should handle all JTS Geometry types") {
      shapefiles.foreach({ path =>
        val f = new java.io.File(path)
        val ds = new ShapefileDataStore(f.toURI.toURL)
        val fs = ds.getFeatureSource
        val fc = fs.getFeatures
        val sfi = fc.features

        while (sfi.hasNext) {
          val simpleFeature = sfi.next
          val v = SimpleFeatureToFeature(simpleFeature)

          v.data.size should be (simpleFeature.getProperties.size - 1)
          v.geom.toString should be (simpleFeature.getDefaultGeometryProperty.getValue.toString)
        }

        sfi.close
        ds.dispose
      })
    }
  }
}
