package geotrellis.admin

import org.codehaus.jackson._
import org.codehaus.jackson.JsonToken._
import org.codehaus.jackson.map._

import geotrellis._
import geotrellis.process._
import geotrellis.io.LoadGeoJson
import geotrellis.feature._

import scala.language.existentials
import scala.language.implicitConversions

trait JsonWrapper { def toJson():String }

object Json {
  def wrap(json: => String) = new JsonWrapper { def toJson() = json }

  implicit def RasterExtentToJson(re:RasterExtent):JsonWrapper = wrap {
    var latLong = Reproject(re.extent,Projections.WebMercator, Projections.LatLong)
    s"""{
          "cols" : "${re.cols}",
          "rows" : "${re.rows}",
          "bbox" : ["${re.extent.xmin}","${re.extent.ymin}","${re.extent.xmax}","${re.extent.ymax}"],
          "cellwidth" : "${re.cellwidth}",         
          "cellheight" : "${re.cellheight}",
          "latlong" : {
              "latmin" : "${latLong.xmin}",
              "longmin" : "${latLong.ymin}",
              "latmax" : "${latLong.xmax}",
              "longmax" : "${latLong.ymax}"
          }
        }"""
  }

  implicit def CatalogToJson(catalog:Catalog):JsonWrapper = wrap {
    s"""{   "name" :  "${catalog.name}",
            "stores" : [ """ + 
                        {for(store <- catalog.stores.values) yield {
                      s"""{ "name" :  "${store.name}",""" +
                      s"""  "layers" : [""" +
                             (store.getNames.map(x => s""" "$x\" """)).mkString(",") +
                                   "] }" 
                              } }.mkString(",") +
                     """]
        }"""
  }

  def classBreaks(breaks:Array[Int]) = 
    s"""{ "classBreaks" : ${breaks.mkString("[", ",", "]")} }"""
}
