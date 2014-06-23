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

package geotrellis.raster.op.zonal

import geotrellis._
import geotrellis.process._
import geotrellis.statistics._

import spire.syntax.cfor._

import scala.collection.mutable

/**
 * Given a raster, return a histogram summary of the cells within each zone.
 *
 * @note    ZonalHistogram does not currently support Double raster data.
 *          If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *          the data values will be rounded to integers.
 */
case class ZonalHistogram(data: Op[Raster], zones: Op[Raster]) 
     extends Op2(data, zones) ({
  (raster, zones) => {
    val histMap = mutable.Map[Int,FastMapHistogram]()

    val rows  = raster.rasterExtent.rows
    val cols  = raster.rasterExtent.cols

    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val v = raster.get(col,row)
        val z = zones.get(col,row)
        if(!histMap.contains(z)) { histMap(z) = FastMapHistogram() }
        histMap(z).countItem(v)
      }
    }

    Result(histMap.toMap)
  }
})
