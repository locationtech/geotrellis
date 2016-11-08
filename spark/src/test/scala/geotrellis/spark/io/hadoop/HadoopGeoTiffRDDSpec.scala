package geotrellis.spark.io.hadoop

import geotrellis.raster._
import geotrellis.raster.testkit.RasterMatchers
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.testfiles._
import geotrellis.spark.io.hadoop.formats._

import org.apache.hadoop.fs.Path

import spire.syntax.cfor._
import org.scalatest._

class HadoopGeoTiffRDDSpec
  extends FunSpec
    with Matchers
    with RasterMatchers
    with TestEnvironment
    with TestFiles {
  describe("HadoopGeoTiffRDD") {

    it("should read the same rasters when reading small windows or with no windows, Spatial, SinglebandGeoTiff") {
      val tilesDir = new Path(localFS.getWorkingDirectory, "raster-test/data/one-month-tiles/")
      val source1 = HadoopGeoTiffRDD.spatial(tilesDir)
      val source2 = HadoopGeoTiffRDD.spatial(tilesDir, HadoopGeoTiffRDD.Options(maxTileSize = Some(128)))

      val (_, md) = source1.collectMetadata[SpatialKey](FloatingLayoutScheme(256))

      val stitched1 = source1.tileToLayout(md).stitch
      val stitched2 = source2.tileToLayout(md).stitch

      assertEqual(stitched1, stitched2)
    }
    
    it("should read the same rasters when reading small windows or with no windows, Spatial, MultibandGeoTiff") {
      val tilesDir = new Path(localFS.getWorkingDirectory, "raster-test/data/geotiff-test-files/3bands/byte/")
      val source1 = HadoopGeoTiffRDD.spatialMultiband(tilesDir)
      val source2 = HadoopGeoTiffRDD.spatialMultiband(tilesDir, HadoopGeoTiffRDD.Options(maxTileSize = Some(128)))

      val (_, md) = source1.collectMetadata[SpatialKey](FloatingLayoutScheme(256))

      val stitched1 = source1.tileToLayout(md).stitch
      val stitched2 = source2.tileToLayout(md).stitch

      assertEqual(stitched1, stitched2)
    }

    it("should read the same rasters when reading small windows or with no windows, Temporal, SinglebandGeoTiff") {
      val tilesDir = new Path(localFS.getWorkingDirectory, "raster-test/data/one-month-tiles/")

      val source1 = HadoopGeoTiffRDD.temporal(tilesDir, HadoopGeoTiffRDD.Options(
        timeTag = "ISO_TIME",
        timeFormat = "yyyy-MM-dd'T'HH:mm:ss"))

      val source2 = HadoopGeoTiffRDD.temporal(tilesDir, HadoopGeoTiffRDD.Options(
        timeTag = "ISO_TIME",
        timeFormat = "yyyy-MM-dd'T'HH:mm:ss",
        maxTileSize = Some(128)))
      
      val (wholeInfo, _) = source1.first()
      val dateTime = wholeInfo.time

      val collection = source2.collect
      
      cfor(0)(_ < source2.count, _ + 1){ i =>
        val (info, _) = collection(i)

        info.time should be (dateTime)
      }
    }

    it("should read the same rasters when reading small windows or with no windows, Temporal, MultibandGeoTiff") {
      val tilesDir = new Path(localFS.getWorkingDirectory, "raster-test/data/one-month-tiles-multiband")

      val source1 = HadoopGeoTiffRDD.temporalMultiband(tilesDir, HadoopGeoTiffRDD.Options(
        timeTag = "ISO_TIME",
        timeFormat = "yyyy-MM-dd'T'HH:mm:ss"))

      val source2 = HadoopGeoTiffRDD.temporalMultiband(tilesDir, HadoopGeoTiffRDD.Options(
        timeTag = "ISO_TIME",
        timeFormat = "yyyy-MM-dd'T'HH:mm:ss",
        maxTileSize = Some(128)))
      
      val (wholeInfo, _) = source1.first()
      val dateTime = wholeInfo.time

      val collection = source2.collect
      
      cfor(0)(_ < source2.count, _ + 1){ i =>
        val (info, _) = collection(i)

        info.time should be (dateTime)
      }

    }
  }
}
