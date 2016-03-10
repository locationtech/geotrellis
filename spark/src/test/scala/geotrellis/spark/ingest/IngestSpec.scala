package geotrellis.spark.ingest

import geotrellis.vector._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.proj4.LatLng

import org.apache.hadoop.fs.Path
import org.scalatest._

/** Actually failes due to LatLon issue: https://github.com/geotrellis/geotrellis/issues/1341 */
/*class IngestSpec extends FunSpec
  with Matchers
  with TestEnvironment {
  describe("Ingest") {
    it("should ingest GeoTiff"){
      val source = sc.hadoopGeoTiffRDD(new Path(inputHome, "all-ones.tif"))
      Ingest[ProjectedExtent, GridKey](source, LatLng, ZoomedLayoutScheme(LatLng, 512)){ (rdd, zoom) =>
        zoom should be (11)
        rdd.filter(!_._2.isNoDataTile).count should be (18)
      }
    }
  }
}*/
