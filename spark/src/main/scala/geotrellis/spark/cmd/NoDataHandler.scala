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

import geotrellis.raster._
import spire.syntax.cfor._

object NoDataHandler {

  def removeUserNoData(tile: MutableArrayTile, userNoData: Double): Unit = {
    /* 
     * This handles all types of RasterData - e.g., FloatArrayTile, ByteArrayTile
     * because the apply/update methods handle conversion of NODATA to the appropriate types
     * via macros i2f, i2b, respectively 
     */
    cfor(0)(_ < tile.size, _ + 1) {i =>
      if (tile(i) == userNoData) tile(i) = NODATA
    }
  }

  def addUserNoData(tile: MutableArrayTile, userNoData: Double): Unit = {
    /* 
     * This handles all types of RasterData - e.g., FloatArrayTile, ByteArrayTile
     * because of the updateDouble vs. update call
     */
    cfor(0)(_ < tile.size, _ + 1) {i =>
      if (isNoData(tile(i))) {
        if(tile.cellType.isFloatingPoint)
          tile.updateDouble(i, userNoData)
        else
          tile.update(i, userNoData.toInt)
        
      } 
    }
  }
}
