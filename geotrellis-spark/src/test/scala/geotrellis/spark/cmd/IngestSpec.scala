package geotrellis.spark.cmd

import geotrellis.spark.TestEnvironment
import geotrellis.spark.utils.SparkUtils
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.Extent
import java.awt.image.DataBuffer
import geotrellis.spark.metadata.RasterMetadata
import geotrellis.spark.tiling.PixelExtent
import geotrellis.spark.tiling.TileExtent
import org.apache.hadoop.fs.Path
import geotrellis.spark.storage.RasterReader
import geotrellis.spark.tiling.TmsTiling
import geotrellis.spark.rdd.TileIdPartitioner

class IngestSpec extends TestEnvironment with MustMatchers with ShouldMatchers {

  describe("Ingest all-ones.tif") {

    // ingest the all-ones tif
    val allOnes = makeQualified(TestSourceRoot, "all-ones.tif")
    val cmd = s"--input ${allOnes} --output ${testLocalDir}"
    Ingest.main(cmd.split(' '))
    val raster = new Path(testLocalDir, "10")

    it("should create the correct metadata") {

      val expectedMeta = PyramidMetadata(
        Extent(141.7066666666667, -18.373333333333342, 142.56000000000003, -17.52000000000001),
        512,
        1,
        -9999.0,
        DataBuffer.TYPE_FLOAT,
        10,
        Map("10" -> new RasterMetadata(PixelExtent(0, 0, 1243, 1243), TileExtent(915, 203, 917, 206))))

      val actualMeta = PyramidMetadata(new Path(testLocalDir), conf)

      actualMeta should be(expectedMeta)
    }

    it("should have the right zoom level directory") {
      raster.getFileSystem(conf).exists(raster) should be(true)
    }

    it("should have the right number of splits for the base zoom level") {
      val partitioner = TileIdPartitioner(raster, conf)
      partitioner.numPartitions should be(1)
    }

    it("should have the correct tiles (checking tileIds)") {
      val meta = PyramidMetadata(new Path(testLocalDir), conf)
      val reader = RasterReader(raster, conf)
      val actualTileIds = reader.map { case (tw, aw) => tw.get }.toList
      val tileExtent = meta.metadataForBaseZoom.tileExtent
      val expectedTileIds = for {
        ty <- tileExtent.ymin to tileExtent.ymax
        tx <- tileExtent.xmin to tileExtent.xmax
      } yield TmsTiling.tileId(tx, ty, meta.maxZoomLevel)
      actualTileIds should be(expectedTileIds)
      reader.close()
    }
  }
}