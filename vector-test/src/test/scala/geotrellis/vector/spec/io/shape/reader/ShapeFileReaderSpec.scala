package geotrellis.vector.io.shape.reader

import org.scalatest._

/**
  * Tests reading shape files.
  */
class ShapeFileReaderSpec extends FunSpec with Matchers {

  describe("should read shape files correctly") {

    def read(path: String) = ShapeFileReader(path).read

    it("should read the demographics shape file correctly") {
      val shapeFile = read("raster-test/data/shapefiles/demographics/demographics")

      shapeFile.size should be(160)
    }

    it("should read the countries shape file correctly") {
      val shapeFile = read("raster-test/data/shapefiles/countries/countries")
      shapeFile.size should be(255)
    }
  }
}
