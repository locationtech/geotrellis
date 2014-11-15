package geotrellis.raster.io.shape.reader

import geotrellis.testkit._

import org.scalatest._

/**
  * Tests reading .dbf files.
  */
class ShapeDBaseFileReaderSpec extends FunSpec
    with Matchers
    with BeforeAndAfterAll
    with TestEngine {

  def read(path: String) = ShapeDBaseFileReader(path).read

  describe("should read shape DBase files correctly") {

    it("should read demographics.dbf correctly") {
      val path = "raster-test/data/shapefiles/demographics/demographics.dbf"
      val dBaseFile = read(path)
      dBaseFile.records.foreach(v => v match {
        case Some(x) => println("VAL" + x + "ENDVAL")
        case None => println("NUON")
      })

      dBaseFile.size should be (160)
    }
  }
}
