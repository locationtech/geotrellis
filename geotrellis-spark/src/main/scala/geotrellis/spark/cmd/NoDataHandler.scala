/*******************************************************************************
 * Copyright (c) 2014 DigitalGlobe.
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
 ******************************************************************************/

package geotrellis.spark.cmd

import geotrellis._
import geotrellis.raster.MutableRasterData
import geotrellis.raster.RasterData

import scalaxy.loops._

object NoDataHandler {

  def removeUserNoData(rd: MutableRasterData, userNoData: Double): MutableRasterData = {
    /* 
     * This handles all types of RasterData - e.g., FloatArrayRasterData, ByteArrayRasterData
     * because the apply/update methods handle conversion of NODATA to the appropriate types
     * via macros i2f, i2b, respectively 
     */
    for (i <- 0 until rd.length optimized) {
      if (rd(i) == userNoData) rd(i) = NODATA
    }
    rd
  }

  def removeGeotrellisNoData(rd: MutableRasterData, userNoData: Double): RasterData = {
    /* 
     * This handles all types of RasterData - e.g., FloatArrayRasterData, ByteArrayRasterData
     * because the scala will convert the raw types to either Double or Int as per the argument
     * of the anonymous function
     */
    if (rd.isFloat)
      rd.mapDouble((i: Double) => if (isNoData(i)) userNoData else i)
    else
      rd.map((i: Int) => if (isNoData(i)) userNoData.toInt else i)
  }
}