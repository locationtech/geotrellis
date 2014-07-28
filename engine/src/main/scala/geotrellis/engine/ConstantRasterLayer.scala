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

package geotrellis.engine

import geotrellis._
import geotrellis.raster._

import com.typesafe.config.Config

object ConstantRasterLayerBuilder extends RasterLayerBuilder {
  def apply(ds: Option[String], jsonPath: String, json: Config): RasterLayer = {
    val cols = json.getInt("cols")
    val rows = json.getInt("rows")

    val (cellWidth, cellHeight) = getCellWidthAndHeight(json)
    val rasterExtent = RasterExtent(getExtent(json), cellWidth, cellHeight, cols, rows)

    val cellType = getCellType(json)
    val info = 
      RasterLayerInfo(
        LayerId(ds, getName(json)),
        getCellType(json),
        rasterExtent,
        getEpsg(json),
        getXskew(json),
        getYskew(json)
      )

    if(cellType.isFloatingPoint) {
      new DoubleConstantLayer(info, json.getDouble("constant"))
    } else {
      new IntConstantLayer(info, json.getInt("constant"))
    }
  }
}

class IntConstantLayer(info: RasterLayerInfo, value: Int) 
extends UntiledRasterLayer(info) {
  def getRaster(targetExtent: Option[RasterExtent] = None) = {
    val re = targetExtent match {
      case Some(rext) => rext
      case None => info.rasterExtent
    }
    IntConstantTile(value, re.cols, re.rows)
  }

  def cache(c: Cache[String]) = {} // No-op
}

class DoubleConstantLayer(info: RasterLayerInfo, value: Double) 
extends UntiledRasterLayer(info) {
  def getRaster(targetExtent: Option[RasterExtent]) = {
    val re = targetExtent match {
      case Some(rext) => rext
      case None => info.rasterExtent
    }
    DoubleConstantTile(value, re.cols, re.rows)
  }

  def cache(c: Cache[String]) = {} // No-op
}

