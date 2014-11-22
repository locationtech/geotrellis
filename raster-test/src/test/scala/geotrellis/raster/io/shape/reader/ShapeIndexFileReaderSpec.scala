package geotrellis.raster.io.shape.reader

import org.scalatest._

/**
  * Tests reading .shx files.
  * TODO: Will we ever care about the index files? Will we ever query from FS?
  * If not, let's scrap this and just check that the .shx file is there, to not
  * waste any time.
  */
class ShapeIndexFileReaderSpec extends FunSpec with Matchers {

  describe("should read shape index files correctly") {

    ignore("should read demographics.shx correctly") { }

    ignore("should read countries.shx correctly") { }

  }

}
