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

package geotrellis.spark.reproject

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.resample._
import geotrellis.raster.reproject._
import geotrellis.raster.reproject.Reproject.{Options => RasterReprojectOptions}
import geotrellis.spark._
import geotrellis.spark.reproject.Reproject.Options
import geotrellis.spark.tiling._
import geotrellis.spark.testkit._
import geotrellis.vector._

import geotrellis.proj4._

import spire.syntax.cfor._

import org.apache.spark._
import org.scalatest.FunSpec

class TileRDDReprojectSpec extends FunSpec with TestEnvironment {

  describe("TileRDDReproject") {
    val path = "raster/data/aspect.tif"
    val gt = SinglebandGeoTiff(path)
    val originalRaster = gt.raster.resample(500, 500)

    // import geotrellis.raster.render._
    // val rainbow = ColorMap((0.0 to 360.0 by 1.0).map{ deg => (deg, HSV.toRGB(deg, 1.0, 1.0)) }.toMap)
    // originalRaster.tile.renderPng(rainbow).write("original.png")

    val (raster, rdd) = {
      val (raster, rdd) = createTileLayerRDD(originalRaster, 10, 10, gt.crs)
      (raster, rdd.withContext { rdd => rdd.repartition(20) })
    }

    def testReproject(method: ResampleMethod, constantBuffer: Boolean): Unit = {
      val expected =
        ProjectedRaster(raster, gt.crs).reproject(
          LatLng,
          RasterReprojectOptions(method = method, errorThreshold = 0)
        )

      // expected.tile.renderPng(rainbow).write("expected.png")

      val (_, actualRdd) =
        if(constantBuffer) {
          rdd.reproject(
            LatLng,
            FloatingLayoutScheme(25),
            bufferSize = 2,
            Options(
              rasterReprojectOptions = RasterReprojectOptions(method = method, errorThreshold = 0),
              matchLayerExtent = true
            )
          )
        } else {
          rdd.reproject(
            LatLng,
            FloatingLayoutScheme(25),
            Options(
              rasterReprojectOptions = RasterReprojectOptions(method = method, errorThreshold = 0),
              matchLayerExtent = true
            )
          )
        }

      val actual =
        actualRdd.stitch

      actualRdd.map { case (_, tile) => tile.dimensions == (25, 25) }.reduce(_ && _) should be (true)

      // actual.tile.renderPng(rainbow).write("actual.png")
      // val errorTile = IntArrayTile.ofDim(expected.tile.cols, expected.tile.rows)
      // cfor(0)(_ < expected.rows, _ + 1) { row =>
      //   cfor(0)(_ < expected.cols, _ + 1) { col =>
      //     val diff = math.abs(actual.tile.getDouble(col, row) - expected.tile.getDouble(col, row))
      //     if (isNoData(actual.tile.getDouble(col, row)) || diff <= 1e-3)
      //       errorTile.set(col, row, 0)
      //     else
      //       errorTile.set(col, row, 1)
      //   }
      // }
      // errorTile.renderPng(ColorMap(0 -> 0x000000ff, 1 -> 0xff0000ff)).write("error.png")

      // Account for tiles being a bit bigger then the actual result
      actual.extent.covers(expected.extent) should be (true)
      actual.rasterExtent.extent.xmin should be (expected.rasterExtent.extent.xmin +- 0.00001)
      actual.rasterExtent.extent.ymax should be (expected.rasterExtent.extent.ymax +- 0.00001)
      actual.rasterExtent.cellwidth should be (expected.rasterExtent.cellwidth +- 0.00001)
      actual.rasterExtent.cellheight should be (expected.rasterExtent.cellheight +- 0.00001)

      val expectedTile = expected.tile
      val actualTile = actual.tile

      actualTile.cols should be >= (expectedTile.cols)
      actualTile.rows should be >= (expectedTile.rows)

      var errCount = 0
      val errors = collection.mutable.ListBuffer.empty[String]
      cfor(0)(_ < actual.rows, _ + 1) { row =>
        cfor(0)(_ < actual.cols, _ + 1) { col =>
          val a = actualTile.getDouble(col, row)
          if(row >= expectedTile.rows || col >= expectedTile.cols) {
            isNoData(a) should be (true)
          } else if(row != 1){
            val expected = expectedTile.getDouble(col, row)
            if (a.isNaN) {
              if (!expected.isNaN) {
                errors.append(s"Failed at col: $col and row: $row, $a != $expected\n")
                errCount += 1
              }
            } else if (expected.isNaN) {
              errors.append(s"Failed at col: $col and row: $row, $a != $expected\n")
              errCount += 1
            } else {
              if (math.abs(expected - a) > 3) {
                errors.append(s"Failed at col: $col and row: $row, $a != $expected\n")
                errCount += 1
              }
            }
          }
        }
      }

      if (errCount > 24) {
        fail("Too many pixels do not agree.  Error log follows.\n" ++ errors.reduce(_++_))
      }
    }

    it("should reproject a raster split into tiles the same as the raster itself: constant border and Bilinear") {
      testReproject(Bilinear, true)
    }

    it("should reproject a raster split into tiles the same as the raster itself: dynamic border and Bilinear") {
      testReproject(Bilinear, false)
    }

    it("should reproject a raster split into tiles the same as the raster itself: constant border and NearestNeighbor") {
      testReproject(NearestNeighbor, true)
    }

    it("should reproject a raster split into tiles the same as the raster itself: dynamic border and NearestNeighbor") {
      testReproject(NearestNeighbor, false)
    }

    it("should function correctly for multiband tiles") {
      val expected =
        ProjectedRaster(raster, gt.crs).reproject(
          LatLng,
          RasterReprojectOptions(NearestNeighbor, errorThreshold = 0)
        )

      val mbrdd = ContextRDD(rdd.mapValues{ tile => MultibandTile(Array(tile)) }, rdd.metadata)
      val (_, actualRdd) =
        mbrdd.reproject(
          LatLng,
          FloatingLayoutScheme(25),
          Options(
            rasterReprojectOptions = RasterReprojectOptions(NearestNeighbor, errorThreshold = 0),
            matchLayerExtent = true
          )
        )

      val actual: Raster[MultibandTile] =
        actualRdd.stitch

      // actual.tile.renderPng(rainbow).write("actual.png")

      // Account for tiles being a bit bigger then the actual result
      actual.extent.covers(expected.extent) should be (true)
      actual.rasterExtent.extent.xmin should be (expected.rasterExtent.extent.xmin +- 0.00001)
      actual.rasterExtent.extent.ymax should be (expected.rasterExtent.extent.ymax +- 0.00001)
      actual.rasterExtent.cellwidth should be (expected.rasterExtent.cellwidth +- 0.00001)
      actual.rasterExtent.cellheight should be (expected.rasterExtent.cellheight +- 0.00001)

      val expectedTile = expected.tile
      val actualTile = actual.tile

      actualTile.cols should be >= (expectedTile.cols)
      actualTile.rows should be >= (expectedTile.rows)

      val tile = actual.tile.bandSafe(0).get

      cfor(0)(_ < actual.rows, _ + 1) { row =>
        cfor(0)(_ < actual.cols, _ + 1) { col =>
          val a = tile.getDouble(col, row)
          if(row >= expectedTile.rows || col >= expectedTile.cols) {
            isNoData(a) should be (true)
          } else if(row != 1){
            val expected = expectedTile.getDouble(col, row)
            if (a.isNaN) {
              withClue(s"Failed at col: $col and row: $row, $a != $expected") {
                expected.isNaN should be (true)
              }
            } else if (expected.isNaN) {
              withClue(s"Failed at col: $col and row: $row, $a != $expected") {
                a.isNaN should be (true)
              }
            } else {
              withClue(s"Failed at col: $col and row: $row, $a != $expected") {
                a should be (expected +- 0.001)
              }
            }
          }
        }
      }
    }

    it("should retain the source RDD's Partitioner") {
      val partitioner = new HashPartitioner(8)
      val expectedPartitioner = Some(partitioner)
      val repartitioned = rdd.withContext{ _.partitionBy(partitioner) }
      val actualPartitioner = repartitioned.reproject(ZoomedLayoutScheme(LatLng))._2.partitioner

      actualPartitioner should be (expectedPartitioner)
    }

    it("should have the designated Partitioner") {
      val partitioner = new HashPartitioner(8)
      val expectedPartitioner = Some(partitioner)
      val actualPartitioner = rdd.reproject(ZoomedLayoutScheme(LatLng), expectedPartitioner)._2.partitioner

      actualPartitioner should be (expectedPartitioner)
    }
  }

  describe("Reprojected with the same scheme and CRS") {
    it("should tile with minimum number of tiles") {
      val tiff = SinglebandGeoTiff(new java.io.File(inputHomeLocalPath, "aspect.tif").getAbsolutePath)
      val rdd = sc.parallelize(Seq( (tiff.projectedExtent, tiff.tile.toArrayTile: Tile) ))
      val scheme = FloatingLayoutScheme(256)
      val extent = Extent(-31.4569758,  27.6350020, 40.2053192,  80.7984255)
      val cellSize = CellSize(0.083328250000000, 0.083328250000000)
      val re = RasterExtent(extent, cellSize)

      val (_, md) = rdd.collectMetadata[SpatialKey](scheme)
      val tiled = ContextRDD(rdd.tileToLayout[SpatialKey](md, NearestNeighbor), md)
      val beforeMetadata = tiled.metadata

      val (_, reprojected) = tiled.reproject(tiled.metadata.crs, scheme)
      val afterMetadata = reprojected.metadata

      afterMetadata.layout should be (beforeMetadata.layout)
    }
  }
}
