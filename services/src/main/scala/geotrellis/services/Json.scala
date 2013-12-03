package geotrellis.service

import geotrellis._
import geotrellis.process._

import scala.language.existentials
import scala.language.implicitConversions

trait JsonWrapper { def toJson():String }

object Json {
  def wrap(json: => String) = new JsonWrapper { def toJson() = json }

  implicit def RasterExtentToJson(re:RasterExtent):JsonWrapper = wrap {
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

  implicit def CatalogToJson(catalog:Catalog):JsonWrapper = wrap {
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

  def classBreaks(breaks:Array[Int]) = 
    s"""{ "classBreaks" : ${breaks.mkString("[", ",", "]")} }"""
}
