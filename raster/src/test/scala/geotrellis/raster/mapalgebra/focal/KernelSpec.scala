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

package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.io.geotiff.{GeoTiffReader, MultibandGeoTiff}
import geotrellis.raster.mapalgebra.focal.Kernel._
import org.scalatest.funsuite.AnyFunSuite

class KernelSpec extends AnyFunSuite {
  test("chebyshev") {
    val chebyshevKernel = chebyshev(3)
    val tile = chebyshevKernel.tile
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      r += 1
      println(arr.toList)
    }
  }

  test("compass") {
    val compassKernel = compass()
    val tile = compassKernel.tile
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      r += 1
      println(arr.toList)
    }
  }

  test("cross") {
    val crossKernel = cross(3, false, 100)
    val tile = crossKernel.tile
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      r += 1
      println(arr.toList)
    }
  }
  test("diamond") {
    val diamondKernel = diamond(3, false)
    val tile = diamondKernel.tile
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      r += 1
      println(arr.toList)
    }
  }

  test("euclidean") {
    val euclideanKernel = euclidean(3, false)
    val tile = euclideanKernel.tile
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r).formatted("%.3f").toDouble
        c += 1
      }
      r += 1
      println(arr.toList)
    }
  }

  test("gaussianWithoutAmp") {
    val gaussianKernel = gaussianWithoutAmp(3, 1)
    val tile = gaussianKernel.tile
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      r += 1
      println(arr.toList)
    }
  }

  test("kirsch") {
    val kirschKernel = kirsch("S")
    val tile = kirschKernel.tile
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      r += 1
      println(arr.toList)
    }
  }
  test("laplacian4") {
    val laplacian4Kernel = laplacian4(true)
    val tile = laplacian4Kernel.tile
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      r += 1
      println(arr.toList)
    }
  }


  test("laplacian8") {
    val laplacian8Kernel = laplacian8(true)
    val tile = laplacian8Kernel.tile
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      r += 1
      println(arr.toList)
    }
  }

  test("manhattan") {
    val manhattanKernel = manhattan(3, true)
    val tile = manhattanKernel.tile
    var r = 0
    var testarr: Array[Double] = Array.ofDim(tile.rows * tile.cols)
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        testarr(r * tile.rows + c) = tile.getDouble(c, r)
        c += 1
      }
      r += 1
      println(arr.toList)
    }
    print(testarr.reduce(_ + _))
  }

  test("octagon") {
    val octagonKernel = octagon(3, false)
    val tile = octagonKernel.tile
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      r += 1
      println(arr.toList)
    }
  }

  test("plus") {
    val plusKernel = plus(3, false)
    val tile = plusKernel.tile
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      r += 1
      println(arr.toList)
    }
  }

  test("prewitt") {
    val prewittKernel = prewitt(false)
    val tile = prewittKernel.tile
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      r += 1
      println(arr.toList)
    }
  }

  test("square") {
    val squareKernel = square(3, false)
    val tile = squareKernel.tile
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      r += 1
      println(arr.toList)
    }
  }

  test("test add") {
    val squareKernel1 = octagon(3, false, 100)
    val tile1 = squareKernel1.tile
    var r1 = 0
    while (r1 < tile1.rows) {
      var c1 = 0
      val arr: Array[Double] = Array.ofDim(tile1.rows)
      while (c1 < tile1.cols) {
        arr(c1) = tile1.getDouble(c1, r1)
        c1 += 1
      }
      r1 += 1
      println(arr.toList)
    }
    println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
    val squareKernel2 = octagon(5, false, 100)
    val tile2 = squareKernel2.tile
    var r2 = 0
    while (r2 < tile2.rows) {
      var c2 = 0
      val arr: Array[Double] = Array.ofDim(tile2.rows)
      while (c2 < tile2.cols) {
        arr(c2) = tile2.getDouble(c2, r2)
        c2 += 1
      }
      r2 += 1
      println(arr.toList)
    }
    println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
    val addsquareKernel = squareKernel1.add(squareKernel2)
    val tile = addsquareKernel.tile
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr: Array[Double] = Array.ofDim(tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      r += 1
      println(arr.toList)
    }
  }
  test("test inverse") {
    val plusKernel1 = plus(5, false, 100)
    val tile1 = plusKernel1.tile
    var r1 = 0
    while (r1 < tile1.rows) {
      var c1 = 0
      val arr: Array[Double] = Array.ofDim(tile1.rows)
      while (c1 < tile1.cols) {
        arr(c1) = tile1.getDouble(c1, r1)
        c1 += 1
      }
      r1 += 1
      println(arr.toList)
    }
    println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
    val plusKernel2 = plusKernel1.inverse()
    val tile2 = plusKernel2.tile
    var r2 = 0
    while (r2 < tile2.rows) {
      var c2 = 0
      val arr: Array[Double] = Array.ofDim(tile2.rows)
      while (c2 < tile2.cols) {
        arr(c2) = tile2.getDouble(c2, r2)
        c2 += 1
      }
      r2 += 1
      println(arr.toList)
    }
  }

  test("focal mean") {
    val gaussianKernel = gaussianWithoutAmp(3, 1)
    val multibandGeoTiff = GeoTiffReader.readMultiband(s"raster/data/geotiff-test-files/jpeg-test-deflate-small.tif")
    val fmtile = multibandGeoTiff.tile.mapBands((_, tile) => tile.focalMean(gaussianKernel))
    MultibandGeoTiff(fmtile, multibandGeoTiff.extent, multibandGeoTiff.crs)
      .write("raster/data/geotiff-test-files/jpeg-test-deflate-small-new.tif")
  }


}
