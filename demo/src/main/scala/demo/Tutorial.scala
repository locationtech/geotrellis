package demo

import javax.servlet.http.{HttpServletRequest}
import javax.ws.rs.{GET, Path, DefaultValue, PathParam, QueryParam}
import javax.ws.rs.core.{Response, Context}

import geotrellis._
import geotrellis.data._
import geotrellis.data.ColorRamps._
import geotrellis.statistics.{Histogram}
import geotrellis.process.{Server}
import geotrellis.Implicits._

import geotrellis.raster.op._
import geotrellis.rest.op._
import geotrellis.statistics.op._


@Path("/greeting")
class HelloWorld {
  @GET
  def get(@Context req:HttpServletRequest) = {
    // give a friendly greeting
    response("text/plain")("hello world")
  }
}

@Path("/adder")
class AddOne {
  @GET
  @Path("/{x}")
  def get(@PathParam("x") s:String,
          @Context req:HttpServletRequest) = {
    // parse the given integer
    val opX:Op[Int] = string.ParseInt(s)

    // add one
    val opY:Op[Int] = opX + 1

    // run the operation
    val data:String = try {
      val y:Int = Demo.server.run(opY)
      y.toString
    } catch {
      case e:Throwable => e.toString
    }

    response("text/plain")(data)
  }
}

@Path("/bbox")
class BoundingBox {
  @GET
  @Path("/{extent1}/union/{extent2}")
  def get(@PathParam("extent1") s1:String,
          @PathParam("extent2") s2:String,
          @Context req:HttpServletRequest) = {
    // parse the given extents
    val extent1 = {
      val Array(xmin,ymin,xmax,ymax) = s1.split(",").map(_.toDouble)
      Extent(xmin,ymin,xmax,ymax)
    }

    val extent2 = {
      val Array(xmin,ymin,xmax,ymax) = s2.split(",").map(_.toDouble)
      Extent(xmin,ymin,xmax,ymax)
    }

    val combined = extent1.combine(extent2)

    response("text/plain")(combined.toString)
  }
}

@Path("/simpleDraw")
class SimpleDrawRaster {
  @GET
  @Path("/{name}")
  def get(@PathParam("name") name:String) = {
    val rasterOp:Op[Raster] = io.LoadRaster(name)
    val pngOp:Op[Array[Byte]] = io.SimpleRenderPng(rasterOp, BlueToRed)
    // run the operation
    try {
      val img:Array[Byte] = Demo.server.run(pngOp)
      response("image/png")(img)
    } catch {
      case e:Throwable => response("text/plain")(e.toString)
    }
  }
}

@Path("/draw")
class DrawRaster {
  @GET
  @Path("/{name}/palette/{palette}/shades/{shades}")
  def get(@PathParam("name") name:String,
          @PathParam("palette") palette:String,
          @PathParam("shades") shades:String,
          @Context req:HttpServletRequest) = {

    // load the raster
    val rasterOp:Op[Raster] = io.LoadRaster(name)

    // find the colors to use
    val paletteOp:Op[Array[Int]] = logic.ForEach(string.SplitOnComma(palette))(string.ParseColor(_))
    val numOp:Op[Int] = string.ParseInt(shades)
    val colorsOp:Op[Array[Int]] = stat.GetColorsFromPalette(paletteOp, numOp)

    // find the appropriate quantile class breaks to use
    val histogramOp:Op[Histogram] = stat.GetHistogram(rasterOp)
    val breaksOp:Op[ColorBreaks] = stat.GetColorBreaks(histogramOp, colorsOp)

    // render the png
    val pngOp:Op[Array[Byte]] = io.RenderPng(rasterOp, breaksOp, 0)

    // run the operation
    try {
      val img:Array[Byte] = Demo.server.run(pngOp)
      response("image/png")(img)
    } catch {
      case e:Throwable => response("text/plain")(e.toString)
    }
  }
}
