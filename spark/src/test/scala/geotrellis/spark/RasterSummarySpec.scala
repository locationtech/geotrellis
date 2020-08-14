/*
 * Copyright 2019 Azavea
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

package geotrellis.spark

import geotrellis.spark.partition._
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.geotiff._
import geotrellis.raster.resample.Bilinear
import geotrellis.layer._

import org.apache.spark.rdd._
import geotrellis.spark.testkit._
import geotrellis.raster.testkit._

import org.scalatest.GivenWhenThen
import org.scalatest.funspec.AnyFunSpec
import java.time.{ZoneOffset, ZonedDateTime}

class RasterSummarySpec extends AnyFunSpec with TestEnvironment with RasterMatchers with GivenWhenThen {

  describe("Should collect GeoTiffRasterSource RasterSummary correct") {
    it("should collect summary for a raw source") {
      val inputPath = Resource.path("vlm/aspect-tiled.tif")
      val files = inputPath :: Nil

      val sourceRDD: RDD[RasterSource] =
        sc.parallelize(files, files.length)
          .map(uri => GeoTiffRasterSource(uri): RasterSource)
          .cache()

      val metadata = RasterSummary.fromRDD(sourceRDD)
      val rasterSource = GeoTiffRasterSource(inputPath)

      rasterSource.crs shouldBe metadata.crs
      rasterSource.extent shouldBe metadata.extent
      rasterSource.cellSize shouldBe metadata.cellSize
      rasterSource.cellType shouldBe metadata.cellType
      rasterSource.size shouldBe metadata.cells
      files.length shouldBe metadata.count
    }

    it("should collect summary for a tiled to layout source") {
      val inputPath = GeoTiffPath(Resource.path("vlm/aspect-tiled.tif"))
      val files = inputPath :: Nil
      val targetCRS = WebMercator
      val method = Bilinear
      val layoutScheme = ZoomedLayoutScheme(targetCRS, tileSize = 256)

      val sourceRDD: RDD[RasterSource] =
        sc.parallelize(files, files.length)
          .map(uri => GeoTiffRasterSource(uri).reproject(targetCRS, method = method): RasterSource)
          .cache()

      val summary = RasterSummary.fromRDD(sourceRDD)
      val LayoutLevel(zoom, layout) = summary.levelFor(layoutScheme)
      val tiledLayoutSource = sourceRDD.map(_.tileToLayout(layout, method))

      val summaryCollected = RasterSummary.fromRDD(tiledLayoutSource.map(_.source))
      val summaryResampled = summary.resample(TargetAlignment(layout))

      val metadata = summary.toTileLayerMetadata(layout)
      val metadataResampled = summaryResampled.toTileLayerMetadata(GlobalLayout(256, zoom, 0.1))

      metadata shouldBe metadataResampled

      summaryCollected.crs shouldBe summaryResampled.crs
      summaryCollected.cellType shouldBe summaryResampled.cellType

      val CellSize(widthCollected, heightCollected) = summaryCollected.cellSize
      val CellSize(widthResampled, heightResampled) = summaryResampled.cellSize

      // the only weird place where cellSize is a bit different
      widthCollected shouldBe (widthResampled +- 1e-7)
      heightCollected shouldBe (heightResampled +- 1e-7)

      summaryCollected.extent shouldBe summaryResampled.extent
      summaryCollected.cells shouldBe summaryResampled.cells
      summaryCollected.count shouldBe summaryResampled.count
    }
  }

  it("should create ContextRDD from RDD of GeoTiffRasterSources") {
    val inputPath = GeoTiffPath(Resource.path("vlm/aspect-tiled.tif"))
    val files = inputPath :: Nil
    val targetCRS = WebMercator
    val method = Bilinear
    val layoutScheme = ZoomedLayoutScheme(targetCRS, tileSize = 256)

    // read sources
    val sourceRDD: RDD[RasterSource] =
      sc.parallelize(files, files.length)
        .map(uri => GeoTiffRasterSource(uri).reproject(targetCRS, method = method): RasterSource)
        .cache()

    // collect raster summary
    val summary = RasterSummary.fromRDD(sourceRDD)
    val LayoutLevel(_, layout) = summary.levelFor(layoutScheme)
    val tiledLayoutSource = sourceRDD.map(_.tileToLayout(layout, method))

    // Create RDD of references, references contain information how to read rasters
    val rasterRefRdd: RDD[(SpatialKey, RasterRegion)] = tiledLayoutSource.flatMap(_.keyedRasterRegions())
    val tileRDD: RDD[(SpatialKey, MultibandTile)] =
      rasterRefRdd // group by keys and distribute raster references using SpatialPartitioner
        .groupByKey(SpatialPartitioner(summary.estimatePartitionsNumber))
        .mapValues { iter => MultibandTile(iter.flatMap(_.raster.toSeq.flatMap(_.tile.bands))) } // read rasters

    val metadata = summary.toTileLayerMetadata(layout)
    val contextRDD: MultibandTileLayerRDD[SpatialKey] = ContextRDD(tileRDD, metadata)

    contextRDD.count() shouldBe rasterRefRdd.count()
    contextRDD.count() shouldBe 72

    contextRDD.stitch.tile.band(0).renderPng().write("/tmp/raster-source-contextrdd.png")
  }

  it("should collect temporal contextRDD") {
    val dates = "2018-01-01" :: "2018-02-01" :: "2018-03-01" :: Nil
    val expectedDates = dates.map { str =>
      val Array(y, m, d) = str.split("-").map(_.toInt)
      ZonedDateTime.of(y, m, d,0,0,0,0, ZoneOffset.UTC).toInstant.toEpochMilli
    }
    val files = dates.map { str => GeoTiffPath(Resource.path(s"vlm/aspect-tiled-$str.tif")) }
    val targetCRS = WebMercator
    val method = Bilinear
    val layoutScheme = ZoomedLayoutScheme(targetCRS, tileSize = 256)

    // read sources
    val sourceRDD: RDD[RasterSource] =
      sc.parallelize(files, files.length)
        .map(uri => GeoTiffRasterSource(uri).reproject(targetCRS, method = method): RasterSource)
        .cache()

    // Im mr. happy face now (with a knife and i bite people)
    val temporalKeyExtractor = TemporalKeyExtractor.fromPath { path =>
      val date = raw"(\d{4})-(\d{2})-(\d{2})".r.findFirstMatchIn(path.toString)
      val Some((y, m, d)) = date.map { d => (d.group(1).toInt, d.group(2).toInt, d.group(3).toInt) }

      ZonedDateTime.of(y, m, d, 0, 0, 0, 0, ZoneOffset.UTC)
    }

    // collect raster summary
    val summary = RasterSummary.fromRDD(sourceRDD, temporalKeyExtractor.getMetadata)
    // lets add layoutScheme overload
    val LayoutLevel(_, layout) = summary.levelFor(layoutScheme)

    val contextRDD =
      RasterSourceRDD.tiledLayerRDD(sourceRDD, layout, temporalKeyExtractor, rasterSummary = Some(summary))

    val (minDate, maxDate) = expectedDates.head -> expectedDates.last

    contextRDD.metadata.bounds match {
      case KeyBounds(minKey, maxKey) =>
        minKey.instant shouldBe minDate
        maxKey.instant shouldBe maxDate

      case EmptyBounds => throw new Exception("EmptyBounds are not allowed here")
    }

    contextRDD.count() shouldBe 72 * dates.length

    contextRDD
      .toSpatial(minDate)
      .stitch
      .tile
      .band(0)
      .renderPng()
      .write(s"/tmp/raster-source-contextrdd-${minDate}.png")
  }
}
