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
import geotrellis.raster.testkit.RasterMatchers
import org.scalatest.funsuite.AnyFunSuite

class KernelSpec extends AnyFunSuite with RasterMatchers {
  test("chebyshev") {
    val chebyshevKernel = chebyshev(5)
    val tile = chebyshevKernel.tile

    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0),
      Array(5.0, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 5.0),
      Array(5.0, 4.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 4.0, 5.0),
      Array(5.0, 4.0, 3.0, 2.0, 2.0, 2.0, 2.0, 2.0, 3.0, 4.0, 5.0),
      Array(5.0, 4.0, 3.0, 2.0, 1.0, 1.0, 1.0, 2.0, 3.0, 4.0, 5.0),
      Array(5.0, 4.0, 3.0, 2.0, 1.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0),
      Array(5.0, 4.0, 3.0, 2.0, 1.0, 1.0, 1.0, 2.0, 3.0, 4.0, 5.0),
      Array(5.0, 4.0, 3.0, 2.0, 2.0, 2.0, 2.0, 2.0, 3.0, 4.0, 5.0),
      Array(5.0, 4.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 4.0, 5.0),
      Array(5.0, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 4.0, 5.0),
      Array(5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0)
    )

    actual shouldBe expected
  }

  test("compass") {
    val compassKernel = compass()
    val tile = compassKernel.tile

    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(1.0, 1.0, -1.0),
      Array(1.0, -2.0, -1.0),
      Array(1.0, 1.0, -1.0),
    )

    actual shouldBe expected
  }

  test("cross") {
    val crossKernel = cross(3, false, 100)
    val tile = crossKernel.tile

    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 100.0),
      Array(0.0, 100.0, 0.0, 0.0, 0.0, 100.0, 0.0),
      Array(0.0, 0.0, 100.0, 0.0, 100.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 100.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 100.0, 0.0, 100.0, 0.0, 0.0),
      Array(0.0, 100.0, 0.0, 0.0, 0.0, 100.0, 0.0),
      Array(100.0, 0.0, 0.0, 0.0, 0.0, 0.0, 100.0)
    )

    actual shouldBe expected

  }
  test("diamond") {
    val diamondKernel = diamond(3, normalize = false)
    val tile = diamondKernel.tile

    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0),
      Array(0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0),
      Array(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
      Array(0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0),
      Array(0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0)
    )

    actual shouldBe expected
  }

  test("euclidean") {
    val euclideanKernel = euclidean(3, normalize = false)
    val tile = euclideanKernel.tile

    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = f"${tile.getDouble(c, r)}%.3f".toDouble
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(4.243, 3.606, 3.162, 3.0, 3.162, 3.606, 4.243),
      Array(3.606, 2.828, 2.236, 2.0, 2.236, 2.828, 3.606),
      Array(3.162, 2.236, 1.414, 1.0, 1.414, 2.236, 3.162),
      Array(3.0, 2.0, 1.0, 0.0, 1.0, 2.0, 3.0),
      Array(3.162, 2.236, 1.414, 1.0, 1.414, 2.236, 3.162),
      Array(3.606, 2.828, 2.236, 2.0, 2.236, 2.828, 3.606),
      Array(4.243, 3.606, 3.162, 3.0, 3.162, 3.606, 4.243)
    )

    actual shouldBe expected
  }

  test("gaussianWithoutAmp") {
    val gaussianKernel = gaussianWithoutAmp(3, 1)
    val tile = gaussianKernel.tile

    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(1.964128034639744E-5, 2.392797792004706E-4, 0.0010723775711956546, 0.0017680517118520167, 0.0010723775711956546, 2.392797792004706E-4, 1.964128034639744E-5),
      Array(2.392797792004706E-4, 0.0029150244650281935, 0.013064233284684921, 0.021539279301848634, 0.013064233284684921, 0.0029150244650281935, 2.392797792004706E-4),
      Array(0.0010723775711956546, 0.013064233284684921, 0.05854983152431917, 0.09653235263005391, 0.05854983152431917, 0.013064233284684921, 0.0010723775711956546),
      Array(0.0017680517118520167, 0.021539279301848634, 0.09653235263005391, 0.15915494309189535, 0.09653235263005391, 0.021539279301848634, 0.0017680517118520167),
      Array(0.0010723775711956546, 0.013064233284684921, 0.05854983152431917, 0.09653235263005391, 0.05854983152431917, 0.013064233284684921, 0.0010723775711956546),
      Array(2.392797792004706E-4, 0.0029150244650281935, 0.013064233284684921, 0.021539279301848634, 0.013064233284684921, 0.0029150244650281935, 2.392797792004706E-4),
      Array(1.964128034639744E-5, 2.392797792004706E-4, 0.0010723775711956546, 0.0017680517118520167, 0.0010723775711956546, 2.392797792004706E-4, 1.964128034639744E-5),
    )

    actual shouldBe expected
  }

  test("kirsch") {
    val kirschKernel = kirsch("S")
    val tile = kirschKernel.tile

    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(-3.0, -3.0, -3.0),
      Array(-3.0, 0.0, -3.0),
      Array(5.0, 5.0, 5.0)
    )

    actual shouldBe expected
  }

  test("laplacian4") {
    val laplacian4Kernel = laplacian4(normalize = true)
    val tile = laplacian4Kernel.tile

    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(0.0, 0.25, 0.0),
      Array(0.25, -1.0, 0.25),
      Array(0.0, 0.25, 0.0)
    )

    actual shouldBe expected
  }

  test("laplacian8") {
    val laplacian8Kernel = laplacian8(normalize =true)
    val tile = laplacian8Kernel.tile

    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(0.125, 0.125, 0.125),
      Array(0.125, -1.0, 0.125),
      Array(0.125, 0.125, 0.125)
    )

    actual shouldBe expected
  }

  test("manhattan") {
    val manhattanKernel = manhattan(3, normalize = true)
    val tile = manhattanKernel.tile

    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var sum = 0d
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        sum += tile.getDouble(c, r)
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(0.03571428571428571, 0.02976190476190476, 0.023809523809523808, 0.017857142857142856, 0.023809523809523808, 0.02976190476190476, 0.03571428571428571),
      Array(0.02976190476190476, 0.023809523809523808, 0.017857142857142856, 0.011904761904761904, 0.017857142857142856, 0.023809523809523808, 0.02976190476190476),
      Array(0.023809523809523808, 0.017857142857142856, 0.011904761904761904, 0.005952380952380952, 0.011904761904761904, 0.017857142857142856, 0.023809523809523808),
      Array(0.017857142857142856, 0.011904761904761904, 0.005952380952380952, 0.0, 0.005952380952380952, 0.011904761904761904, 0.017857142857142856),
      Array(0.023809523809523808, 0.017857142857142856, 0.011904761904761904, 0.005952380952380952, 0.011904761904761904, 0.017857142857142856, 0.023809523809523808),
      Array(0.02976190476190476, 0.023809523809523808, 0.017857142857142856, 0.011904761904761904, 0.017857142857142856, 0.023809523809523808, 0.02976190476190476),
      Array(0.03571428571428571, 0.02976190476190476, 0.023809523809523808, 0.017857142857142856, 0.023809523809523808, 0.02976190476190476, 0.03571428571428571)
    )

    actual shouldBe expected
    sum shouldBe 1d +-.000000000000001
  }

  test("octagon") {
    val octagonKernel = octagon(3, normalize = false)
    val tile = octagonKernel.tile

    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0),
      Array(0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0),
      Array(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
      Array(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
      Array(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
      Array(0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0),
      Array(0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0)
    )

    actual shouldBe expected
  }

  test("plus") {
    val plusKernel = plus(3, normalize = false)
    val tile = plusKernel.tile

    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0),
      Array(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
      Array(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0)
    )

    actual shouldBe expected
  }

  test("prewitt") {
    val prewittKernel = prewitt(normalize = false)
    val tile = prewittKernel.tile

    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(1.0, 0.0, -1.0),
      Array(1.0, 0.0, -1.0),
      Array(1.0, 0.0, -1.0)
    )

    actual shouldBe expected
  }

  test("square") {
    val squareKernel = square(3, normalize = false)
    val tile = squareKernel.tile

    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
      Array(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
      Array(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
      Array(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
      Array(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
      Array(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
      Array(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
    )

    actual shouldBe expected
  }

  test("test add") {
    val squareKernel1 = octagon(3, normalize = false, magnitude = 100)
    val tile1 = squareKernel1.tile

    var r1 = 0
    while (r1 < tile1.rows) {
      var c1 = 0
      val arr = Array.ofDim[Double](tile1.rows)
      while (c1 < tile1.cols) {
        arr(c1) = tile1.getDouble(c1, r1)
        c1 += 1
      }
      r1 += 1
    }

    val squareKernel2 = octagon(5, normalize = false, magnitude = 100)
    val tile2 = squareKernel2.tile
    var r2 = 0
    while (r2 < tile2.rows) {
      var c2 = 0
      val arr = Array.ofDim[Double](tile2.rows)
      while (c2 < tile2.cols) {
        arr(c2) = tile2.getDouble(c2, r2)
        c2 += 1
      }
      r2 += 1
    }

    val addsquareKernel = squareKernel1.add(squareKernel2)
    val tile = addsquareKernel.tile
    val actual = Array.ofDim[Double](tile.rows, tile.cols)
    var r = 0
    while (r < tile.rows) {
      var c = 0
      val arr = Array.ofDim[Double](tile.rows)
      while (c < tile.cols) {
        arr(c) = tile.getDouble(c, r)
        c += 1
      }
      actual(r) = arr
      r += 1
    }

    val expected = Array(
      Array(0.0, 0.0, 0.0, 100.0, 100.0, 100.0, 100.0, 100.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 0.0, 0.0),
      Array(0.0, 100.0, 100.0, 100.0, 200.0, 200.0, 200.0, 100.0, 100.0, 100.0, 0.0),
      Array(100.0, 100.0, 100.0, 200.0, 200.0, 200.0, 200.0, 200.0, 100.0, 100.0, 100.0),
      Array(100.0, 100.0, 200.0, 200.0, 200.0, 200.0, 200.0, 200.0, 200.0, 100.0, 100.0),
      Array(100.0, 100.0, 200.0, 200.0, 200.0, 200.0, 200.0, 200.0, 200.0, 100.0, 100.0),
      Array(100.0, 100.0, 200.0, 200.0, 200.0, 200.0, 200.0, 200.0, 200.0, 100.0, 100.0),
      Array(100.0, 100.0, 100.0, 200.0, 200.0, 200.0, 200.0, 200.0, 100.0, 100.0, 100.0),
      Array(0.0, 100.0, 100.0, 100.0, 200.0, 200.0, 200.0, 100.0, 100.0, 100.0, 0.0),
      Array(0.0, 0.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 100.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 100.0, 100.0, 100.0, 100.0, 100.0, 0.0, 0.0, 0.0)
    )

    actual shouldBe expected
  }
  test("test inverse") {
    val plusKernel1 = plus(5, normalize = false, magnitude = 100)
    val tile1 = plusKernel1.tile
    var r1 = 0
    while (r1 < tile1.rows) {
      var c1 = 0
      val arr = Array.ofDim[Double](tile1.rows)
      while (c1 < tile1.cols) {
        arr(c1) = tile1.getDouble(c1, r1)
        c1 += 1
      }
      r1 += 1
    }

    val plusKernel2 = plusKernel1.inverse()
    val tile2 = plusKernel2.tile
    val actual = Array.ofDim[Double](tile2.rows, tile2.cols)
    var r2 = 0
    while (r2 < tile2.rows) {
      var c2 = 0
      val arr = Array.ofDim[Double](tile2.rows)
      while (c2 < tile2.cols) {
        arr(c2) = tile2.getDouble(c2, r2)
        c2 += 1
      }
      actual(r2) = arr
      r2 += 1
    }

    val expected = Array(
      Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.01, 0.0, 0.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.01, 0.0, 0.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.01, 0.0, 0.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.01, 0.0, 0.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.01, 0.0, 0.0, 0.0, 0.0, 0.0),
      Array(0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01),
      Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.01, 0.0, 0.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.01, 0.0, 0.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.01, 0.0, 0.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.01, 0.0, 0.0, 0.0, 0.0, 0.0),
      Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.01, 0.0, 0.0, 0.0, 0.0, 0.0)
    )

    actual shouldBe expected
  }

  test("focal mean") {
    val gaussianKernel = gaussianWithoutAmp(3, 1)
    val multibandGeoTiff = GeoTiffReader.readMultiband(s"raster/data/geotiff-test-files/jpeg-test-small.tif")
    val actual = multibandGeoTiff.tile.mapBands((_, tile) => tile.focalMean(gaussianKernel))
    val expected = GeoTiffReader.readMultiband(s"raster/data/geotiff-test-files/jpeg-test-small-gaussian-expected.tif").tile

    assertEqual(actual, expected)
  }
}
