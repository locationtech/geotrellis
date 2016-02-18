package geotrellis.spark.ingest

import geotrellis.raster._
import geotrellis.raster.io.arg.ArgReader
import geotrellis.vector._
import geotrellis.proj4._

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats._
import geotrellis.proj4.LatLng
import geotrellis.spark.utils.SparkUtils

import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.hadoop.fs.Path
import org.joda.time.DateTime
import org.scalatest._


class IngestSpec extends FunSpec
  with Matchers
  with TestEnvironment
{

  describe("Ingest") {
    it("should ingest GeoTiff"){
      val source = sc.hadoopGeoTiffRDD(new Path(inputHome, "all-ones.tif"))
      Ingest[ProjectedExtent, SpatialKey](source, LatLng, ZoomedLayoutScheme(LatLng, 512)){ (rdd, zoom) =>
        zoom should be (10)
        rdd.count should be (8)
      }
    }
  }
}
