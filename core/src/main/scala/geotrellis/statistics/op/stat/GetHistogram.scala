/***
 * Copyright (c) 2014 Azavea.
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
 ***/

package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.statistics._ 
import geotrellis.process._

/**
 * Contains several different operations for building a histograms of a raster.
 *
 * @note     Rasters with a double type (TypeFloat,TypeDouble) will have their values
 *           rounded to integers when making the Histogram.
 */
object GetHistogram {
  def apply(r:Op[Raster]) = 
    r.map(FastMapHistogram.fromRaster(_))
     .withName("GetHistogram")
}

/**
 * Implements a histogram in terms of an array of the given size.
 * The size provided must be less than or equal to the number of distinct 
 * values in the Raster.
 *
 * @note     Rasters with a double type (TypeFloat,TypeDouble) will have their values
 *           rounded to integers when making the Histogram.
 */
object GetArrayHistogram {
  def apply(r:Op[Raster],size:Int) = 
    r.map(ArrayHistogram.fromRaster(_,size))
     .withName("GetArrayHistogram")
}

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
object GetDoubleHistogram {
  def apply(r:Op[Raster], significantDigits:Int) = 
    r.map(FastMapHistogram.fromRasterDouble(_,significantDigits))
     .withName("GetDoubleHistogram")
}
