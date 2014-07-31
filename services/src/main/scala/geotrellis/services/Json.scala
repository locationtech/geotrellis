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

package geotrellis.services

import geotrellis.raster._
import geotrellis.engine._

import scala.language.existentials
import scala.language.implicitConversions

trait JsonWrapper { def toJson(): String }

object Json {
  def wrap(json: => String) = new JsonWrapper { def toJson() = json }

  implicit def RasterExtentToJson(re: RasterExtent): JsonWrapper = wrap {
    val latLong = re.extent
    s"""{
          "cols" : "${re.cols}",
          "rows" : "${re.rows}",
          "bbox" : ["${re.extent.xmin}","${re.extent.ymin}","${re.extent.xmax}","${re.extent.ymax}"],
          "cellwidth" : "${re.cellwidth}",         
          "cellheight" : "${re.cellheight}",
          "latlong" : {
              "latmin" : "${latLong.ymin}",
              "longmin" : "${latLong.xmin}",
              "latmax" : "${latLong.ymax}",
              "longmax" : "${latLong.xmax}"
          }
        }"""
  }

  implicit def CatalogToJson(catalog: Catalog): JsonWrapper = wrap {
    s"""{   "name" :  "${catalog.name}",
            "stores" : [ """ + 
                   (for(store <- catalog.stores.values) yield {
                          s"""{ "name" :  "${store.name}",""" +
                          s"""  "layers" : [""" +
                          store.getNames.toList.sorted.map(x => s""" "$x\" """).mkString(",") +
                                   "] }" 
                   }).mkString(",") +
                     """]
        }"""
  }

  def classBreaks(breaks: Array[Int]) = 
    s"""{ "classBreaks" : ${breaks.mkString("[", ",", "]")} }"""
}
