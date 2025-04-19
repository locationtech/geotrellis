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

/**
 * [[Kernel]]
 *
 * Represents a neighborhood that is represented by a tile.
 */
case class Kernel(tile: Tile) extends Neighborhood {
  require(tile.rows == tile.cols, "Kernel tile must be square")
  require(tile.rows % 2 == 1, "Kernel tile must have odd dimension")
  val extent = (tile.rows / 2).toInt

  // Not supporting masks, since masks are implemented as 0 values in the kernel weight
  val hasMask = false

  def cellType = tile.cellType

  def add(otherkernel: Kernel): Kernel = {
    val selfarr = tile.toArrayDouble()
    val otherarr = otherkernel.tile.toArrayDouble()
    val otherextent = otherkernel.extent

    if (extent == otherextent) {
      Kernel(DoubleArrayTile(selfarr.zip(otherarr).map { case (x, y) => x + y }, tile.rows, tile.rows))
    } else if (extent < otherextent) {
      val diff = otherextent - extent
      val newarr = otherarr
      var r = 0
      while (r < tile.rows) {
        var c = 0
        while (c < tile.rows) {
          newarr((diff + r) * otherkernel.tile.rows + diff + c) = newarr((diff + r) * otherkernel.tile.rows + diff + c) + selfarr(r * tile.rows + c)
          c += 1
        }
        r += 1
      }
      Kernel(DoubleArrayTile(newarr, otherkernel.tile.rows, otherkernel.tile.rows))
    } else {
      val diff = extent - otherextent
      val newarr = selfarr
      var r = 0
      while (r < otherkernel.tile.rows) {
        var c = 0
        while (c < otherkernel.tile.rows) {
          newarr((diff + r) * tile.rows + diff + c) = newarr((diff + r) * tile.rows + diff + c) + otherarr(r * otherkernel.tile.rows + c)
          c += 1
        }
        r += 1
      }
      Kernel(DoubleArrayTile(newarr, tile.rows, tile.rows))
    }
  }

  /**
   *
   * Returns a kernel which has each of its weights multiplicatively inverted.
   * Weights with a value of zero are not inverted and remain zero.
   *
   */
  def inverse(): Kernel = {
    val newarr = tile.toArrayDouble().map(value => {
      if (value != 0)
        1.0 / value
      else
        0
    })
    Kernel(DoubleArrayTile(newarr, tile.rows, tile.rows))
  }
}

object Kernel {
  implicit def tile2Kernel(r: Tile): Kernel = Kernel(r)

  def apply(nbhd: Neighborhood): Kernel = {
    require(nbhd.hasMask, "Neighborhood must have a mask method")

    val w = 2 * nbhd.extent + 1
    val size = w * w
    val tile = DoubleArrayTile(new Array[Double](size), w, w)

    var r = 0
    while (r < w) {
      var c = 0
      while (c < w) {
        tile.set(c, r, if (nbhd.mask(c, r)) 0 else 1)
        c = c + 1
      }
      r = r + 1
    }

    new Kernel(tile)
  }


  /**
   * Creates a Gaussian kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param    size           Number of rows of the resulting tile.
   * @param    sigma          Sigma parameter for Gaussian
   * @param    amp            Amplitude for Gaussian. Will be the value at the center of
   *                          the resulting tile.
   *
   * @note                    Tile will be IntConstantNoDataCellType
   */
  def gaussian(size: Int, sigma: Double, amp: Double): Kernel = {
    val output = DoubleArrayTile.empty(size, size)

    val denom = 2.0*sigma*sigma

    var r = 0
    var c = 0
    while(r < size) {
      c = 0
      while(c < size) {
        val rsqr = (c - (size / 2)) * (c - (size / 2)) + (r - (size / 2)) * (r - (size / 2))
        val g = amp * (math.exp(-rsqr / denom))
        output.setDouble(c, r, g)
        c += 1
      }
      r += 1
    }

    Kernel(output)
  }

  /**
   * Creates a Circle kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param       size           Number of rows in the resulting tile.
   * @param       cellWidth      Cell width of the resutling tile.
   * @param       rad            Radius of the circle.
   *
   * @note                       Tile will be IntConstantNoDataCellType
   */
  def circle(size: Int, cellWidth: Double, rad: Int) = {
    val output = DoubleArrayTile.empty(size, size)

    val rad2 = rad*rad

    val w = size/2
    var r = -w
    var c = -w
    while(r <= w) {
      while (c <= w) {
        output.setDouble(c + w, r + w, if (r * r + c * c <= rad2) 1 else 0)
        c += 1
      }
      c = -w
      r += 1
    }

    Kernel(output)
  }


