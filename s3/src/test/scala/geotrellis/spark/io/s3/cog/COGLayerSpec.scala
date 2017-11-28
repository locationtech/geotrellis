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

package geotrellis.spark.io.s3.cog

import geotrellis.proj4._
import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.ingest._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.index.ZCurveKeyIndexMethod
import geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex
import geotrellis.spark.testkit._
import geotrellis.spark.tiling._
import geotrellis.vector._

import org.apache.hadoop.fs.Path
import org.scalatest._

import scala.collection.mutable.ListBuffer

class COGLayerSpec extends FunSpec
  with Matchers
  with TestEnvironment {
  describe("COGLayer") {
    it("should read GeoTrellis COGLayer") {
      val attributeStore = S3AttributeStore("geotrellis-test", "daunnc/cogs")

      val reader = new S3COGLayerReader(attributeStore)

      val layer = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](LayerId("test", 11))

      val tiff =
        GeoTiff(layer.stitch, layer.metadata.mapTransform(layer.metadata.gridBounds), LatLng)

      GeoTiffWriter.write(tiff.crop(layer.metadata.extent), "/tmp/tests.tif", optimizedOrder = true)
    }

    it("should write GeoTrellis COGLayer") {
      // /Users/daunnc/Downloads/2452.tiff
      val source = sc.hadoopGeoTiffRDD(new Path("file:///Users/daunnc/subversions/git/github/pomadchin/geotrellis/raster/data/geotiff-test-files/reproject/cea.tif"))
      val list: ListBuffer[(Int, TileLayerRDD[SpatialKey])] = ListBuffer()
      val layoutScheme = ZoomedLayoutScheme(LatLng, 512)

      Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme) { (rdd, zoom) =>
        list += zoom -> rdd
      }

      val (zoom, layer) = list.head

      val keyIndexMethod = ZCurveKeyIndexMethod

      val index: ZSpatialKeyIndex = new ZSpatialKeyIndex(layer.metadata.bounds match {
        case kb: KeyBounds[SpatialKey] => kb
        case _ => null
      })

      val attributeStore = S3AttributeStore("geotrellis-test", "daunnc/cogs")
      val writer =
        new S3COGLayerWriter(() => attributeStore, "geotrellis-test", "daunnc/cogs")

      val cogs = COGLayer.applyWithMetadata(layer)(zoom, 7, layoutScheme)

      writer.write(cogs)(LayerId("test", 0), keyIndexMethod)
    }
  }
}
