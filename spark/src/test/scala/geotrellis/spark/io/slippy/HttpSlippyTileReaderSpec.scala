package geotrellis.spark.io.slippy

import geotrellis.spark.SpatialKey

import org.scalatest._

class HttpSlippyTileReaderTest extends FunSpec {
  describe("HttpSlippyTileReader") {
    it("should return correct urls for given zoom level") {
      val reader =
        new HttpSlippyTileReader[String]("http://tile.openstreetmap.us/vectiles-highroad/{z}/{x}/{y}.mvt")({ case (key, bytes) => key.toString })

      assert(
        reader.getURLs(2) == Seq(
          "http://tile.openstreetmap.us/vectiles-highroad/2/0/0.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/0/1.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/0/2.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/0/3.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/1/0.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/1/1.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/1/2.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/1/3.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/2/0.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/2/1.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/2/2.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/2/3.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/3/0.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/3/1.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/3/2.mvt",
          "http://tile.openstreetmap.us/vectiles-highroad/2/3/3.mvt"
        )
      )
    }
  }
}
