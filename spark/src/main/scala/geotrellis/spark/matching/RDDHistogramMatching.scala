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

package geotrellis.spark.matching

import geotrellis.raster._
import geotrellis.raster.histogram._
import geotrellis.raster.matching.HistogramMatching
import geotrellis.spark._
import geotrellis.spark.summary._

import org.apache.spark.rdd.RDD


object RDDHistogramMatching {

  private def computeHistogram[K, V: (? => Tile)](rdd: RDD[(K, V)]): StreamingHistogram =
    rdd
      .map({ case (_, v) => StreamingHistogram.fromTile(v, 1<<8) })
      .reduce(_ + _)

  private def computeHistograms[K, V: (? => MultibandTile)](
    rdd: RDD[(K, V)],
    bandCount: Int
  ): Seq[StreamingHistogram] =
    (0 until bandCount).map({ i =>
      rdd.map({ case (_, v) => StreamingHistogram.fromTile(v.bands(i), 1<<8) })
        .reduce(_ + _)
    })

  def singleband[T1 <: AnyVal, T2 <: AnyVal, K, V: (? => Tile)](
    rdd: RDD[(K, V)],
    sourceHistogram: Histogram[T1],
    targetHistogram: Histogram[T2]
  ): RDD[(K, Tile)] =
    rdd.map({ case (key, tile) =>
      (key, HistogramMatching(tile, sourceHistogram, targetHistogram)) })

  def singleband[T <: AnyVal, K, V: (? => Tile)](
    rdd: RDD[(K, V)],
    targetHistogram: Histogram[T]
  ): RDD[(K, Tile)] = {
    val sourceHistogram = computeHistogram(rdd)
    singleband(rdd, sourceHistogram, targetHistogram)
  }

  def multiband[T1 <: AnyVal, T2 <: AnyVal, K, V: (? => MultibandTile)](
    rdd: RDD[(K, V)],
    sourceHistograms: Seq[Histogram[T1]],
    targetHistograms: Seq[Histogram[T2]]
  ): RDD[(K, MultibandTile)] =
    rdd.map({ case (key, tile: MultibandTile) =>
      (key, HistogramMatching(tile, sourceHistograms, targetHistograms)) })

  def multiband[T <: AnyVal, K, V: (? => MultibandTile)](
    rdd: RDD[(K, V)],
    targetHistograms: Seq[Histogram[T]]
  ): RDD[(K, MultibandTile)] = {
    val sourceHistograms = computeHistograms(rdd, targetHistograms.length)
    multiband(rdd, sourceHistograms, targetHistograms)
  }

}
