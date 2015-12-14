package geotrellis.spark.io.hadoop

import geotrellis.proj4.LatLng
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.testfiles._
import geotrellis.spark.tiling._
import geotrellis.vector._
import org.apache.hadoop.fs.Path
import org.scalatest._

class HadoopIngestSpec
  extends FunSpec
    with Matchers
    with RasterRDDMatchers
    with TestEnvironment with TestFiles
    with TestSparkContext{

  val layoutScheme = ZoomedLayoutScheme(LatLng, 512)

  it("should allow filtering files in hadoopGeoTiffRDD") {
    val tilesDir = new Path(localFS.getWorkingDirectory,
      "raster-test/data/one-month-tiles/")
    val source = sc.hadoopGeoTiffRDD(tilesDir)

    // Raises exception if the bogus file isn't properly filtered out
    Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme){ (rdd, level) => {} }
  }

  it("should allow overriding tiff file extensions in hadoopGeoTiffRDD") {
    val tilesDir = new Path(localFS.getWorkingDirectory,
      "raster-test/data/one-month-tiles-tiff/")
    val source = sc.hadoopGeoTiffRDD(tilesDir, ".tiff")

    // Raises exception if the ".tiff" extension override isn't provided
    Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme){ (rdd, level) => {} }
  }
}

