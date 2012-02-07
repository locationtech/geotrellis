package trellis.rest.myapp

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.{GET, Path, DefaultValue, PathParam, QueryParam}
import javax.ws.rs.core.{Response, Context}

import trellis._
import trellis.operation._
import trellis.process._

object MyApp {
  val server = Server("myapp", "src/main/resources/myapp-catalog.json")
  def response(mime:String)(data:Any) = Response.ok(data).`type`(mime).build()
}

@Path("/greeting")
class HelloWorld {
  @GET
  def get(@Context req:HttpServletRequest) = {
    // give a friendly greeting
    MyApp.response("text/plain")("hello world")
  }
}

@Path("/adder/")
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
      val y:Int = MyApp.server.run(opY)
      y.toString
    } catch {
      case e => e.toString
    }

    MyApp.response("text/plain")(data)
  }
}

@Path("/bbox/")
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
    val data = try {
      val extent:Extent = MyApp.server.run(op)
      extent.toString
    } catch {
      case e => e.toString
    }

    MyApp.response("text/plain")(data)
  }
}

@Path("/draw/")
class DrawRaster {
  @GET
  @Path("/{name}/palette/{palette}/shades/{shades}")
  def get(@PathParam("name") name:String,
          @PathParam("palette") palette:String,
          @PathParam("shades") shades:String,
          @Context req:HttpServletRequest) = {

    // load the raster
    val rasterOp = LoadRaster(name)

    // find the colors to use
    val paletteOp = ForEach(SplitOnComma(palette))(ParseHexInt(_))
    val numOp = ParseInt(shades)
    val colorsOp = ColorsFromPalette(paletteOp, numOp)

    // find the appropriate quantile class breaks to use
    val histogramOp = BuildMapHistogram(rasterOp)
    val breaksOp = FindColorBreaks(histogramOp, colorsOp)

    // render the png
    val pngOp = RenderPNG(rasterOp, breaksOp, 0, true)

    // run the operation
    try {
      val img:Array[Byte] = MyApp.server.run(pngOp)
      MyApp.response("image/png")(img)
    } catch {
      case e => MyApp.response("text/plain")(e.toString)
    }
  }
}
