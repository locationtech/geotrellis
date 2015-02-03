/*
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
 */

package geotrellis.raster.io.geotiff

private[geotiff] case class Nodata(value: Double, isSet: Boolean) {
  /**
    * Int nodata value to use when writing raster.
    */
  def toInt(settings: Settings): Int = (settings.format, settings.nodata) match {
    case (Signed | Unsigned, Nodata(d, true)) => d.toInt
    case (Signed, Nodata(_, false))           => (1L << (settings.size.bits - 1)).toInt
    case (Unsigned, Nodata(_, false))         => ((1L << settings.size.bits) - 1).toInt
    case (Floating, _)                        => sys.error("floating point not supported")
  }

  /**
    * String nodata value to use in GeoTIFF metadata.
    */
  def toString(settings: Settings): String = (settings.format, settings.nodata) match {
    /* first two cases represent when nodata value was set (NoData.isSet = true) */
    case (Signed | Unsigned, Nodata(d, true)) => d.toLong.toString
    case (Floating, Nodata(d, true)) => Double.MinValue.toString
    /* next cases represent when nodata value was not set (NoData.isSet = false) */
    case (Signed, Nodata(_, false))   => "-" + (1L << (settings.size.bits - 1)).toString
    case (Unsigned, Nodata(_, false)) => ((1L << settings.size.bits) - 1).toString
    case (Floating, Nodata(_, false)) => Double.MinValue.toString
  }
}

object Nodata {
  val Default = Nodata(Double.NaN, false)
}
