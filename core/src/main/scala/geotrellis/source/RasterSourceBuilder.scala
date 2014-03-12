/**************************************************************************
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
 **************************************************************************/

package geotrellis.source
  
import geotrellis._
import geotrellis.raster._
import geotrellis.process.LayerId

class RasterSourceBuilder extends SourceBuilder[Raster,RasterSource] {
  private var _dataDefinition:Op[RasterDefinition] = null
  private var _ops:Op[Seq[Op[Raster]]] = null

  def setOp(op: Op[Seq[Op[Raster]]]): this.type = {
    _ops = op
    this 
  }

  def setRasterDefinition(dfn: Op[RasterDefinition]): this.type = {
    this._dataDefinition = dfn
    this
  }

  def result = new RasterSource(_dataDefinition,_ops)
}

object RasterSourceBuilder {
  def apply(rasterSource:RasterSource) = {
    val builder = new RasterSourceBuilder()
    builder.setRasterDefinition(rasterSource.rasterDefinition)
  }
}

/** Builder for a RasterSource where the RasterDefinition is not known,
  * and requires evaluation of the single Raster inside the tiles.
  * Should only be used for in memory rasters, i.e. mapping a ValueSource
  * to a RasterSource.
  */
class BareRasterSourceBuilder extends SourceBuilder[Raster,RasterSource] {
  private var _dataDefinition:Op[RasterDefinition] = null
  private var _ops:Op[Seq[Op[Raster]]] = null

  def setOp(op: Op[Seq[Op[Raster]]]): this.type = {
    _ops = op
    this
  }

  def setRasterDefinition(dfn: Op[RasterDefinition]): this.type = 
    sys.error("Shouldn't be setting the RasterDefinition of a BareRasterSourceBuilder, use RasterSourceBuilder instead.")

  def result = {
    val rasterDefinition = 
      for(seq <- _ops;
          r <- seq.head) yield {
        RasterDefinition.fromRaster(r)
      }
    new RasterSource(rasterDefinition,_ops)
  }
}
