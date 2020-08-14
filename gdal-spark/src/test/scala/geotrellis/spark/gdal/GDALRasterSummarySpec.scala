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

package geotrellis.spark.gdal

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.gdal._
import geotrellis.raster.resample.Bilinear
import geotrellis.spark._
import geotrellis.spark.partition._
import geotrellis.layer._
import geotrellis.vector.Extent

import geotrellis.spark.testkit._

import org.apache.spark.rdd._
import spire.syntax.cfor._

import org.scalatest.GivenWhenThen
import org.scalatest.funspec.AnyFunSpec

class GDALRasterSummarySpec extends AnyFunSpec with TestEnvironment with GivenWhenThen {
  import geotrellis.GDALTestUtils._

  describe("Should collect GDALRasterSource RasterSummary correct") {
    it("should collect summary for a raw source") {
      val inputPath = gdalGeoTiffPath("vlm/aspect-tiled.tif")
      val files = inputPath :: Nil

      val sourceRDD: RDD[RasterSource] =
        sc.parallelize(files, files.length)
          .map(uri => GDALRasterSource(uri): RasterSource)
          .cache()

      val summary = RasterSummary.fromRDD(sourceRDD)
      val rasterSource = GDALRasterSource(inputPath)

      rasterSource.crs shouldBe summary.crs
      rasterSource.extent shouldBe summary.extent
      rasterSource.cellSize shouldBe summary.cellSize
      rasterSource.cellType shouldBe summary.cellType
      rasterSource.size shouldBe summary.cells
      files.length shouldBe summary.count
    }

    // TODO: fix this test
    it("should collect summary for a tiled to layout source GDAL") {
      val inputPath = gdalGeoTiffPath("vlm/aspect-tiled.tif")
      val files = inputPath :: Nil
      val targetCRS = WebMercator
      val method = Bilinear
      val layoutScheme = ZoomedLayoutScheme(targetCRS, tileSize = 256)

      val sourceRDD: RDD[RasterSource] =
        sc.parallelize(files, files.length)
          .map(uri => GDALRasterSource(uri).reproject(targetCRS, method = method): RasterSource)
          .cache()

      val summary = RasterSummary.fromRDD(sourceRDD)
      val LayoutLevel(zoom, layout) = summary.levelFor(layoutScheme)
      val tiledRDD = sourceRDD.map(_.tileToLayout(layout, method))

      val summaryCollected = RasterSummary.fromRDD(tiledRDD.map(_.source))
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

      // TODO: investigate the reason of why this won't work here
      // but probably this function should be removed in the future completely and nowhere used
      // val Extent(xminc, yminc, xmaxc, ymaxc) = summaryCollected.extent
      // val Extent(xminr, yminr, xmaxr, ymaxr) = summaryResampled.extent

      // extent probably can be calculated a bit different via GeoTrellis API
      // xminc shouldBe xminr +- 1e-5
      // yminc shouldBe yminr +- 1e-5
      // xmaxc shouldBe xmaxr +- 1e-5
      // ymaxc shouldBe ymaxr +- 1e-5

      // summaryCollected.cells shouldBe summaryResampled.cells
      // summaryCollected.count shouldBe summaryResampled.count
    }
  }

  it("should create ContextRDD from RDD of GDALRasterSources") {
    val inputPath = gdalGeoTiffPath("vlm/aspect-tiled.tif")
    val files = inputPath :: Nil
    val targetCRS = WebMercator
    val method = Bilinear
    val layoutScheme = ZoomedLayoutScheme(targetCRS, tileSize = 256)

    // read sources
    val sourceRDD: RDD[RasterSource] =
      sc.parallelize(files, files.length)
        .map(uri => GDALRasterSource(uri).reproject(targetCRS, method = method): RasterSource)
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
        .mapValues { iter => MultibandTile {
          iter.flatMap { rr => rr.raster.toSeq.flatMap(_.tile.bands) }
        } } // read rasters

    val metadata = summary.toTileLayerMetadata(layout)
    val contextRDD: MultibandTileLayerRDD[SpatialKey] = ContextRDD(tileRDD, metadata)

    val res = contextRDD.collect()
    res.foreach { case (_, v) => v.dimensions shouldBe layout.tileLayout.tileDimensions }
    res.length shouldBe rasterRefRdd.count()
    res.length shouldBe 72

    contextRDD.stitch.tile.band(0).renderPng().write("/tmp/raster-source-contextrdd-gdal.png")
  }

  it("Should cleanup GDAL Datasets by the end of the loop (10 iterations)") {
    val inputPath = gdalGeoTiffPath("vlm/aspect-tiled.tif")
    val targetCRS = WebMercator
    val method = Bilinear
    val layout = LayoutDefinition(GridExtent[Int](Extent(-2.0037508342789244E7, -2.0037508342789244E7, 2.0037508342789244E7, 2.0037508342789244E7), CellSize(9.554628535647032, 9.554628535647032)), 256)
    val RasterExtent(Extent(exmin, eymin, exmax, eymax), ecw, ech, ecols, erows) = RasterExtent(Extent(-8769161.632988561, 4257685.794912352, -8750625.653629405, 4274482.8318780195), CellSize(9.554628535647412, 9.554628535646911))

    cfor(0)(_ < 11, _ + 1) { _ =>
      val reference = GDALRasterSource(inputPath).reproject(targetCRS, method = method).tileToLayout(layout, method)
      val RasterExtent(Extent(axmin, aymin, axmax, aymax), acw, ach, acols, arows) = reference.source.gridExtent.toRasterExtent

      axmin shouldBe exmin +- 1e-5
      aymin shouldBe eymin +- 1e-5
      axmax shouldBe exmax +- 1e-5
      aymax shouldBe eymax +- 1e-5
      acw shouldBe ecw +- 1e-5
      ach shouldBe ech +- 1e-5
      acols shouldBe ecols
      arows shouldBe erows
    }
  }
}
