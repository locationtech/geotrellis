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

package geotrellis.spark.io.cog

import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.ingest._
import geotrellis.spark.stitch._
import geotrellis.spark.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.proj4._
import geotrellis.raster.Tile
import geotrellis.raster.io.geotiff.{GeoTiff, GeoTiffTile, SinglebandGeoTiff}
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex
import geotrellis.spark.testkit._

import org.apache.hadoop.fs.Path
import org.scalatest._

import java.net.URI

import geotrellis.spark.io.file.FileAttributeStore
import geotrellis.spark.io.file.cog.{FileCOGLayerReader, FileCOGValueReader}

import scala.collection.mutable.ListBuffer

class COGLayerSpec extends FunSpec
  with Matchers
  with TestEnvironment {
  describe("COGLayer") {
  it("should write GeoTrellis stitch based COGLayer") {
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

    val cogs = COGLayer(layer)(zoom, 10, layoutScheme)
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
  }

    it("should write GeoTrellis COGLayer") {
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

      val cogs = COGLayer(layer)(zoom, 10, layoutScheme)
      cogs.write(index, new URI("file:///tmp/write2"))

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

  it("nicez") {
    import geotrellis.spark.io.file.cog._

    val attributeStore = FileAttributeStore("/data/chatta-cogs-file")
    lazy val reader = new FileCOGLayerReader(attributeStore, "/data/chatta-cogs-file")
    lazy val tileReader = new FileCOGValueReader(attributeStore, "/data/chatta-cogs-file")

    tileReader.reader[SpatialKey, Tile](LayerId("mask", 0)).read(SpatialKey(0, 0))
  }

  it("test new COG") {
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


    val cogs = COGLayer.applyWithMetadataCalc(layer)(zoom, layoutScheme, minZoom = Some(7))

    COGLayer.write(cogs.layers.values.last)(index, new URI("file:///tmp/test3"))
    COGLayer.write(cogs.layers.values.head)(index, new URI("file:///tmp/test2"))
  }
}
