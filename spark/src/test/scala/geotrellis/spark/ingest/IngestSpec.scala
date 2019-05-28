/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.ingest

import geotrellis.vector._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.store.hadoop._
import geotrellis.proj4._
import geotrellis.spark.testkit._

import org.apache.hadoop.fs.Path
import org.scalatest._

import scala.collection.mutable

class IngestSpec extends FunSpec
  with Matchers
  with TestEnvironment {
  describe("Ingest") {
    it("should read GeoTiff with overrided input CRS") {
      val source = HadoopGeoTiffRDD.spatial(new Path(inputHome, "all-ones.tif"), HadoopGeoTiffRDD.Options(crs = Some(CRS.fromEpsgCode(3857))))
      // val source = sc.hadoopGeoTiffRDD(new Path(inputHome, "all-ones.tif"), sc.defaultTiffExtensions, crs = "EPSG:3857")
      source.take(1).toList.map { case (k, _) => k.crs.proj4jCrs.getName }.head shouldEqual "EPSG:3857"
    }

    it("should ingest GeoTiff") {
      val source = sc.hadoopGeoTiffRDD(new Path(inputHome, "all-ones.tif"))
      Ingest[ProjectedExtent, SpatialKey](source, LatLng, ZoomedLayoutScheme(LatLng, 512)) { (rdd, zoom) =>
        zoom should be (10)
        rdd.filter(!_._2.isNoDataTile).count should be (8)
      }
    }

    it("should ingest GeoTiff with pyramid on a zoomed and floating schemes") {
      val source = sc.hadoopGeoTiffRDD(new Path(inputHome, "all-ones.tif"))
      val (zlist, flist) = mutable.ListBuffer[(Int, Long)]() -> mutable.ListBuffer[(Int, Long)]()

      // force to use zoomed layout scheme
      Ingest[ProjectedExtent, SpatialKey](source, LatLng, ZoomedLayoutScheme(LatLng, 512), pyramid = true, maxZoom = Some(10)) { (rdd, zoom) =>
        zlist += (zoom -> rdd.filter(!_._2.isNoDataTile).count)
      }

      Ingest[ProjectedExtent, SpatialKey](source, LatLng, ZoomedLayoutScheme(LatLng, 512), pyramid = true) { (rdd, zoom) =>
        flist += (zoom -> rdd.filter(!_._2.isNoDataTile).count)
      }

      zlist should contain theSameElementsAs flist
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
