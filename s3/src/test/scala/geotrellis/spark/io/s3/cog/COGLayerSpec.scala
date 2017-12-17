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
import geotrellis.raster.reproject.Reproject.{Options => RasterReprojectOptions}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.reproject._
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
import java.nio.file.Paths

import geotrellis.raster.resample.NearestNeighbor
import geotrellis.spark.io.cog.vrt.VRT

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

      GeoTiffWriter.write(tiff.crop(layer.metadata.extent), "/tmp/tests123.tif", optimizedOrder = true)
    }

    it("should write GeoTrellis COGLayer") {
      val source =
        sc.hadoopGeoTiffRDD(
          new Path(s"file:///${Paths.get("raster").toAbsolutePath.toString}/data/geotiff-test-files/reproject/cea.tif")
        )
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

    it("should create no metadata GeoTrellis COGLayer") {
      val source =
        sc.hadoopGeoTiffRDD(
          new Path(s"file:///${Paths.get("raster").toAbsolutePath.toString}/data/geotiff-test-files/reproject/cea.tif")
        )
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

      //val attributeStore = S3AttributeStore("geotrellis-test", "daunnc/cogs")
      //val writer =
      //  new S3COGLayerWriter(() => attributeStore, "geotrellis-test", "daunnc/cogs")

      val cogs1 = COGLayer.applyWithMetadata(layer)(zoom, 10, layoutScheme)
      println(s"cogs1.count: ${cogs1.count()}")
      println(s"cogs1.map(_._1).collect().toList: ${cogs1.map(_._1).collect().toList}")

      val cogs2 = COGLayer.apply(layer)(zoom, 10, layoutScheme)
      println(s"cogs2.count: ${cogs2.count()}")
      println(s"cogs2.map(_._1).collect().toList: ${cogs2.map(_._1).collect().toList}")

      //writer.write(cogs)(LayerId("test", 0), keyIndexMethod)
    }

    it("should create no metadata GeoTrellis stitched layer COGLayer") {
      // /Users/daunnc/Downloads/2452.tiff
      val source =
        sc.hadoopGeoTiffRDD(
          new Path(s"file:///${Paths.get("raster").toAbsolutePath.toString}/data/geotiff-test-files/reproject/cea.tif")
        )
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

      val cogs = COGLayer.applyWithMetadata(layer)(zoom, 10, layoutScheme)

      writer.write(cogs)(LayerId("testStitched", 0), keyIndexMethod)
    }

    it("should write GeoTrellis COGLayerZZZ") {
      val source =
        sc.hadoopGeoTiffRDD(
          new Path(s"file:///${Paths.get("raster").toAbsolutePath.toString}/data/geotiff-test-files/reproject/cea.tif")
        )
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

      val cogs = COGLayer.applyWithMetadata(layer)(zoom, 10, layoutScheme)

      writer.write(cogs)(LayerId("test10", 0), keyIndexMethod)
    }

    it("should write chatta GeoTrellis COGLayer") {
      val layoutScheme = ZoomedLayoutScheme(LatLng, 256)

      val sourceTiles =
        sc.hadoopGeoTiffRDD(
          new Path(s"file:///data/arg_wm/DevelopedLand.tiff")
        )

      val (_, md) = sourceTiles.collectMetadata[SpatialKey](FloatingLayoutScheme(256))

      println(s"md.crs.epsgCode: ${md.crs.epsgCode}")

      val tiled = ContextRDD(sourceTiles.tileToLayout[SpatialKey](md, NearestNeighbor), md)

      val LayoutLevel(zoom, layoutDefinition) = layoutScheme.levelForZoom(13)
      val layer =
        tiled
          .reproject(
            LatLng, layoutDefinition,
            RasterReprojectOptions(method = NearestNeighbor, targetCellSize = Some(layoutDefinition.cellSize))
          )._2

      val tiff =
        GeoTiff(layer.stitch, layer.metadata.mapTransform(layer.metadata.gridBounds), LatLng)

      GeoTiffWriter.write(tiff, "/tmp/testsChatta.tif", optimizedOrder = true)
    }

    it("should read split GeoTrellis COGLayer") {
      val attributeStore = S3AttributeStore("geotrellis-test", "daunnc/cogs")

      val reader = new S3COGLayerReader(attributeStore)

      val layer = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](LayerId("testSplited", 11))

      val tiff =
        GeoTiff(layer.stitch, layer.metadata.mapTransform(layer.metadata.gridBounds), LatLng)

      GeoTiffWriter.write(tiff.crop(layer.metadata.extent), "/tmp/testSplited.tif", optimizedOrder = true)
    }

    it("should read split value GeoTrellis COGLayer") {
      // KeyBounds(SpatialKey(177,318),SpatialKey(178,321))

      val attributeStore = S3AttributeStore("geotrellis-test", "daunnc/cogs")

      val reader = new S3COGLayerReader(attributeStore)

      val valueReader = new S3COGValueReader(attributeStore).reader[SpatialKey, Tile](LayerId("testSplited", 11))

      val tile = valueReader.read(SpatialKey(355,638))

      tile.renderPng().write(s"/tmp/${355}_${638}.png")
    }

    it("should write png") {
      val source =
        sc.hadoopGeoTiffRDD(
          new Path(s"file:///${Paths.get("raster").toAbsolutePath.toString}/data/geotiff-test-files/reproject/cea.tif")
        )

      val list: ListBuffer[(Int, TileLayerRDD[SpatialKey])] = ListBuffer()
      val layoutScheme = ZoomedLayoutScheme(LatLng, 512)

      Ingest[ProjectedExtent, SpatialKey](source, LatLng, layoutScheme) { (rdd, zoom) =>
        list += zoom -> rdd
      }

      val map = list.toMap

      println(s"map.keys: ${map.keys}")
      map(11).collect().toList.map { case (SpatialKey(col, row), tile) =>
        tile.renderPng().write(s"/tmp/pngs_t/${col}_${row}.png")
      }
    }

    it("should write split GeoTrellis COGLayer") {
      val source =
        sc.hadoopGeoTiffRDD(
          new Path(s"file:///${Paths.get("raster").toAbsolutePath.toString}/data/geotiff-test-files/reproject/cea.tif")
        )

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

      val cogsList = COGLayer.applyWithMetadataCalc(layer)(zoom, layoutScheme, minZoom = Some(7))

      cogsList.map { cogs =>
        writer.write(cogs)(LayerId("testSplited", 0), keyIndexMethod)
      }
    }

    it("should write vrt split GeoTrellis COGLayer") {
      val source =
        sc.hadoopGeoTiffRDD(
          new Path(s"file:///${Paths.get("raster").toAbsolutePath.toString}/data/geotiff-test-files/reproject/cea.tif")
        )

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

      val cogsList = COGLayer.applyWithMetadataCalc(layer)(zoom, layoutScheme, minZoom = Some(7))

      var i = 0
      cogsList.map { cogs =>
        val vrt = writer.writeVRT(cogs)(LayerId("testSplited", 0), keyIndexMethod, vrtOnly = true)
        vrt.write(s"/tmp/testSplited_${i}.vrt")
        i = i + 1
      }
    }
  }
}
