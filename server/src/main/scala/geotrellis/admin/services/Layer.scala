package geotrellis.admin.services

import javax.servlet.http.HttpServletRequest
import javax.ws.rs._
import javax.ws.rs.core.{Response, Context, MediaType, MultivaluedMap}
import geotrellis._
import geotrellis.admin._
import geotrellis.admin.Json._
import geotrellis.raster.op._
import geotrellis.statistics.op._
import geotrellis.rest._
import geotrellis.rest.op._
import geotrellis.raster._
import geotrellis.feature._
import geotrellis.feature.op.geometry.AsPolygonSet
import geotrellis.feature.rasterize.{Rasterizer, Callback}
import geotrellis.data.ColorRamps._

import scala.collection.JavaConversions._

@Path("/admin/layer")
class Layer {
  @GET
  @Path("/info")
  def render(
    @DefaultValue("") @QueryParam("layer") layer:String,
    @Context req:HttpServletRequest
  ):Response = {
    val r = io.LoadRasterExtent(layer);
    GeoTrellis.run(r) match {
      case process.Complete(rasterExtent,h) =>
        OK.json(s"""{
          "name" : "${layer}",
          "rasterExtent" : ${rasterExtent.toJson}
         }""")
           .allowCORS()
      case process.Error(message,failure) =>
        ERROR(message,failure)
    }
  }

  @GET
  @Path("/render")
  def render(
    @DefaultValue("") @QueryParam("bbox") bbox:String,
    @DefaultValue("256") @QueryParam("cols") cols:String,
    @DefaultValue("256") @QueryParam("rows") rows:String,
    @DefaultValue("") @QueryParam("layer") layer:String,
    @DefaultValue("") @QueryParam("palette") palette:String,
    @DefaultValue("4") @QueryParam("colors") numColors:String,
    @DefaultValue("image/png") @QueryParam("format") format:String,
    @DefaultValue("") @QueryParam("breaks") breaks:String,
    @DefaultValue("blue-to-red") @QueryParam("colorRamp") colorRampKey:String,
    @Context req:HttpServletRequest
  ):Response = {
    val extentOp = string.ParseExtent(bbox)

    val colsOp = string.ParseInt(cols)
    val rowsOp = string.ParseInt(rows)

    val reOp = extent.GetRasterExtent(extentOp, colsOp, rowsOp)

    val layerOp = io.LoadRaster(layer,reOp).map { r =>
      // Convert 0 of bit raster to NODATA
      if(r.data.getType == TypeBit) { r.convert(TypeByte).map { z => if(z == 0) NODATA else z } }
      else { r }
    }                                                 

 
    val breaksOp = 
      logic.ForEach(string.SplitOnComma(breaks))(string.ParseInt(_))
    
    val ramp = breaksOp.map { b => 
      val cr = Colors.rampMap.getOrElse(colorRampKey,BlueToRed)
      if(cr.toArray.length < b.length) { cr.interpolate(b.length) }
      else { cr }
    }

    val png = Render(layerOp,ramp,breaksOp)

    GeoTrellis.run(png) match {
      case process.Complete(img,h) =>
        OK.png(img)
          .cache(1000)
      case process.Error(message,failure) =>
        ERROR(message,failure)
    }
  }

  @GET
  @Path("/valuegrid")
  def render(
    @DefaultValue("") @QueryParam("layer") layer:String,
    @DefaultValue("") @QueryParam("lat") lat:String,
    @DefaultValue("") @QueryParam("lng") lng:String,
    @DefaultValue("7") @QueryParam("size") size:String,
    @Context req:HttpServletRequest
  ):Response = {
    val lngOp = string.ParseDouble(lat)
    val latOp = string.ParseDouble(lng)
    val sizeOp = string.ParseInt(size)

    val layerOp = io.LoadRaster(layer)

    val op = for(rast <- layerOp;
                 lat  <- latOp;
                 lng  <- lngOp;
                 size <- sizeOp) yield {
      val rp = Reproject(Point(lng,lat,0), Projections.LatLong, Projections.WebMercator)
                        .asInstanceOf[Point[Int]]
                        .geom
      val x = rp.getX
      val y = rp.getY

      val s = size / 2
      val (col,row) = rast.rasterExtent.mapToGrid(x,y)
      for(r <- (row - s) to (row + s);
          c <- (col - s) to (col + s)) yield {
        if(0 <= c && c <= rast.cols &&
           0 <= r && r <= rast.rows) {
          "\"%.2f\"".format(rast.getDouble(c,r))
        } else {
          "\"\""
        }
      }
    }
 
    GeoTrellis.run(op) match {
      case process.Complete(values,h) =>
        val data = s""" { "success" : "1", "values" : [ ${values.mkString(",")} ] } """
        OK.json(data)
      case process.Error(message,failure) =>
        ERROR(message,failure)
    }
  }
}
