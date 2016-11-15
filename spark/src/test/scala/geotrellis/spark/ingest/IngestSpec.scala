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
    it("should read GeoTiff with overrided input CRS") {
      val source = sc.hadoopGeoTiffRDD(new Path(inputHome, "all-ones.tif"), sc.defaultTiffExtensions, crs = "EPSG:3857")
      source.take(1).toList.map { case (k, _) => k.crs.proj4jCrs.getName }.head shouldEqual "EPSG:3857"
    }

    it("should ingest GeoTiff") {
      val source = sc.hadoopGeoTiffRDD(new Path(inputHome, "all-ones.tif"))
      Ingest[ProjectedExtent, SpatialKey](source, LatLng, ZoomedLayoutScheme(LatLng, 512)) { (rdd, zoom) =>
        zoom should be (10)
        rdd.filter(!_._2.isNoDataTile).count should be (8)
      }
    }

    it("should ingest GeoTiff with preset max zoom level") {
      val source = sc.hadoopGeoTiffRDD(new Path(inputHome, "all-ones.tif"))
      Ingest[ProjectedExtent, SpatialKey](source, LatLng, ZoomedLayoutScheme(LatLng, 512), maxZoom = Some(11)) { (rdd, zoom) =>
        zoom should be (11)
        rdd.filter(!_._2.isNoDataTile).count should be (18)
      }
    }
  }
}
