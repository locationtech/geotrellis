/*
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
 */

package geotrellis.spark.cmd

import geotrellis._
import geotrellis.raster.MutableRasterData
import geotrellis.raster.RasterData
import spire.syntax.cfor._

object NoDataHandler {

  def removeUserNoData(rd: MutableRasterData, userNoData: Double): Unit = {
    /* 
     * This handles all types of RasterData - e.g., FloatArrayRasterData, ByteArrayRasterData
     * because the apply/update methods handle conversion of NODATA to the appropriate types
     * via macros i2f, i2b, respectively 
     */
    cfor(0)(_ < rd.length, _ + 1) {i =>
      if (rd(i) == userNoData) rd(i) = NODATA
    }
  }

  def addUserNoData(rd: MutableRasterData, userNoData: Double): Unit = {
    /* 
     * This handles all types of RasterData - e.g., FloatArrayRasterData, ByteArrayRasterData
     * because of the updateDouble vs. update call
     */
    cfor(0)(_ < rd.length, _ + 1) {i =>
      if (isNoData(rd(i))) {
        if(rd.isFloat)
          rd.updateDouble(i, userNoData)
        else
          rd.update(i, userNoData.toInt)
        
      } 
    }
  }
}