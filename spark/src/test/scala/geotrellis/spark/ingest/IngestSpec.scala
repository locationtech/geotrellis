package geotrellis.spark.ingest

import geotrellis.raster._
import geotrellis.raster.reproject._
import geotrellis.raster.mosaic._
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
  with OnlyIfGdalInstalled
{

  describe("Ingest") {
    it("should ingest GeoTiff"){
      val source = sc.hadoopGeoTiffRDD(new Path(inputHome, "all-ones.tif"))
      Ingest[ProjectedExtent, SpatialKey](source, LatLng, ZoomedLayoutScheme(LatLng, 512)){ (rdd, zoom) =>
        zoom should be (10)
        rdd.count should be (8)
      }
    }

    ifGdalInstalled {
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

      it("should ingest time-band NetCDF") {
        val source = sc.netCdfRDD(new Path(inputHome, "ipcc-access1-tasmin.nc"))
        Ingest[NetCdfBand, SpaceTimeKey](source, LatLng, FloatingLayoutScheme(256)){ (rdd, level) =>
          val ingestKeys = rdd.keys.collect()
          info(ingestKeys.toList.toString)
          ingestKeys should contain theSameElementsAs expectedKeys
        }
      }

      it("should ingest time-band NetCDF in stages"){
        val source = sc.netCdfRDD(new Path(inputHome, "ipcc-access1-tasmin.nc"))
        val (zoom, rmd) = source.collectMetaData(LatLng, FloatingLayoutScheme(256))
        val tiled = source.tile[SpaceTimeKey](rmd)
        val ingestKeys = tiled.keys.collect()
        ingestKeys should contain theSameElementsAs expectedKeys
      }
    }
  }
}
