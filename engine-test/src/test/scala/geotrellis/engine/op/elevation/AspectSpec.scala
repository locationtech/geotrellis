package geotrellis.engine.op.elevation


import geotrellis.engine.TestEngine
import geotrellis.engine._
import geotrellis.raster._
import geotrellis.raster.op.elevation._
import geotrellis.vector.Extent
import org.scalatest._

class AspectSpec extends FunSpec with Matchers with TestEngine {
  describe("Aspect") {
    it("should match gdal computed aspect raster") {
      val rasterExtentElevation = RasterSource(LayerId("test:fs", "elevation")).rasterExtent.get

      //Use the testkit to resolve everything to bare Tiles
      val r: Tile = get(getRaster("elevation"))
      val rg: Tile = get(getRaster("aspect"))

      val aspectComputed = r.aspect(rasterExtentElevation.cellSize)

      val rasterExtent = RasterSource("aspect").rasterExtent.get

      // Gdal actually computes the parimeter values differently.
      // So take out the edge results
      val (xmin, ymax) = rasterExtent.gridToMap(1, 1)
      val (xmax, ymin) = rasterExtent.gridToMap(rg.cols - 2, rg.rows - 2)

      val cropExtent = Extent(xmin, ymin, xmax, ymax)

      val rgc = rg.convert(TypeDouble).crop(rasterExtent.extent, cropExtent)
      val rc = get(aspectComputed).crop(rasterExtentElevation.extent, cropExtent)

      assertEqual(rgc, rc, 0.1)
    }
  }
}
