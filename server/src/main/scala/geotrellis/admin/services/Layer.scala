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
  ) = {
    val r = io.LoadRasterExtent(layer);
    Main.run(r) match {
      case process.Complete(rasterExtent,h) =>
        OK.json(s"""{
          "name" : "${layer}",
          "rasterExtent" : ${rasterExtent.toJson}
         }""")
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
  ) = {
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

    Main.run(png) match {
      case process.Complete(img,h) =>
        OK.png(img)
      case process.Error(message,failure) =>
        ERROR(message,failure)
    }
  }
}
