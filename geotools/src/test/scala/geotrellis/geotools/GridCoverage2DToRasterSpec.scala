/*
 * Copyright (c) 2016 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.geotools

import geotrellis.raster._
import geotrellis.vector._

import org.geotools.coverage.grid._
import org.geotools.coverage.grid.io._
import org.geotools.gce.geotiff._
import org.scalatest._


trait GridCoverage2DToRasterSpec
    extends FunSpec
    with Matchers {

  def getImage(path: String): GridCoverage2D = {
    val policy = AbstractGridFormat.OVERVIEW_POLICY.createValue
    val gridSize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue
    val useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue
    val file = new java.io.File(path)
    val reader = new GeoTiffReader(file)

    policy.setValue(OverviewPolicy.IGNORE)
    gridSize.setValue("1024,1024")
    useJaiRead.setValue(true)

    val image = reader.read(List(policy, gridSize, useJaiRead).toArray)
    val renderedImage = image.getRenderedImage

    require(renderedImage.getHeight <= 1024)
    require(renderedImage.getWidth <= 1024)

    image
  }

  val path: String
  val height: Int
  val width: Int
  val bandCount: Int
  val typeStr: String
  val noData: Option[AnyVal]
  val epsg: Option[Int]

  lazy val image: GridCoverage2D = getImage(path)
  lazy val renderedImage = image.getRenderedImage
  lazy val buffer = renderedImage.getData.getDataBuffer
  lazy val sampleModel = renderedImage.getSampleModel
  lazy val Raster(tile, extent) = GridCoverage2DToRaster(image)

  def sumOfAllSamples: Int = {
    val array = Array.ofDim[Int](sampleModel.getNumBands)
    var result: Int = 0

    var col = 0; while (col < renderedImage.getWidth) {
      var row = 0; while (row < renderedImage.getHeight) {
        sampleModel.getPixel(col, row, array, buffer)
        result += array.sum
        row += 1
      }
      col += 1
    }

    result
  }

  def sumOfAllSamplesDouble: Double = {
    val array = Array.ofDim[Double](sampleModel.getNumBands)
    var result: Double = 0

    var col = 0; while (col < renderedImage.getWidth) {
      var row = 0; while (row < renderedImage.getHeight) {
        sampleModel.getPixel(col, row, array, buffer)
        result += array.sum
        row += 1
      }
      col += 1
    }

    result
  }

  describe("The GridCoverage2DToRaster Object") {

    it("should correctly extract width") {
      tile.cols should be (width)
    }

    it("should correctly extract height") {
      tile.rows should be (height)
    }

    it("should correctly extract bandCount") {
      tile.bandCount should be (bandCount)
    }

    it("should correctly extract cellType") {
      tile.cellType.toString.take(typeStr.length) should be (typeStr)
    }

    it("should correctly extract NODATA value") {
      val actual = GridCoverage2DToRaster.noData(image)
      val expected = noData

      actual should equal (expected)
    }

    it("should correctly extract the CRS") {
      val actual = GridCoverage2DToRaster.crs(image)
      if (actual.nonEmpty)
        actual.get.epsgCode should be (epsg)
      else {
        actual should be (None)
        epsg should be (None)
      }
    }
  }

  describe("The GridCoverage2DMultibandTile Class") {

    it("should have a working foreach method") {
      var result: Int = 0
      tile.foreach({ (_,z) => result += z })

      result should be (sumOfAllSamples)
    }

    it("should have a working foreachDouble method") {
      var result: Double = 0
      tile.foreachDouble({ (_,z) => result += z })

      result should be (sumOfAllSamplesDouble)
    }

    it("should have a working combine method") {
      val n = tile.bandCount
      val subset = (0 until n).filter(_ != 1)
      val array = Array.ofDim[Int](n)
      val actual = tile.combine(subset)({ xs => xs.sum }).get(10, 10)
      sampleModel.getPixel(10, 10, array, buffer)
      val expected = subset.map({ i => array(i) }).sum

      actual should be (expected)
    }

    it("should have a working combineDouble method") {
      val n = tile.bandCount
      val subset = (0 until n).filter(_ != 1)
      val array = Array.ofDim[Double](n)
      val actual = tile.combineDouble(subset)({ xs => xs.sum }).getDouble(10, 10)
      sampleModel.getPixel(10, 10, array, buffer)
      val expected = subset.map({ i => array(i) }).sum

      actual should be (expected)
    }

    it("should have a working map method") {
      val n = tile.bandCount
      val subset = (0 until n).filter(_ != 1)
      val array = Array.ofDim[Int](n)
      val mappedTile = tile.map(subset)({ (i,z) => i + z + 1 })
      val actual = (0 until n).map({ i => mappedTile.bands(i).get(10, 10) }).toList

      sampleModel.getPixel(10, 10, array, buffer)
      var b = 0; while (b < n) {
        if (b != 1)
          array(b) += (b + 1)
        b += 1
      }

      val expected = array.toList.take(n)

      actual should be (expected)
    }

    it("should have a working mapDouble method") {
      val n = tile.bandCount
      val subset = (0 until n).filter(_ != 1)
      val array = Array.ofDim[Double](n)
      val mappedTile = tile.mapDouble(subset)({ (i,z) => i + z + 1.1 })
      val actual = (0 until n).map({ i => mappedTile.bands(i).getDouble(10, 10) }).toList

      sampleModel.getPixel(10, 10, array, buffer)
      var b = 0; while (b < n) {
        if (b != 1)
          array(b) += (b + 1.1)
        b += 1
      }

      val expected = array.toList.take(n)

      actual should be (expected)
    }
  }
}

class NexPrTile_GridCoverage2DToRasterSpec extends GridCoverage2DToRasterSpec {
  val path = "./raster-test/data/geotiff-test-files/nex-pr-tile.tif"
  val width = 512
  val height = 33
  val bandCount = 1
  val typeStr = "float32"
  val noData = Some(1.00000002004087734e+20f)
  val epsg = None
}

class Uint323BandsTiledBand_GridCoverage2DToRasterSpec extends GridCoverage2DToRasterSpec {
  val path = "./raster-test/data/geotiff-test-files/3bands/uint32/3bands-tiled-band.tif"
  val width = 20
  val height = 40
  val bandCount = 3
  val typeStr = "int32r"
  val noData = None
  val epsg = Some(4326)
}

class DeflateStripedUint16_GridCoverage2DToRasterSpec extends GridCoverage2DToRasterSpec {
  val path = "./raster-test/data/geotiff-test-files/deflate/striped/uint16.tif"
  val width = 500
  val height = 600
  val bandCount = 1
  val typeStr = "uint16"
  val noData = Some(-32768)
  val epsg = Some(4326)
}

class GridCoverage2D_UncompressedStripedFloat32Spec extends GridCoverage2DToRasterSpec {
  val path = "./raster-test/data/geotiff-test-files/uncompressed/striped/float32.tif"
  val width = 500
  val height = 600
  val bandCount = 1
  val typeStr = "float32"
  val noData = Some(-1.79769313486231571e+308)
  val epsg = Some(4326)
}

class GridCoverage2D_NdviWebMercatorSpec extends GridCoverage2DToRasterSpec {
  val path = "./raster-test/data/geotiff-test-files/ndvi-web-mercator.tif"
  val width = 231
  val height = 157
  val bandCount = 1
  val typeStr = "uint8raw"
  val noData = None
  val epsg = Some(3857)
}

class GridCoverage2D_Test200506Spec extends GridCoverage2DToRasterSpec {
  val path = "./raster-test/data/one-month-tiles/test-200506000000_0_2.tif"
  val width = 512
  val height = 512
  val bandCount = 1
  val typeStr = "float32"
  val noData = Some(1.00000002004087734e+20)
  val epsg = None
}

class GridCoverage2D_NlcdTileWebMercatorNearestNeighborSpec extends GridCoverage2DToRasterSpec {
  val path = "./raster-test/data/reproject/nlcd_tile_webmercator-nearestneighbor.tif"
  val width = 448
  val height = 569
  val bandCount = 1
  val typeStr = "uint16"
  val noData = None
  val epsg = Some(3857)
}
