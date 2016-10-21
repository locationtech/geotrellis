package geotrellis.spark.mapalgebra.focal

import geotrellis.raster._
import geotrellis.spark._
import org.scalatest.FunSpec
import geotrellis.raster.io.geotiff._
import java.io._

class SlopeSpec extends FunSpec with TestEnvironment {

  describe("Slope Elevation Spec") {

    it("should match gdal computed slope raster") {
      val rasterOp = (tile: Tile, re: RasterExtent) => tile.slope(re.cellSize)
      val sparkOp = (rdd: TileLayerRDD[SpatialKey]) => rdd.slope()

      val path = "aspect.tif"

      testGeoTiff(sc, path)(rasterOp, sparkOp)
    }

    it("should update RDD cellType of DoubleConstantNoDataCellType") {
      val tile = SinglebandGeoTiff(new File(inputHomeLocalPath, "aspect.tif").getPath).tile

      val (_, rasterRDD) = createTileLayerRDD(tile, 4, 3)
      val slopeRDD = rasterRDD.slope()
      slopeRDD.metadata.cellType should be (DoubleConstantNoDataCellType)
      slopeRDD.collect.head._2.cellType should be (DoubleConstantNoDataCellType)
    }

    it("should match gdal computed slope raster (collections api)") {
      val rasterOp = (tile: Tile, re: RasterExtent) => tile.slope(re.cellSize)
      val sparkOp = (collection: TileLayerCollection[SpatialKey]) => collection.slope()

      val path = "aspect.tif"

      testGeoTiffCollection(sc, path)(rasterOp, sparkOp)
    }

  }
}
