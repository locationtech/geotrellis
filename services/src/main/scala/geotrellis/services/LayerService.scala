package geotrellis.service

import geotrellis._
import geotrellis.source._
import geotrellis.process.LayerId
import geotrellis.render.ColorRamps._

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

  def render(
    bbox:String,
    cols:Int,
    rows:Int,
    layer:LayerId,
    breaksString:String,
    colorRampKey:String
  ):ValueSource[Array[Byte]] = {
    val extent = {
      val Array(xmin,ymin,xmax,ymax) = bbox.split(",").map(_.toDouble)
      Extent(xmin,ymin,xmax,ymax)
    }

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

    RasterSource(layer)
      .renderPng(ramp,breaks)
  }

  // @GET
  // @Path("/valuegrid")
  // def render(
  //   @DefaultValue("") @QueryParam("layer") layer:String,
  //   @DefaultValue("") @QueryParam("lat") lat:String,
  //   @DefaultValue("") @QueryParam("lng") lng:String,
  //   @DefaultValue("7") @QueryParam("size") size:String,
  //   @Context req:HttpServletRequest
  // ):Response = {
  //   val latOp = string.ParseDouble(lat)
  //   val lngOp = string.ParseDouble(lng)
  //   val sizeOp = string.ParseInt(size)

  //   val layerOp = io.LoadRaster(layer)

  //   val op = for(rast <- layerOp;
  //                lat  <- latOp;
  //                lng  <- lngOp;
  //                size <- sizeOp) yield {
  //     val (x,y) = srs.LatLng.transform(lng,lat,srs.WebMercator)
  //     val s = size / 2
  //     val (col,row) = rast.rasterExtent.mapToGrid(x,y)
  //     for(r <- (row - s) to (row + s);
  //         c <- (col - s) to (col + s)) yield {
  //       if(0 <= c && c <= rast.cols &&
  //          0 <= r && r <= rast.rows) {
  //         "\"%.2f\"".format(rast.getDouble(c,r))
  //       } else {
  //         "\"\""
  //       }
  //     }
  //   }
 
  //   GeoTrellis.run(op) match {
  //     case process.Complete(values,h) =>
  //       val data = s""" { "success" : "1", "values" : [ ${values.mkString(",")} ] } """
  //       OK.json(data)
  //     case process.Error(message,failure) =>
  //       ERROR(message,failure)
  //   }
  // }
}
