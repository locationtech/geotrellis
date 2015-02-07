package geotrellis.vector.io.shape.reader

import org.scalatest._

/**
  * Tests reading .cpg files.
  */
class CodePageFileReaderSpec extends FunSpec with Matchers {

  val BasePath = "raster-test/data/shapefiles/codepagefiles/"

  describe("should read .cpg files correctly") {

    it("should read an us ascii encoding correctly") {
      CodePageFileReader(BasePath + "us-ascii").read.code should be ("US-ASCII")
    }

    it("should read a latin encoding correctly") {
      CodePageFileReader(BasePath + "iso-8859-1").read.code should be ("US-ASCII")
    }

    it("should read a UTF-8 encoding correctly") {
      CodePageFileReader(BasePath + "utf-8").read.code should be ("US-ASCII")
    }

    it("should read a UTF-16BE encoding correctly") {
      CodePageFileReader(BasePath + "utf-16be").read.code should be ("US-ASCII")
    }

    it("should read a UTF-16LE encoding correctly") {
      CodePageFileReader(BasePath + "utf-16le").read.code should be ("US-ASCII")
    }

    it("should read a UTF-16 encoding correctly") {
      CodePageFileReader(BasePath + "utf-16").read.code should be ("US-ASCII")
    }

    it("should read a bogus encoding and return the default ASCII encoding") {
      CodePageFileReader(BasePath + "mumbojumbo").read.code should be ("US-ASCII")
    }

  }

}
