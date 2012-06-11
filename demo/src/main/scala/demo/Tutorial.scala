package demo

import javax.servlet.http.{HttpServletRequest}
import javax.ws.rs.{GET, Path, DefaultValue, PathParam, QueryParam}
import javax.ws.rs.core.{Response, Context}

import geotrellis.{Extent, Raster}
import geotrellis.data.{ColorBreaks}
import geotrellis.stat.{Histogram}
import geotrellis.operation._
import geotrellis.process.{Server}
import geotrellis.Implicits._


//object Demo {
//  val server = Server("myapp", "src/main/resources/myapp-catalog.json")
//}

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
    val opX:Op[Int] = ParseInt(s)

    // add one
    val opY:Op[Int] = opX + 1

    // run the operation
    val data:String = try {
      val y:Int = Demo.server.run(opY)
      y.toString
    } catch {
      case e => e.toString
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
    val e1:Op[Extent] = ParseExtent(s1)
    val e2:Op[Extent] = ParseExtent(s2)

    // combine the extents
    val op:Op[Extent] = CombineExtents(e1, e2)

    // run the operation
    val data:String = try {
      val extent:Extent = Demo.server.run(op)
      extent.toString
    } catch {
      case e => e.toString
    }

    response("text/plain")(data)
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
    val rasterOp:Op[Raster] = LoadRaster(name)

    // find the colors to use
    val paletteOp:Op[Array[Int]] = ForEach(SplitOnComma(palette))(ParseHexInt(_))
    val numOp:Op[Int] = ParseInt(shades)
    val colorsOp:Op[Array[Int]] = ColorsFromPalette(paletteOp, numOp)

    // find the appropriate quantile class breaks to use
    val histogramOp:Op[Histogram] = BuildMapHistogram(rasterOp)
    val breaksOp:Op[ColorBreaks] = FindColorBreaks(histogramOp, colorsOp)

    // render the png
    val pngOp:Op[Array[Byte]] = RenderPNG(rasterOp, breaksOp, 0, true)

    // run the operation
    try {
      val img:Array[Byte] = Demo.server.run(pngOp)
      response("image/png")(img)
    } catch {
      case e => response("text/plain")(e.toString)
    }
  }
}
