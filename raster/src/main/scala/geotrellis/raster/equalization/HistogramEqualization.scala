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

package geotrellis.raster.equalization

import geotrellis.raster._
import geotrellis.raster.histogram.Histogram
import geotrellis.raster.histogram.StreamingHistogram


/**
  * Uses the approach given here:
  * http://www.math.uci.edu/icamp/courses/math77c/demos/hist_eq.pdf
  */
object HistogramEqualization {

  /**
    * Comparison class for sorting an array of (x, cdf(x))
    * pairs by x label.
    */
  private class BucketComparator extends java.util.Comparator[(Double, Double)] {
    // Compare the (label, cdf(label)) pairs by their labels
    def compare(left: (Double, Double), right: (Double, Double)): Int = {
      if (left._1 < right._1) -1
      else if (left._1 > right._1) 1
      else 0
    }
  }

  private val cmp = new BucketComparator

  /**
    * Given a [[CellType]] and a CDF, this function produces a
    * function that takes an intensity x to CDF(x).
    *
    * @param  cellType  The CellType in which the intensity x is given
    * @param  cdf       The CDF
    * @param  x         An intensity value which is mapped to CDF(x) by the returned function
    * @return           A function from Double => Double which maps x to CDF(x)
    */
  @inline def intensityToCdf(cellType: CellType, cdf: Array[(Double, Double)])(x: Double): Double = {
    val i = java.util.Arrays.binarySearch(cdf, (x, 0.0), cmp)
    val smallestCdf = cdf(0)._2
    val rawCdf =
      if (x < cdf(0)._1) { // x is smaller than any label in the array
        smallestCdf // there is almost no mass here, so report 0.0
      }
      else if (-1 * i > cdf.length) { // x is larger than any label in the array
        1.0 // there is almost no mass here, so report 1.0
      }
      else if (i >= 0) { // i is the location of the label x in the array
        cdf(i)._2
      }
      else { // x is between two labels in the array
        val j = -1 * i - 2
        val label0 = cdf(j+0)._1
        val label1 = cdf(j+1)._1
        val t = (x - label0) / (label1 - label0)
        val cdf0 = cdf(j+0)._2
        val cdf1 = cdf(j+1)._2
        (1.0-t)*cdf0 + t*cdf1
      }

    math.max(0.0, math.min(1.0, (rawCdf - smallestCdf) / (1.0 - smallestCdf)))
  }

  /**
    * Given a [[CellType]] and an intensity value in the unit
    * interval, this function returns an corresponding intensity value
    * appropriately scaled for the given cell type.
    */
  @inline def toIntensity(cellType: CellType, x: Double): Double = {
    val bits = cellType.bits

    cellType match {
      case _: FloatCells => (Float.MaxValue * (2*x - 1.0))
      case _: DoubleCells => (Double.MaxValue * (2*x - 1.0))
      case _: BitCells | _: UByteCells | _: UShortCells =>
        ((1<<bits) - 1) * x
      case _: ByteCells | _: ShortCells | _: IntCells =>
        (((1<<bits) - 1) * x) - (1<<(bits-1))
    }
  }

  /**
    * When a function from intensity to the CDF is given as input,
    * this function implements of the transformation T referred to
    * in the citation given above.
    *
    * @param  cellType  The [[CellType]] of the output of the returned function
    * @param  fn        A function from Double => Double which takes an intensity value x to CDF(x)
    * @param  x         A value CDF(x) which is mapped to an intensity value
    * @return           A function Double => Double which takes CDF(x) and returns an intensity value
    */
  @inline private def transform(cellType: CellType, fn: (Double => Double))(x: Double): Double =
    toIntensity(cellType, fn(x))

  /**
    * Given a [[Tile]], return a Tile with an equalized histogram.
    *
    * @param  tile  A singleband tile
    * @return       A singleband tile with improved contrast
    */
  def apply(tile: Tile): Tile = {
    val histogram = StreamingHistogram.fromTile(tile, 1<<17)
    HistogramEqualization(tile, histogram)
  }

  /**
    * Given a [[Tile]] and a
    * [[geotrellis.raster.histogram.Histogram]], return a Tile with an
    * equalized histogram.
    *
    * @param  tile       A singleband tile
    * @param  histogram  The histogram of the tile
    * @return            A singleband tile with improved contrast
    */
  def apply[T <: AnyVal](tile: Tile, histogram: Histogram[T]): Tile = {
    val localIntensityToCdf = intensityToCdf(tile.cellType, histogram.cdf)_
    val localTransform = transform(tile.cellType, localIntensityToCdf)_
    tile.mapDouble(localTransform)
  }

  /**
    * Given a [[MultibandTile]], return a MultibandTile whose bands
    * all have equalized histograms.
    *
    * @param  tile  A multiband tile
    * @return       A multiband tile with improved contrast
    */
  def apply(tile: MultibandTile): MultibandTile = {
    val histograms = tile.bands.map({ tile => StreamingHistogram.fromTile(tile, 1<<17) })
    HistogramEqualization(tile, histograms)
  }

  /**
    * Given a [[MultibandTile]] and a
    * [[geotrellis.raster.histogram.Histogram]] for each of its bands,
    * return a MultibandTile whose bands all have equalized
    * histograms.
    *
    * @param  tile        A multiband tile
    * @param  histograms  A sequence of histograms, one for each band
    * @return             A multiband tile with improved contrast
    */
  def apply[T <: AnyVal](tile: MultibandTile, histograms: Seq[Histogram[T]]): MultibandTile = {
    MultibandTile(
      tile.bands
        .zip(histograms)
        .map({ case (tile, histogram) => HistogramEqualization(tile, histogram) })
    )
  }

}
