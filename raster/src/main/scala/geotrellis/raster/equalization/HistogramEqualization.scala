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
    * Comparison class for sorting an array of (label, cdf(label))
    * pairs by their label.
    */
  private class _cmp extends java.util.Comparator[(Double, Double)] {
    // Compare the (label, cdf(label)) pairs by their labels
    def compare(left: (Double, Double), right: (Double, Double)): Int = {
      if (left._1 < right._1) -1
      else if (left._1 > right._1) 1
      else 0
    }
  }

  private val cmp = new _cmp()

  /**
    * An implementation of the transformation T referred to in the
    * citation given above.
    */
  private def _T(cellType: CellType, cdf: Array[(Double, Double)])(x: Double): Double = {
    val i = java.util.Arrays.binarySearch(cdf, (x, 0.0), cmp)
    val bits = cellType.bits
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
        val j = (-1 * i - 2)
        val cdf0 = cdf(j+0)._2
        val cdf1 = cdf(j+1)._2
        val t = (x - cdf0) / (cdf1 - cdf0)
        (1.0-t)*cdf0 + t*cdf1
      }
    val normalizedCdf = math.max(0.0, math.min(1.0, (rawCdf - smallestCdf) / (1.0 - smallestCdf)))

    cellType match {
      case _: FloatCells => (Float.MaxValue * (2*normalizedCdf - 1.0))
      case _: DoubleCells => (Double.MaxValue * (2*normalizedCdf - 1.0))
      case _: BitCells | _: UByteCells | _: UShortCells =>
        ((1<<bits) - 1) * normalizedCdf
      case _: ByteCells | _: ShortCells | _: IntCells =>
        (((1<<bits) - 1) * normalizedCdf) - (1<<(bits-1))
    }
  }

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
    * Given a [[Tile]] and a [[Histogram[T]]], return a Tile with an
    * equalized histogram.
    *
    * @param  tile       A singleband tile
    * @param  histogram  The histogram of the tile
    * @return            A singleband tile with improved contrast
    */
  def apply[T <: AnyVal](tile: Tile, histogram: Histogram[T]): Tile = {
    val T = _T(tile.cellType, histogram.cdf)_
    tile.mapDouble(T)
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
    * Given a [[MultibandTile]] and a [[Histogram[T]]] for each of its
    * bands, return a MultibandTile whose bands all have equalized
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
