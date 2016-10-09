package geotrellis.spark.equalization

import geotrellis.raster._
import geotrellis.raster.equalization.HistogramEqualization
import geotrellis.raster.histogram.StreamingHistogram
import geotrellis.spark._

import org.apache.spark.rdd.RDD

import scala.reflect._


object RDDHistogramEqualization {

  private def r(left: Array[StreamingHistogram], right: Array[StreamingHistogram]) = {
    left.zip(right)
      .map({ case (l, r) => l + r })
  }

  def singleband[K, V: (? => Tile): ClassTag, M](
    rdd: RDD[(K, V)] with Metadata[M]
  ): RDD[(K, Tile)] with Metadata[M] = {
    val histogram = rdd
      .map({ case (_, tile: Tile) => StreamingHistogram.fromTile(tile, 1<<17)  })
      .reduce(_ + _)

    singleband(rdd, histogram)
  }

  def singleband[K, V: (? => Tile): ClassTag, M](
    rdd: RDD[(K, V)] with Metadata[M],
    histogram: StreamingHistogram
  ): RDD[(K, Tile)] with Metadata[M] = {
    ContextRDD(
      rdd.map({ case (key, tile: Tile) =>
        (key, HistogramEqualization(tile, histogram)) }),
      rdd.metadata
    )
  }

  def multiband[K, V: (? => MultibandTile): ClassTag, M](
    rdd: RDD[(K, V)] with Metadata[M]
  ): RDD[(K, MultibandTile)] with Metadata[M] = {
    val histograms = rdd
      .map({ case (_, tile: MultibandTile) =>
        tile.bands
          .map({ band => StreamingHistogram.fromTile(band, 1<<17) })
          .toArray })
      .reduce(r)

    multiband(rdd, histograms)
  }

  def multiband[K, V: (? => MultibandTile): ClassTag, M](
    rdd: RDD[(K, V)] with Metadata[M],
    histograms: Array[StreamingHistogram]
  ): RDD[(K, MultibandTile)] with Metadata[M] = {
    ContextRDD(
      rdd.map({ case (key, tile: MultibandTile) =>
        (key, HistogramEqualization(tile, histograms)) }),
      rdd.metadata
    )
  }

}
