package geotrellis.spark.ingest

import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.proj4._

import org.apache.hadoop.fs.Path
import org.scalatest._

class IngestSpec extends FunSpec
  with Matchers
  with TestEnvironment {
  describe("Ingest") {
    it("should ingest GeoTiff") {
      val source = sc.hadoopGeoTiffRDD(new Path(inputHome, "all-ones.tif"))
      Ingest[ProjectedExtent, SpatialKey](source, LatLng, ZoomedLayoutScheme(LatLng, 512)) { (rdd, zoom) =>
        zoom should be (10)
        rdd.filter(!_._2.isNoDataTile).count should be (8)
      }
    }
  }
}
