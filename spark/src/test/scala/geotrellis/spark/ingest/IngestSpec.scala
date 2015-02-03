package geotrellis.spark.ingest

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.hadoop.formats.NetCdfBand
import geotrellis.spark.tiling._
import geotrellis.proj4.LatLng
import geotrellis.spark.utils.SparkUtils

import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.hadoop.fs.Path
import org.joda.time.DateTime
import org.scalatest._


class IngestSpec extends FunSpec
  with Matchers
  with TestEnvironment
  with OnlyIfCanRunSpark
{

  describe("Ingest") {
    ifCanRunSpark { 


      it("should ingest GeoTiff"){
        val source = sc.hadoopGeoTiffRDD(new Path(inputHome, "all-ones.tif"))
        val (level, rdd) = Ingest[ProjectedExtent, SpatialKey](source, LatLng, ZoomedLayoutScheme(512))

        level.zoom should be (10)
        rdd.count should be (18)
      }

      it("should ingest time-band NetCDF") {
        implicit val tiler: Tiler[NetCdfBand, SpaceTimeKey] = {
          val getExtent = (inKey: NetCdfBand) => inKey.extent
          val createKey = (inKey: NetCdfBand, spatialComponent: SpatialKey) =>
            SpaceTimeKey(spatialComponent, inKey.time)

          Tiler(getExtent, createKey)
        }

        val source = sc.netCdfRDD(new Path(inputHome, "ipcc-access1-tasmin.nc"))
        val (md, rdd) = Ingest[NetCdfBand, SpaceTimeKey](source, LatLng, ZoomedLayoutScheme(512))

        val expectedKeys = List(
          SpaceTimeKey(SpatialKey(1,1),TemporalKey(DateTime.parse("2006-03-16T12:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(2,0),TemporalKey(DateTime.parse("2006-01-16T12:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(2,1),TemporalKey(DateTime.parse("2006-02-15T00:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(0,0),TemporalKey(DateTime.parse("2006-01-16T12:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(2,1),TemporalKey(DateTime.parse("2006-01-16T12:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(2,1),TemporalKey(DateTime.parse("2006-03-16T12:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(0,1),TemporalKey(DateTime.parse("2006-03-16T12:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(0,1),TemporalKey(DateTime.parse("2006-02-15T00:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(0,1),TemporalKey(DateTime.parse("2006-01-16T12:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(1,0),TemporalKey(DateTime.parse("2006-02-15T00:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(1,0),TemporalKey(DateTime.parse("2006-01-16T12:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(0,0),TemporalKey(DateTime.parse("2006-02-15T00:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(1,0),TemporalKey(DateTime.parse("2006-03-16T12:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(1,1),TemporalKey(DateTime.parse("2006-01-16T12:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(1,1),TemporalKey(DateTime.parse("2006-02-15T00:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(2,0),TemporalKey(DateTime.parse("2006-02-15T00:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(2,0),TemporalKey(DateTime.parse("2006-03-16T12:00:00.000Z"))),
          SpaceTimeKey(SpatialKey(0,0),TemporalKey(DateTime.parse("2006-03-16T12:00:00.000Z")))
        )

        val ingestKeys = rdd.map(_._1).collect
        ingestKeys should contain only (expectedKeys: _*)
      }
    }
  }
}
