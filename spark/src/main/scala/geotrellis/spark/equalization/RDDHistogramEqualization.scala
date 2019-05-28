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

package geotrellis.spark.equalization

import geotrellis.layers.Metadata
import geotrellis.raster._
import geotrellis.raster.equalization.HistogramEqualization
import geotrellis.raster.histogram.Histogram
import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.spark._
import org.apache.spark.rdd.RDD

import scala.reflect._


object RDDHistogramEqualization {

  private def r(left: Array[StreamingHistogram], right: Array[StreamingHistogram]) = {
    left.zip(right)
      .map({ case (l, r) => l + r })
  }

  /**
    * Given an RDD of Tile objects, return another RDD of tiles where
    * the respective tiles have had their histograms equalized the
    * joint histogram of all of the tiles.
    *
    * @param  rdd  An RDD of tile objects
    */
  def singleband[K, V: (? => Tile): ClassTag, M](
    rdd: RDD[(K, V)] with Metadata[M]
  ): RDD[(K, Tile)] with Metadata[M] = {
    val histogram = rdd
      .map({ case (_, tile: Tile) => StreamingHistogram.fromTile(tile, 1<<17)  })
      .reduce(_ + _)

    singleband(rdd, histogram)
  }

  /**
    * Given an RDD of Tile objects and a Histogram which summarizes
    * all of the tiles, return another RDD of tiles where the
    * respective tiles have had their histograms equalized.
    *
    * @param  rdd        An RDD of tile objects
    * @param  histogram  A histogram derived from the whole RDD of tiles
    */
  def singleband[K, V: (? => Tile): ClassTag, M, T <: AnyVal](
    rdd: RDD[(K, V)] with Metadata[M],
    histogram: Histogram[T]
  ): RDD[(K, Tile)] with Metadata[M] = {
    ContextRDD(
      rdd.map({ case (key, tile: Tile) =>
        (key, HistogramEqualization(tile, histogram)) }),
      rdd.metadata
    )
  }

  /**
    * Given an RDD of MultibandTile objects, return another RDD of
    * multiband tiles where the respective bands of the respective
    * tiles have been equalized according to a joint histogram of the
    * bands of the input RDD.
    *
    * @param  rdd  An RDD of multiband tile objects
    */
  def multiband[K, V: (? => MultibandTile): ClassTag, M](
    rdd: RDD[(K, V)] with Metadata[M]
  ): RDD[(K, MultibandTile)] with Metadata[M] = {
    val histograms = rdd
      .map({ case (_, tile: MultibandTile) =>
        tile.bands
          .map({ band => StreamingHistogram.fromTile(band, 1<<17) })
          .toArray })
      .reduce(r)
      .map(_.asInstanceOf[Histogram[Double]])

    multiband(rdd, histograms)
  }

  /**
    * Given an RDD of MultibandTile objects and a sequence of
    * Histogram objects (on per band) derived from all of the tiles,
    * return another RDD of multiband tiles where the respective bands
    * of the respective tiles have had their histograms equalized.
    *
    * @param  rdd         An RDD of tile objects
    * @param  histograms  A histogram derived from the whole RDD of tiles
    */
  def multiband[K, V: (? => MultibandTile): ClassTag, M, T <: AnyVal](
    rdd: RDD[(K, V)] with Metadata[M],
    histograms: Array[Histogram[T]]
  ): RDD[(K, MultibandTile)] with Metadata[M] = {
    ContextRDD(
      rdd.map({ case (key, tile: MultibandTile) =>
        (key, HistogramEqualization(tile, histograms)) }),
      rdd.metadata
    )
  }

}