  /**
   * Creates a chebyshev kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param rad       Radius of the kernel.
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def chebyshev(rad: Int, normalize: Boolean = false, magnitude: Float = 1): Kernel = {
    val size = 2 * rad + 1
    var output: Array[Double] = Array.ofDim(size * size)
    var r = 0
    var c = 0
    var sum: Double = 0
    while (r < size) {
      c = 0
      while (c < size) {
        val g = Math.max(Math.abs(rad - r), Math.abs(rad - c))
        output(c * size + r) = g
        sum = sum + g
        c += 1
      }
      r += 1
    }
    if (normalize == true)
      output = output.map(_ / sum * magnitude)
    else
      output = output.map(_ * magnitude)

    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Creates a Generates a 3x3 Prewitt's Compass edge-detection kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def compass(normalize: Boolean = false, magnitude: Float = 1): Kernel = {
    val size = 3
    var output: Array[Double] = Array(1.0, 1.0, -1.0, 1.0, -2.0, -1.0, 1.0, 1.0, -1.0)

    if (normalize == true)
      output = output.map(_ / 5.0 * magnitude)
    else
      output = output.map(_ * magnitude)

    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Creates a Generates a cross-shaped('X') boolean kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param rad       Radius of the kernel.
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def cross(rad: Int, normalize: Boolean = true, magnitude: Float = 1): Kernel = {
    val size = 2 * rad + 1
    var output: Array[Double] = Array.ofDim(size * size)
    var r = 0
    var c = 0
    var sum: Double = 4 * rad + 1
    while (r < size) {
      c = 0
      while (c < size) {
        var g = 0
        if (r == c || 2 * rad - r == c) {
          g = 1
        }
        output(c * size + r) = g
        c += 1
      }
      r += 1
    }
    if (normalize == true)
      output = output.map(_ / sum * magnitude)
    else
      output = output.map(_ * magnitude)

    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Creates a diamond-shaped kernel ('◇'). Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param rad       Radius of the kernel.
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def diamond(rad: Int, normalize: Boolean = true, magnitude: Float = 1): Kernel = {
    val size = 2 * rad + 1
    var output: Array[Double] = Array.ofDim(size * size)
    var r = 0
    var c = 0
    var sum: Double = 0
    while (r < size) {
      c = 0
      while (c < size) {
        if (Math.abs(rad - r) + Math.abs(rad - c) <= rad) {
          output(c * size + r) = 1.0
          sum = sum + 1.0
        } else {
          output(c * size + r) = 0
        }
        c += 1
      }
      r += 1
    }
    if (normalize == true)
      output = output.map(_ / sum * magnitude)
    else
      output = output.map(_ * magnitude)

    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Creates a  distance kernel based on Euclidean (straight-line) distance. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param rad       Radius of the kernel.
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def euclidean(rad: Int, normalize: Boolean = false, magnitude: Float = 1): Kernel = {
    val size = 2 * rad + 1
    var output: Array[Double] = Array.ofDim(size * size)
    var r = 0
    var c = 0
    var sum: Double = 0
    while (r < size) {
      c = 0
      while (c < size) {
        val rc = r - rad;
        val cc = c - rad;
        val g = Math.sqrt(Math.pow(rad - c, 2) + Math.pow(rad - r, 2))
        output(c * size + r) = g
        sum = sum + g
        c += 1
      }
      r += 1
    }
    if (normalize == true)
      output = output.map(_ / sum * magnitude)
    else
      output = output.map(_ * magnitude)

    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Creates a Gaussian kernel (by Two-dimensional Gaussian distribution). Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param rad       Radius of the kernel.
   * @param sigma     Standard deviation of the Gaussian function
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def gaussianWithoutAmp(rad: Int, sigma: Float, normalize: Boolean = false, magnitude: Float = 1): Kernel = {
    val size = 2 * rad + 1
    var output: Array[Double] = Array.ofDim(size * size)
    var r = 0
    var c = 0
    var sum: Double = 0
    while (r < size) {
      c = 0
      while (c < size) {
        val calc = -(Math.pow(rad - c, 2) + Math.pow(rad - r, 2))
        val param = 2 * sigma * sigma
        val g = (1 / (Math.PI * param)) * Math.exp(calc / param)
        output(c * size + r) = g
        sum = sum + g
        c += 1
      }
      r += 1
    }
    if (normalize == true)
      output = output.map(_ / sum * magnitude)
    else
      output = output.map(_ * magnitude)
    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Creates a 3x3 Kirsch's Compass edge-detection kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param direction The direction of kernel use "N、NW、W、SW、S、SE、E and NE"
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def kirsch(direction: String = "W", normalize: Boolean = false, magnitude: Float = 1): Kernel = {
    val size = 3
    var output: Array[Double] = direction match {
      case "N" => Array(5.0, 5.0, 5.0, -3.0, 0, -3.0, -3.0, -3.0, -3.0)
      case "NW" => Array(5.0, 5.0, -3.0, 5.0, 0, -3.0, -3.0, -3.0, -3.0)
      case "W" => Array(5.0, -3.0, -3.0, 5.0, 0, -3.0, 5.0, -3.0, -3.0)
      case "SW" => Array(-3.0, -3.0, -3.0, 5.0, 0, -3.0, 5.0, 5.0, -3.0)
      case "S" => Array(-3.0, -3.0, -3.0, -3.0, 0, -3.0, 5.0, 5.0, 5.0)
      case "SE" => Array(-3.0, -3.0, -3.0, -3.0, 0, 5.0, -3.0, 5.0, 5.0)
      case "E" => Array(-3.0, -3.0, 5.0, -3.0, 0, 5.0, -3.0, -3.0, 5.0)
      case "NE" => Array(-3.0, 5.0, 5.0, -3.0, 0, 5.0, -3.0, -3.0, -3.0)
    }

    if (normalize == true)
      output = output.map(_ / 15.0 * magnitude)
    else
      output = output.map(_ * magnitude)

    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Creates a 3x3 Laplacian-4 edge-detection kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def laplacian4(normalize: Boolean = false, magnitude: Float = 1): Kernel = {
    val size = 3
    var output: Array[Double] = Array(0, 1, 0, 1, -4, 1, 0, 1, 0)

    if (normalize == true)
      output = output.map(_ / 4.0 * magnitude)
    else
      output = output.map(_ * magnitude)

    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Creates a 3x3 Laplacian-8 edge-detection kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def laplacian8(normalize: Boolean = false, magnitude: Float = 1): Kernel = {
    val size = 3
    var output: Array[Double] = Array(1, 1, 1, 1, -8, 1, 1, 1, 1)

    if (normalize == true)
      output = output.map(_ / 8.0 * magnitude)
    else
      output = output.map(_ * magnitude)

    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Creates a chebyshev kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param rad       Radius of the kernel.
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def manhattan(rad: Int, normalize: Boolean = false, magnitude: Float = 1): Kernel = {
    val size = 2 * rad + 1
    var output: Array[Double] = Array.ofDim(size * size)
    var r = 0
    var c = 0
    var sum: Double = 0
    while (r < size) {
      c = 0
      while (c < size) {
        val g = Math.abs(rad - r) + Math.abs(rad - c)
        output(c * size + r) = g
        sum = sum + g
        c += 1
      }
      r += 1
    }
    if (normalize == true)
      output = output.map(_ / sum * magnitude)
    else
      output = output.map(_ * magnitude)
    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Createsan octagon-shaped boolean kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param rad       Radius of the kernel.
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def octagon(rad: Int, normalize: Boolean = true, magnitude: Float = 1): Kernel = {
    val size = 2 * rad + 1
    var output: Array[Double] = Array.ofDim(size * size)
    var r = 0
    var c = 0
    var sum: Double = 0
    while (r < size) {
      c = 0
      while (c < size) {
        if (Math.abs(rad - r) + Math.abs(rad - c) <= 1.5 * rad) {
          output(c * size + r) = 1.0
          sum = sum + 1.0
        } else {
          output(c * size + r) = 0
        }
        c += 1
      }
      r += 1
    }
    if (normalize == true)
      output = output.map(_ / sum * magnitude)
    else
      output = output.map(_ * magnitude)

    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Creates a plus-shaped boolean kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param rad       Radius of the kernel.
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def plus(rad: Int, normalize: Boolean = true, magnitude: Float = 1): Kernel = {
    val size = 2 * rad + 1
    var output: Array[Double] = Array.ofDim(size * size)
    var r = 0
    var c = 0
    var sum: Double = 4 * rad + 1
    while (r < size) {
      c = 0
      while (c < size) {
        var g = 0
        if (r == rad || c == rad) {
          g = 1
        }
        output(c * size + r) = g
        c += 1
      }
      r += 1
    }
    if (normalize == true)
      output = output.map(_ / sum * magnitude)
    else
      output = output.map(_ * magnitude)

    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Creates a 3x3 Prewitt edge-detection kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def prewitt(normalize: Boolean = false, magnitude: Float = 1): Kernel = {
    val size = 3
    var output: Array[Double] = Array(1, 0, -1, 1, 0, -1, 1, 0, -1)

    if (normalize == true)
      output = output.map(_ / 3.0 * magnitude)
    else
      output = output.map(_ * magnitude)

    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Creates a 2x2 Roberts edge-detection kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def roberts(magnitude: Float = 1): Kernel = {
    val size = 2
    var output: Array[Double] = Array(1, 0, 0, -1)
    output = output.map(_ * magnitude)
    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Creates a 3x3 Sobel edge-detection kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def Sobel(normalize: Boolean = false, magnitude: Float = 1): Kernel = {
    val size = 3
    var output: Array[Double] = Array(-1, 0, 1, -2, 0, 2, 1, 0, 1)

    if (normalize == true)
      output = output.map(_ / 4.0 * magnitude)
    else
      output = output.map(_ * magnitude)

    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }

  /**
   * Creates a square-shaped boolean kernel. Can be used with the [[Convolve]] or
   * KernelDensity operations.
   *
   * @param rad       Radius of the kernel.
   * @param normalize Normalize the kernel values to sum to 1.
   * @param magnitude Scale each value by this amount.
   * @note Tile will be DoubleConstantNoDataCellType
   */
  def square(rad: Int, normalize: Boolean = true, magnitude: Float = 1): Kernel = {
    val size = 2 * rad + 1
    var output: Array[Double] = Array.fill(size * size)(1.0)
    val sum = size * size
    if (normalize == true)
      output = output.map(_ / sum * magnitude)
    else
      output = output.map(_ * magnitude)

    val arrTile = DoubleArrayTile(output, size, size)
    Kernel(arrTile)
  }


}
