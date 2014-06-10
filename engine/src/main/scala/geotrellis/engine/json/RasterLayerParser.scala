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

package geotrellis.engine.json

import geotrellis.raster._
import geotrellis.engine._

import com.typesafe.config.ConfigFactory

object RasterLayerParser {
  def parseType(s:String):CellType = s match {
    case "bool" => TypeBit
    case "int8" => TypeByte
    case "int16" => TypeShort
    case "int32" => TypeInt
    case "float32" => TypeFloat
    case "float64" => TypeDouble
    case s => sys.error("unsupported datatype '%s'" format s)
  }

  def apply(jsonString:String,path:String = "") = {
        val json = ConfigFactory.parseString(jsonString)
        val layerType = json.getString("type").toLowerCase

        Catalog.getRasterLayerBuilder(layerType) match {
          case Some(builder) => builder(path,json)
          case None => 
            throw new java.io.IOException(s"Raster layer defined at $path has raster layer type $layerType " +
                                           "for which this catalog has no builder.")
        }
  }
}
