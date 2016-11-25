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

package geotrellis.raster.matching

import geotrellis.raster._
import geotrellis.raster.histogram.Histogram
import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.raster.equalization.HistogramEqualization._


/**
  * Uses the approach given here:
  * http://fourier.eng.hmc.edu/e161/lectures/contrast_transform/node3.html
  */
object HistogramMatching {

  /**
    * Comparison class for sorting an array of (x, cdf(x))
    * pairs by cdf(x).
    */
  private class BucketComparator extends java.util.Comparator[(Double, Double)] {
    // Compare the (label, cdf(label)) pairs by their labels
    def compare(left: (Double, Double), right: (Double, Double)): Int = {
      if (left._2 < right._2) -1
      else if (left._2 > right._2) 1
      else 0
    }
  }

  private val cmp = new BucketComparator

  /**
    * An implementation of the second part of the compound
    * transformation referred to in the citation given above.  The
    * idea is to transform from the source histogram to an equalized
    * one, then transform from the equalized one to the target one via
    * the inverse of the transformation that would equalize the target
    * one.
    */
  @inline private def transform(
    targetCdf: Array[(Double, Double)], fn: (Double => Double)
  )(x: Double): Double = {
    val cdfx = fn(x)
    val i = java.util.Arrays.binarySearch(targetCdf, (0.0, cdfx), cmp)

    if (i < 0) targetCdf(math.min(-(i+1), targetCdf.length-1))._1
    else targetCdf(i)._1
  }

  /**
    * Given a [[Tile]], a source
    * [[geotrellis.raster.histogram.Histogram]] (ostensibly that of
    * the tile), and a target histogram, this function produces a tile
    * whose histogram has been matched to the target histogram.
    *
    * @param  tile             The source tile
    * @param  sourceHistogram  The ostensible histogram of the tile
    * @param  targetHistogram  The target histogram of the output tile
    * @return                  The histogram-matched tile
    */
  def apply[T1 <: AnyVal, T2 <: AnyVal](
    tile: Tile,
    sourceHistogram: Histogram[T1],
    targetHistogram: Histogram[T2]
  ): Tile = {
    val cellType = tile.cellType
    val localIntensityToCdf = intensityToCdf(cellType, sourceHistogram.cdf)_
    val localTransform = transform(targetHistogram.cdf, localIntensityToCdf)_

    tile.mapDouble(localTransform)
  }

  /**
    * Given a [[Tile]] and a target
    * [[geotrellis.raster.histogram.Histogram]], this function
    * produces a tile whose histogram has been matched to the target
    * histogram.
    *
    * @param  tile             The source tile
    * @param  targetHistogram  The target histogram of the output tile
    * @return                  The histogram-matched tile
    */
  def apply[T <: AnyVal](tile: Tile, targetHistogram: Histogram[T]): Tile =
    HistogramMatching(
      tile,
      StreamingHistogram.fromTile(tile, 1<<17),
      targetHistogram
    )

  /**
    * Given a [[MultibandTile]], a sequence of source
    * [[geotrellis.raster.histogram.Histogram]] objects (ostensibly
    * those of the bands of the tile), and a sequence of target
    * histograms, this function produces a tile whose bands have been
    * respectively matched to the target histograms.
    *
    * @param  tile              The source tile
    * @param  sourceHistograms  The ostensible histograms of the bands of the tile
    * @param  targetHistograms  The target histograms for the bands of the output tile
    * @return                   The histogram-matched tile
    */
  def apply[T1 <: AnyVal, T2 <: AnyVal](
    tile: MultibandTile,
    sourceHistograms: Seq[Histogram[T1]],
    targetHistograms: Seq[Histogram[T2]]
  ): MultibandTile =
    MultibandTile(
      tile.bands
        .zip(sourceHistograms.zip(targetHistograms))
        .map({ case (tile, (source, target)) =>
          HistogramMatching(tile, source, target)
        })
    )

  /**
    * Given a [[MultibandTile]], and a sequence of target
    * [[geotrellis.raster.histogram.Histogram]] objects, this function
    * produces a tile whose bands have been respectively matched to
    * the target histograms.
    *
    * @param  tile              The source tile
    * @param  targetHistograms  The target histograms for the bands of the output tile
    * @return                   The histogram-matched tile
    */
  def apply[T <: AnyVal](
    tile: MultibandTile,
    targetHistograms: Seq[Histogram[T]]
  ): MultibandTile =
    MultibandTile(
      tile.bands
        .zip(targetHistograms)
        .map({ case (tile, target) =>
          HistogramMatching(
            tile,
            StreamingHistogram.fromTile(tile, 1<<17),
            target
          )
        })
    )
}
