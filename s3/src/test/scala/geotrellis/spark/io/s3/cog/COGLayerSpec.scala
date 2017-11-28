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

import java.net.URI

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
  /* it("should write GeoTrellis stitch based COGLayer") {
    val source = sc.hadoopGeoTiffRDD(new Path("file:///Users/daunnc/subversions/git/github/pomadchin/geotrellis/raster/data/geotiff-test-files/reproject/cea.tif"))
    val list: ListBuffer[(Int, TileLayerRDD[SpatialKey])] = ListBuffer()
    val layoutScheme = ZoomedLayoutScheme(LatLng, 512)

    Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme) { (rdd, zoom) =>
      list += zoom -> rdd
    }

    val (zoom, layer) = list.head
    val index: ZSpatialKeyIndex = new ZSpatialKeyIndex(layer.metadata.bounds match {
      case kb: KeyBounds[SpatialKey] => kb
      case _ => null
    })

    //COGLayer.withStitch(layer)(zoom, 7, layoutScheme).collect().head

    val cogs = COGLayerS(layer)(zoom, 7, layoutScheme)
    cogs.write(index, new URI("file:///tmp/write"))

    val (key, tiff: GeoTiff[Tile]) = cogs.collect().head
    val sfc = index.toIndex(key)

    val tiff2 =
      GeoTiff(layer.stitch, layer.metadata.mapTransform(layer.metadata.gridBounds), LatLng)
        .copy(options = tiff.options)

    GeoTiffWriter.write(tiff2.crop(layer.metadata.extent), "/tmp/testo2.tif", optimizedOrder = true)

    //GeoTiffWriter.write(tiff, s"/tmp/${sfc}_base.tif", optimizedOrder = true)
    //GeoTiffWriter.write(tiff.overviews(0), s"/tmp/${sfc}_0.tif", optimizedOrder = true)
    //GeoTiffWriter.write(tiff.overviews(1), s"/tmp/${sfc}_1.tif", optimizedOrder = true)
    //GeoTiffWriter.write(tiff.overviews(2), s"/tmp/${sfc}_2.tif", optimizedOrder = true)
  } */

    it("should read GeoTrellis COGLayer") {
      val attributeStore = S3AttributeStore("geotrellis-test", "daunnc/cogs")

      val reader = new S3COGLayerReader(attributeStore)

      val layer = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](LayerId("test3", 11))

      val tiff =
        GeoTiff(layer.stitch, layer.metadata.mapTransform(layer.metadata.gridBounds), LatLng)

      GeoTiffWriter.write(tiff.crop(layer.metadata.extent), "/tmp/tests77.tif", optimizedOrder = true)
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

      //COGLayer.withStitch(layer)(zoom, 7, layoutScheme).collect().head

      val cogs = COGLayer.applyWithMetadata(layer)(zoom, 7, layoutScheme)

      writer.write(cogs)(LayerId("test3", 0), keyIndexMethod)

      // cogs.write(index, new URI("file:///tmp/write2"))

      //val (key, tiff: GeoTiff[Tile]) = cogs.collect().head
      /*val sfc = index.toIndex(key)

      val tiff2 =
        GeoTiff(layer.stitch, layer.metadata.mapTransform(layer.metadata.gridBounds), LatLng)
          .copy(options = tiff.options)

      GeoTiffWriter.write(tiff2.crop(layer.metadata.extent), "/tmp/testo2.tif", optimizedOrder = true)*/

      //GeoTiffWriter.write(tiff, s"/tmp/${sfc}_base.tif", optimizedOrder = true)
      //GeoTiffWriter.write(tiff.overviews(0), s"/tmp/${sfc}_0.tif", optimizedOrder = true)
      //GeoTiffWriter.write(tiff.overviews(1), s"/tmp/${sfc}_1.tif", optimizedOrder = true)
      //GeoTiffWriter.write(tiff.overviews(2), s"/tmp/${sfc}_2.tif", optimizedOrder = true)
    }
  }
}
