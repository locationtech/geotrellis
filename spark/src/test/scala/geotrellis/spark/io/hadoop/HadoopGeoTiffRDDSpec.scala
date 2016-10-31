package geotrellis.spark.io.hadoop

import geotrellis.raster._
import geotrellis.raster.testkit.RasterMatchers
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.testfiles._

import org.apache.hadoop.fs.Path
import org.scalatest._

class HadoopGeoTiffRDDSpec
  extends FunSpec
    with Matchers
    with RasterMatchers
    with TestEnvironment
    with TestFiles {
  describe("HadoopGeoTiffRDD") {
    it("should read the same rasters when reading small windows or with no windows") {
      val tilesDir = new Path(localFS.getWorkingDirectory, "raster-test/data/one-month-tiles/")
      val source1 = HadoopGeoTiffRDD.spatial(tilesDir)
      val source2 = HadoopGeoTiffRDD.spatial(tilesDir, HadoopGeoTiffRDD.Options(maxTileSize = Some(128)))

      val (_, md) = source1.collectMetadata[SpatialKey](FloatingLayoutScheme(256))

      val stitched1 = source1.tileToLayout(md).stitch
      val stitched2 = source2.tileToLayout(md).stitch

      assertEqual(stitched1, stitched2)
    }
  }
}
