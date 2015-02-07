package geotrellis.geotools

import geotrellis.vector._

import org.scalatest._

class ShapeFileReaderSpec extends FunSpec with Matchers {
  describe("ShapeFileReader") {
    it("should read multipolygons feature attribute") {
      val path = "raster-test/data/shapefiles/countries/countries.shp"
      val features = ShapeFileReader.readMultiPolygonFeatures(path)
      //features.size should be (160)
      var i = 0
      for(MultiPolygonFeature(mp, data) <- features) {
        if (i == 7) println(mp.polygons.map(_.exterior).last)
        i += 1
        //print(mp.polygons.map(p => p.holes :+ p.exterior).flatten.map(_.hashCode).toSet.hashCode + ", ")
      }
    }
  }
}
