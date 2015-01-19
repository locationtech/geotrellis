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

package geotrellis.raster.io.geotiff.reader.extensions

trait GDALNoDataParser {

  val gdalNoDataNaNs = List("-1.#QNAN", "1.#QNAN", "-1.#IND", "nan")
  val gdalNoDataNegInfinity = List("-inf", "-1.#INF")
  val gdalNoDataInfinity = List("inf", "1.#INF")

  def parseGDALNoDataString(input: String): Option[Double] = {
    val noDataString = input.replace(',', '.').filter(_ != ' ').filter(_ != '\0')

    if (gdalNoDataNaNs.contains(noDataString)) Some(Double.NaN)
    else if (gdalNoDataNegInfinity.contains(noDataString)) Some(Double.MinValue)
    else if (gdalNoDataInfinity.contains(noDataString)) Some(Double.MaxValue)
    else {
      try Some(noDataString.toDouble)
      catch {
        case e: NumberFormatException => None
      }
    }
  }

}
