/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.services

import geotrellis._
import geotrellis.source._
import geotrellis.process.LayerId
import geotrellis.render.ColorRamps._
import geotrellis.util.srs

import Json._

object LayerService {
  def getInfo(layer:LayerId):ValueSource[String] =
    RasterSource(layer)
      .info
      .map { info =>
        s"""{
            "name" : "${info.id.name}",
            "rasterExtent" : ${info.rasterExtent.toJson},
            "datatype" :" ${info.rasterType}"
           }"""
       }

  def getBreaks(layer:LayerId,numBreaks:Int):ValueSource[String] =
      RasterSource(layer)
        .classBreaks(numBreaks)
        .map (Json.classBreaks(_))

  /** Gets the raster's extent in lat long coordinates (assuming it's in Web Mercator) */
  def getBoundingBox(layer:LayerId):ValueSource[String] =
    RasterSource(layer)
      .info
      .map(_.rasterExtent.extent)
      .map(extent => srs.WebMercator.transform(extent,srs.LatLng))
      .map { extent => 
        s"""{"latmin" : "${extent.ymin}",
             "latmax" : "${extent.ymax}",
             "lngmin" : "${extent.xmin}",
             "lngmax" : "${extent.xmax}"}"""
       }

  def render(
    bbox:String,
    cols:Int,
    rows:Int,
    layer:LayerId,
    breaksString:String,
    colorRampKey:String
  ):ValueSource[Array[Byte]] = {
    val extent = Extent.fromString(bbox)

    val breaks = breaksString.split(",").map(_.toInt)

    render(RasterExtent(extent, cols, rows),layer,breaks,colorRampKey)
  }

  def render(
    rasterExtent:RasterExtent,
    layer:LayerId,
    breaks:Array[Int],
    colorRampKey:String
  ):ValueSource[Array[Byte]] = {
    val ramp = {
      val cr = ColorRampMap.getOrElse(colorRampKey,BlueToRed)
      if(cr.toArray.length < breaks.length) { cr.interpolate(breaks.length) }
      else { cr }
    }

    RasterSource(layer,rasterExtent)
      .renderPng(ramp,breaks)
  }
}
