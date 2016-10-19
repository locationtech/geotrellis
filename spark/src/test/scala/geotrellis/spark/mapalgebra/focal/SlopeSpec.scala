package geotrellis.spark.mapalgebra.focal

import geotrellis.raster._
import geotrellis.spark._
import org.scalatest.FunSpec

class SlopeSpec extends FunSpec with TestEnvironment {

  describe("Slope Elevation Spec") {

    it("should match gdal computed slope raster") {
      val rasterOp = (tile: Tile, re: RasterExtent) => tile.slope(re.cellSize)
      val sparkOp = (rdd: TileLayerRDD[SpatialKey]) => rdd.slope()

      val path = "aspect.tif"

      testGeoTiff(sc, path)(rasterOp, sparkOp)
    }

    it("should match gdal computed slope raster (collections api)") {
      val rasterOp = (tile: Tile, re: RasterExtent) => tile.slope(re.cellSize)
      val sparkOp = (collection: TileLayerCollection[SpatialKey]) => collection.slope()

      val path = "aspect.tif"

      testGeoTiffCollection(sc, path)(rasterOp, sparkOp)
    }

  }
}
