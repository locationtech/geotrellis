package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis._
import geotrellis.statistics._
import geotrellis.logic.{Reducer1,Reducer2}
import geotrellis.process._

/**
 * Contains several different operations for building a histograms of a raster.
 *
 * @note     Rasters with a double type (TypeFloat,TypeDouble) will have their values
 *           rounded to integers when making the Histogram.
 */
object GetHistogram {
  def apply(r:Op[Raster]):Op[Histogram] = GetHistogramMap(r)
  def apply(r:Op[Raster], size:Op[Int]):Op[Histogram] = GetHistogramMap(r)
}

/**
 * Implements a histogram in terms of a map.
 *
 * @note     Rasters with a double type (TypeFloat,TypeDouble) will have their values
 *           rounded to integers when making the Histogram.
 */
case class GetHistogramMap(r:Op[Raster]) extends Reducer1(r)({
  r => FastMapHistogram.fromRaster(r.force)
})({
  hs => FastMapHistogram.fromHistograms(hs)
})

/**
 * Implements a histogram in terms of an array of the given size.
 *
 * @note     Rasters with a double type (TypeFloat,TypeDouble) will have their values
 *           rounded to integers when making the Histogram.
 */
case class GetHistogramArray(r:Op[Raster], n:Op[Int]) extends Reducer2(r, n)({
  (r, n) => ArrayHistogram.fromRaster(r.force, n)
})({
  (hs, n) => ArrayHistogram.fromHistograms(hs, n)
})


/**
 * Create a histogram from double values in a raster.
 *
 * FastMapHistogram only works with integer values, which is great for performance
 * but means that, in order to use FastMapHistogram with double values, each value 
 * must be multiplied by a power of ten to preserve significant fractional digits.
 *
 * For example, if you want to save one significant digit (2.1 from 2.123), set
 * sigificantDigits to 1, and the histogram will save 2.1 as "21" by multiplying
 * each value by 10.  The multiplier is 10 raised to the power of significant digits
 * (e.g. 1 digit: 10, 2 digits: 100, and so on).
 *
 * Important: Be sure that the maximum value in the rater multiplied by 
 *            10 ^ significantDigits does not overflow Int.MaxValue (2,147,483,647). 
 *
 * @param r                   Raster to create histogram from
 * @param significantDigits   Number of significant digits to preserve by multiplying 
 */ 
case class GetDoubleHistogram(r:Op[Raster], significantDigits:Int) extends Reducer1(r)({
  r => FastMapHistogram.fromRasterDouble(r.force, significantDigits)
})({
  hs => FastMapHistogram.fromHistograms(hs)
})

